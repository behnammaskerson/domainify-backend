package com.domainify.service;

import com.domainify.dto.CurrencyRatesDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fetches FX rates from IraniWallet and caches them in-process.
 * Upstream limit is 10 req/min with ~10 minute refresh — we refresh at most every 10 minutes.
 */
@Service
public class CurrencyRateService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyRateService.class);

    private static final Set<String> SUPPORTED = Set.of(
            "USD", "USDT", "EUR", "CAD", "AUD", "TRY", "AED", "CNY"
    );

    private final RestTemplate restTemplate;
    private final String ratesUrl;
    private final long cacheTtlMs;
    private final long minFetchGapMs;

    private final AtomicReference<CurrencyRatesDto> cache = new AtomicReference<>(emptySnapshot());
    private final Object fetchLock = new Object();
    private volatile long lastAttemptAt;

    public CurrencyRateService(
            RestTemplate restTemplate,
            @Value("${app.currency.rates-url:https://iraniwallet.com/api/v1/rates}") String ratesUrl,
            @Value("${app.currency.cache-ttl-ms:600000}") long cacheTtlMs,
            @Value("${app.currency.min-fetch-gap-ms:15000}") long minFetchGapMs) {
        this.restTemplate = restTemplate;
        this.ratesUrl = ratesUrl;
        this.cacheTtlMs = cacheTtlMs;
        this.minFetchGapMs = minFetchGapMs;
    }

    /** Returns cached rates, refreshing upstream when the TTL has expired. */
    public CurrencyRatesDto getRates() {
        CurrencyRatesDto current = cache.get();
        if (isFresh(current)) {
            return current;
        }
        refreshIfDue(false);
        return cache.get();
    }

    @Scheduled(fixedDelayString = "${app.currency.cache-ttl-ms:600000}", initialDelay = 5_000)
    public void scheduledRefresh() {
        refreshIfDue(false);
    }

    private void refreshIfDue(boolean force) {
        CurrencyRatesDto current = cache.get();
        if (!force && isFresh(current)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAttemptAt < minFetchGapMs) {
            return;
        }
        synchronized (fetchLock) {
            current = cache.get();
            now = System.currentTimeMillis();
            if (!force && isFresh(current)) {
                return;
            }
            if (now - lastAttemptAt < minFetchGapMs) {
                return;
            }
            lastAttemptAt = now;
            try {
                CurrencyRatesDto fresh = fetchUpstream();
                if (fresh != null && !fresh.getSellByCode().isEmpty()) {
                    cache.set(fresh);
                    log.info("Currency rates refreshed ({} codes, updatedAt={})",
                            fresh.getSellByCode().size(), fresh.getUpdatedAt());
                }
            } catch (RestClientException ex) {
                log.warn("Currency rates fetch failed: {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("Currency rates fetch unexpected error: {}", ex.getMessage());
            }
        }
    }

    private boolean isFresh(CurrencyRatesDto snapshot) {
        if (snapshot == null || snapshot.getSellByCode().isEmpty() || snapshot.getFetchedAt() <= 0) {
            return false;
        }
        return System.currentTimeMillis() - snapshot.getFetchedAt() < cacheTtlMs;
    }

    private CurrencyRatesDto fetchUpstream() {
        ResponseEntity<UpstreamRatesResponse> response =
                restTemplate.getForEntity(ratesUrl, UpstreamRatesResponse.class);
        UpstreamRatesResponse body = response.getBody();
        if (body == null || body.rates == null) {
            return null;
        }
        Map<String, Double> sellByCode = new LinkedHashMap<>();
        for (UpstreamRateRow row : body.rates) {
            if (row == null || !StringUtils.hasText(row.code)) {
                continue;
            }
            String code = row.code.trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED.contains(code)) {
                continue;
            }
            Double sell = parseRate(row.sell);
            if (sell != null && sell > 0) {
                sellByCode.put(code, sell);
            }
        }
        String base = StringUtils.hasText(body.base) ? body.base.trim().toUpperCase(Locale.ROOT) : "IRT";
        return new CurrencyRatesDto(base, body.updatedAt, System.currentTimeMillis(), sellByCode);
    }

    private static Double parseRate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static CurrencyRatesDto emptySnapshot() {
        return new CurrencyRatesDto("IRT", null, 0L, Map.of());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UpstreamRatesResponse {
        public String base;
        public String updatedAt;
        public List<UpstreamRateRow> rates;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UpstreamRateRow {
        public String code;
        public String name;
        public String buy;
        public String sell;
        public String url;
    }
}
