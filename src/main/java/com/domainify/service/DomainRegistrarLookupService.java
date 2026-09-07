package com.domainify.service;

import com.domainify.dto.DomainRegistrarExpiryDto;
import com.domainify.dto.DomainWhoisDto;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Domain registration lookup via RDAP (IANA bootstrap + registry / rdap.org).
 */
@Service
public class DomainRegistrarLookupService {

    private static final Logger log = LoggerFactory.getLogger(DomainRegistrarLookupService.class);

    private static final Pattern DOMAIN_NAME_PATTERN = Pattern.compile(
            "^(?=.{1,253}$)(?!-)[a-z0-9-]+(\\.[a-z0-9-]+)+$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final long BOOTSTRAP_TTL_MS = 24L * 60L * 60L * 1000L;

    private final ObjectMapper objectMapper;
    private final String bootstrapUrl;
    private final HttpClient httpClient;

    private final AtomicReference<BootstrapCache> bootstrapCache = new AtomicReference<>();

    public DomainRegistrarLookupService(
            ObjectMapper objectMapper,
            @Value("${app.domain.rdap.bootstrap-url:https://data.iana.org/rdap/dns.json}") String bootstrapUrl) {
        this.objectMapper = objectMapper;
        this.bootstrapUrl = bootstrapUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Full registration details (WHOIS-equivalent via RDAP). */
    public DomainWhoisDto lookupWhois(String rawName) {
        String name = normalizeName(rawName);
        if (!StringUtils.hasText(name) || !DOMAIN_NAME_PATTERN.matcher(name).matches()) {
            throw new ApiException(ErrorCode.DOMAIN_NAME_INVALID);
        }

        List<String> baseUrls = resolveRdapBases(name);
        Exception lastError = null;
        for (String base : baseUrls) {
            String url = joinRdapUrl(base, name);
            try {
                return parseWhois(fetchJson(url), name, url);
            } catch (ApiException ex) {
                if (ex.getCode() == ErrorCode.DOMAIN_WHOIS_NOT_FOUND
                        || ex.getCode() == ErrorCode.DOMAIN_EXPIRY_NOT_FOUND) {
                    throw new ApiException(ErrorCode.DOMAIN_WHOIS_NOT_FOUND);
                }
                throw ex;
            } catch (Exception ex) {
                lastError = ex;
                log.debug("RDAP WHOIS failed for {} via {}: {}", name, url, ex.getMessage());
            }
        }

        try {
            String fallback = "https://rdap.org/domain/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
            return parseWhois(fetchJson(fallback), name, fallback);
        } catch (ApiException ex) {
            if (ex.getCode() == ErrorCode.DOMAIN_WHOIS_NOT_FOUND
                    || ex.getCode() == ErrorCode.DOMAIN_EXPIRY_NOT_FOUND) {
                throw new ApiException(ErrorCode.DOMAIN_WHOIS_NOT_FOUND);
            }
            throw ex;
        } catch (Exception ex) {
            lastError = ex;
            log.debug("RDAP.org WHOIS fallback failed for {}: {}", name, ex.getMessage());
        }

        if (lastError != null) {
            log.info("WHOIS lookup failed for {}: {}", name, lastError.getMessage());
        }
        throw new ApiException(ErrorCode.DOMAIN_WHOIS_LOOKUP_FAILED);
    }

    /** Expiry-focused lookup used by refresh-expiry flows. */
    public DomainRegistrarExpiryDto lookup(String rawName) {
        DomainWhoisDto whois = lookupWhois(rawName);
        if (whois.getExpiresAt() == null) {
            throw new ApiException(ErrorCode.DOMAIN_EXPIRY_LOOKUP_FAILED);
        }
        return new DomainRegistrarExpiryDto(
                whois.getDomainName(),
                whois.getExpiresAt(),
                whois.getRegistrar(),
                whois.getRdapUrl(),
                whois.getCheckedAt() != null ? whois.getCheckedAt() : Instant.now()
        );
    }

    private DomainWhoisDto parseWhois(JsonNode root, String name, String url) {
        DomainWhoisDto dto = new DomainWhoisDto();
        dto.setDomainName(name);
        dto.setLdhName(firstNonBlank(text(root, "ldhName"), text(root, "handle"), name));
        dto.setUnicodeName(text(root, "unicodeName"));
        dto.setStatuses(extractStatuses(root));
        dto.setRegisteredAt(extractEventDate(root, "registration", "registered", "create", "created"));
        dto.setUpdatedAt(extractEventDate(root, "last changed", "last update", "last updated", "update", "changed"));
        dto.setExpiresAt(extractEventDate(root,
                "expiration", "expiry", "expire", "registry expiration", "registrar expiration"));
        fillRegistrar(root, dto);
        fillRegistrant(root, dto);
        dto.setNameServers(extractNameServers(root));
        dto.setDnssecSigned(extractDnssecSigned(root));
        dto.setRdapUrl(url);
        dto.setCheckedAt(Instant.now());
        dto.setAvailable(false);
        return dto;
    }

    private List<String> extractStatuses(JsonNode root) {
        Set<String> statuses = new LinkedHashSet<>();
        JsonNode node = root.path("status");
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && StringUtils.hasText(item.asText())) {
                    statuses.add(item.asText().trim());
                }
            }
        } else if (node.isTextual() && StringUtils.hasText(node.asText())) {
            statuses.add(node.asText().trim());
        }
        return new ArrayList<>(statuses);
    }

    private LocalDate extractEventDate(JsonNode root, String... actions) {
        JsonNode events = root.path("events");
        if (!events.isArray()) {
            return null;
        }
        Set<String> wanted = new LinkedHashSet<>();
        for (String action : actions) {
            wanted.add(normalizeAction(action));
        }
        for (JsonNode event : events) {
            String action = text(event, "eventAction");
            if (action == null) {
                continue;
            }
            if (!wanted.contains(normalizeAction(action))) {
                continue;
            }
            LocalDate date = parseEventDate(text(event, "eventDate"));
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    private void fillRegistrar(JsonNode root, DomainWhoisDto dto) {
        JsonNode entity = findEntityByRole(root, "registrar");
        if (entity == null) {
            return;
        }
        VcardInfo vcard = extractVcard(entity.path("vcardArray"));
        String handle = text(entity, "handle");
        dto.setRegistrar(firstNonBlank(vcard.fn, handle));
        dto.setRegistrarIanaId(firstNonBlank(publicId(entity, "IANA Registrar ID"), handle));
        dto.setRegistrarEmail(vcard.email);
        dto.setRegistrarUrl(firstNonBlank(vcard.url, firstLink(entity)));
    }

    private void fillRegistrant(JsonNode root, DomainWhoisDto dto) {
        JsonNode entity = findEntityByRole(root, "registrant");
        if (entity == null) {
            return;
        }
        VcardInfo vcard = extractVcard(entity.path("vcardArray"));
        dto.setRegistrantName(vcard.fn);
        dto.setRegistrantOrganization(vcard.org);
        dto.setRegistrantCountry(vcard.country);
        dto.setRegistrantEmail(vcard.email);
    }

    private JsonNode findEntityByRole(JsonNode root, String role) {
        JsonNode entities = root.path("entities");
        if (!entities.isArray()) {
            return null;
        }
        for (JsonNode entity : entities) {
            if (hasRole(entity, role)) {
                return entity;
            }
            // Nested entities (common for registrar contacts)
            JsonNode nested = entity.path("entities");
            if (nested.isArray()) {
                for (JsonNode child : nested) {
                    if (hasRole(child, role)) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    private boolean hasRole(JsonNode entity, String role) {
        JsonNode roles = entity.path("roles");
        if (!roles.isArray()) {
            return false;
        }
        for (JsonNode item : roles) {
            if (item.isTextual() && role.equalsIgnoreCase(item.asText().trim())) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractNameServers(JsonNode root) {
        Set<String> servers = new LinkedHashSet<>();
        JsonNode nameservers = root.path("nameservers");
        if (nameservers.isArray()) {
            for (JsonNode ns : nameservers) {
                String ldh = firstNonBlank(text(ns, "ldhName"), text(ns, "unicodeName"));
                if (StringUtils.hasText(ldh)) {
                    servers.add(ldh.toLowerCase(Locale.ROOT));
                }
            }
        }
        // Some registries put NS under "secureDNS" / links only — also scan NS records if present
        JsonNode records = root.path("ns");
        if (records.isArray()) {
            for (JsonNode ns : records) {
                if (ns.isTextual() && StringUtils.hasText(ns.asText())) {
                    servers.add(ns.asText().trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(servers);
    }

    private boolean extractDnssecSigned(JsonNode root) {
        JsonNode secure = root.path("secureDNS");
        if (secure.isMissingNode() || secure.isNull()) {
            return false;
        }
        JsonNode delegation = secure.path("delegationSigned");
        if (delegation.isBoolean()) {
            return delegation.asBoolean();
        }
        JsonNode zone = secure.path("zoneSigned");
        if (zone.isBoolean()) {
            return zone.asBoolean();
        }
        JsonNode dsData = secure.path("dsData");
        return dsData.isArray() && !dsData.isEmpty();
    }

    private String publicId(JsonNode entity, String type) {
        JsonNode ids = entity.path("publicIds");
        if (!ids.isArray()) {
            return null;
        }
        for (JsonNode id : ids) {
            String idType = text(id, "type");
            String identifier = text(id, "identifier");
            if (type.equalsIgnoreCase(idType) && StringUtils.hasText(identifier)) {
                return identifier;
            }
        }
        return null;
    }

    private String firstLink(JsonNode entity) {
        JsonNode links = entity.path("links");
        if (!links.isArray()) {
            return null;
        }
        for (JsonNode link : links) {
            String href = text(link, "href");
            if (StringUtils.hasText(href)) {
                return href;
            }
        }
        return null;
    }

    private VcardInfo extractVcard(JsonNode vcardArray) {
        VcardInfo info = new VcardInfo();
        if (!vcardArray.isArray() || vcardArray.size() < 2) {
            return info;
        }
        JsonNode entries = vcardArray.get(1);
        if (!entries.isArray()) {
            return info;
        }
        for (JsonNode entry : entries) {
            if (!entry.isArray() || entry.size() < 4) {
                continue;
            }
            String key = entry.get(0).asText("");
            JsonNode value = entry.get(3);
            if (!StringUtils.hasText(key) || value == null || value.isNull()) {
                continue;
            }
            switch (key.toLowerCase(Locale.ROOT)) {
                case "fn" -> {
                    if (value.isTextual()) {
                        info.fn = value.asText();
                    }
                }
                case "org" -> {
                    if (value.isTextual()) {
                        info.org = value.asText();
                    } else if (value.isArray() && !value.isEmpty() && value.get(0).isTextual()) {
                        info.org = value.get(0).asText();
                    }
                }
                case "email" -> {
                    if (value.isTextual()) {
                        info.email = value.asText();
                    }
                }
                case "url" -> {
                    if (value.isTextual()) {
                        info.url = value.asText();
                    }
                }
                case "adr" -> {
                    if (value.isArray() && value.size() >= 7 && value.get(6).isTextual()) {
                        info.country = value.get(6).asText();
                    }
                }
                default -> {
                    // ignore
                }
            }
        }
        return info;
    }

    private String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeAction(String action) {
        return action.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }

    private List<String> resolveRdapBases(String domain) {
        Map<String, List<String>> byTld = loadBootstrap();
        List<String> labels = List.of(domain.split("\\."));
        List<String> bases = new ArrayList<>();
        for (int i = 0; i < labels.size() - 1; i++) {
            String tld = String.join(".", labels.subList(i, labels.size()));
            List<String> urls = byTld.get(tld);
            if (urls != null && !urls.isEmpty()) {
                for (String url : urls) {
                    if (StringUtils.hasText(url) && !bases.contains(url)) {
                        bases.add(url);
                    }
                }
                break;
            }
        }
        return bases;
    }

    private Map<String, List<String>> loadBootstrap() {
        BootstrapCache cached = bootstrapCache.get();
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.fetchedAt < BOOTSTRAP_TTL_MS) {
            return cached.byTld;
        }
        synchronized (this) {
            cached = bootstrapCache.get();
            if (cached != null && now - cached.fetchedAt < BOOTSTRAP_TTL_MS) {
                return cached.byTld;
            }
            try {
                JsonNode root = fetchJson(bootstrapUrl);
                Map<String, List<String>> byTld = new LinkedHashMap<>();
                JsonNode services = root.path("services");
                if (services.isArray()) {
                    for (JsonNode service : services) {
                        if (!service.isArray() || service.size() < 2) {
                            continue;
                        }
                        JsonNode tlds = service.get(0);
                        JsonNode urls = service.get(1);
                        if (!tlds.isArray() || !urls.isArray()) {
                            continue;
                        }
                        List<String> urlList = new ArrayList<>();
                        for (JsonNode urlNode : urls) {
                            if (urlNode.isTextual() && StringUtils.hasText(urlNode.asText())) {
                                urlList.add(urlNode.asText().trim());
                            }
                        }
                        for (JsonNode tldNode : tlds) {
                            if (tldNode.isTextual() && StringUtils.hasText(tldNode.asText())) {
                                byTld.put(tldNode.asText().trim().toLowerCase(Locale.ROOT), urlList);
                            }
                        }
                    }
                }
                bootstrapCache.set(new BootstrapCache(byTld, System.currentTimeMillis()));
                return byTld;
            } catch (Exception ex) {
                log.warn("Failed to load RDAP bootstrap: {}", ex.getMessage());
                if (cached != null) {
                    return cached.byTld;
                }
                return Map.of();
            }
        }
    }

    private String joinRdapUrl(String base, String domain) {
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized + "/domain/" + URLEncoder.encode(domain, StandardCharsets.UTF_8);
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/rdap+json, application/json")
                .header("User-Agent", "Domainify/1.0 (RDAP WHOIS lookup)")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 404) {
            throw new ApiException(ErrorCode.DOMAIN_WHOIS_NOT_FOUND);
        }
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("HTTP " + status + " from " + url);
        }
        String body = response.body();
        if (!StringUtils.hasText(body)) {
            throw new IllegalStateException("Empty RDAP response");
        }
        return objectMapper.readTree(body);
    }

    private LocalDate parseEventDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            return Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // continue
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(10, value.length())));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode child = node.path(field);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        if (child.isTextual()) {
            return child.asText();
        }
        return child.asText(null);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private record BootstrapCache(Map<String, List<String>> byTld, long fetchedAt) {
    }

    private static final class VcardInfo {
        private String fn;
        private String org;
        private String email;
        private String url;
        private String country;
    }
}
