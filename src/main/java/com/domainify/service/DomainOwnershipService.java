package com.domainify.service;

import com.domainify.dto.DomainDto;
import com.domainify.dto.DomainOwnershipChallengeDto;
import com.domainify.dto.StartDomainOwnershipRequest;
import com.domainify.entity.Domain;
import com.domainify.entity.DomainOwnershipMethod;
import com.domainify.entity.DomainOwnershipStatus;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class DomainOwnershipService {

    private static final Logger log = LoggerFactory.getLogger(DomainOwnershipService.class);
    private static final String DNS_HOST_PREFIX = "_domainify-challenge";
    private static final String HTTP_PATH = "/.well-known/domainify-verification.txt";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DomainRepository domainRepository;
    private final DomainDnsLookup domainDnsLookup;
    private final Duration tokenTtl;
    private final int maxCheckAttempts;
    private final Duration checkCooldown;
    private final HttpClient httpClient;

    public DomainOwnershipService(
            DomainRepository domainRepository,
            DomainDnsLookup domainDnsLookup,
            @Value("${app.domain-ownership.token-ttl-hours:168}") long tokenTtlHours,
            @Value("${app.domain-ownership.max-check-attempts:20}") int maxCheckAttempts,
            @Value("${app.domain-ownership.check-cooldown-seconds:15}") long checkCooldownSeconds) {
        this.domainRepository = domainRepository;
        this.domainDnsLookup = domainDnsLookup;
        this.tokenTtl = Duration.ofHours(Math.max(1, tokenTtlHours));
        this.maxCheckAttempts = Math.max(3, maxCheckAttempts);
        this.checkCooldown = Duration.ofSeconds(Math.max(5, checkCooldownSeconds));
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Transactional
    public DomainOwnershipChallengeDto start(User owner, Long domainId, StartDomainOwnershipRequest request) {
        Domain domain = requireOwned(owner, domainId);
        if (request == null || request.getMethod() == null) {
            throw new ApiException(ErrorCode.DOMAIN_OWNERSHIP_METHOD_INVALID);
        }
        DomainOwnershipMethod method = request.getMethod();

        String token = HexFormat.of().formatHex(randomBytes(16));
        Instant expiresAt = Instant.now().plus(tokenTtl);

        domain.setOwnershipStatus(DomainOwnershipStatus.PENDING);
        domain.setOwnershipMethod(method);
        domain.setOwnershipToken(token);
        domain.setOwnershipTokenExpiresAt(expiresAt);
        domain.setOwnershipCheckAttempts(0);
        domain.setOwnershipLastCheckedAt(null);
        // Keep verifiedAt until success re-sets it; clear when starting fresh from verified
        if (domain.getOwnershipVerifiedAt() != null) {
            domain.setOwnershipVerifiedAt(null);
        }
        domainRepository.save(domain);
        return toChallenge(domain);
    }

    @Transactional(readOnly = true)
    public DomainOwnershipChallengeDto getChallenge(User owner, Long domainId) {
        Domain domain = requireOwned(owner, domainId);
        if (domain.getOwnershipStatus() != DomainOwnershipStatus.PENDING
                || !StringUtils.hasText(domain.getOwnershipToken())
                || domain.getOwnershipMethod() == null) {
            throw new ApiException(ErrorCode.DOMAIN_OWNERSHIP_NOT_PENDING);
        }
        if (domain.getOwnershipTokenExpiresAt() != null
                && domain.getOwnershipTokenExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.DOMAIN_OWNERSHIP_TOKEN_EXPIRED);
        }
        return toChallenge(domain);
    }

    @Transactional
    public DomainDto check(User owner, Long domainId) {
        Domain domain = requireOwned(owner, domainId);
        if (domain.getOwnershipStatus() != DomainOwnershipStatus.PENDING
                || !StringUtils.hasText(domain.getOwnershipToken())
                || domain.getOwnershipMethod() == null) {
            throw new ApiException(ErrorCode.DOMAIN_OWNERSHIP_NOT_PENDING);
        }
        if (domain.getOwnershipTokenExpiresAt() != null
                && domain.getOwnershipTokenExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.DOMAIN_OWNERSHIP_TOKEN_EXPIRED);
        }

        Instant now = Instant.now();
        if (domain.getOwnershipLastCheckedAt() != null
                && domain.getOwnershipLastCheckedAt().plus(checkCooldown).isAfter(now)) {
            throw new ApiException(ErrorCode.DOMAIN_OWNERSHIP_CHECK_TOO_SOON);
        }
        if (domain.getOwnershipCheckAttempts() >= maxCheckAttempts) {
            throw new ApiException(ErrorCode.DOMAIN_OWNERSHIP_CHECK_LIMIT);
        }

        domain.setOwnershipLastCheckedAt(now);
        domain.setOwnershipCheckAttempts(domain.getOwnershipCheckAttempts() + 1);

        boolean matched = switch (domain.getOwnershipMethod()) {
            case DNS_TXT -> verifyDns(domain.getName(), domain.getOwnershipToken());
            case HTTP_FILE -> verifyHttp(domain.getName(), domain.getOwnershipToken());
        };

        if (!matched) {
            domainRepository.save(domain);
            throw new ApiException(ErrorCode.DOMAIN_OWNERSHIP_NOT_FOUND);
        }

        domain.setOwnershipStatus(DomainOwnershipStatus.VERIFIED);
        domain.setOwnershipVerifiedAt(now);
        domain.setOwnershipToken(null);
        domain.setOwnershipTokenExpiresAt(null);
        domain.setOwnershipCheckAttempts(0);
        return DomainDto.from(domainRepository.save(domain));
    }

    @Transactional
    public DomainDto cancel(User owner, Long domainId) {
        Domain domain = requireOwned(owner, domainId);
        clearChallenge(domain, DomainOwnershipStatus.UNVERIFIED);
        domain.setOwnershipVerifiedAt(null);
        return DomainDto.from(domainRepository.save(domain));
    }

    /** Called when the domain name changes — ownership must be re-proven. */
    public void resetOwnership(Domain domain) {
        clearChallenge(domain, DomainOwnershipStatus.UNVERIFIED);
        domain.setOwnershipVerifiedAt(null);
        domain.setOwnershipMethod(null);
    }

    private void clearChallenge(Domain domain, DomainOwnershipStatus status) {
        domain.setOwnershipStatus(status);
        domain.setOwnershipToken(null);
        domain.setOwnershipTokenExpiresAt(null);
        domain.setOwnershipCheckAttempts(0);
        domain.setOwnershipLastCheckedAt(null);
        if (status == DomainOwnershipStatus.UNVERIFIED) {
            domain.setOwnershipMethod(null);
        }
    }

    private boolean verifyDns(String domainName, String token) {
        String host = DNS_HOST_PREFIX + "." + domainName;
        List<String> records = domainDnsLookup.lookupTxt(host);
        for (String record : records) {
            if (tokenMatches(record, token)) {
                return true;
            }
        }
        // Also accept root TXT with prefix for providers that dislike underscore hosts
        List<String> root = domainDnsLookup.lookupTxt(domainName);
        String prefixed = "domainify-site-verification=" + token;
        for (String record : root) {
            if (tokenMatches(record, token) || tokenMatches(record, prefixed)) {
                return true;
            }
        }
        return false;
    }

    private boolean verifyHttp(String domainName, String token) {
        String[] urls = {
                "https://" + domainName + HTTP_PATH,
                "http://" + domainName + HTTP_PATH
        };
        for (String url : urls) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .header("User-Agent", "DomainifyOwnershipCheck/1.0")
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String body = response.body() != null ? response.body().trim() : "";
                    if (tokenMatches(body, token)) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                log.debug("HTTP ownership check failed for {}: {}", url, ex.getMessage());
            }
        }
        return false;
    }

    private static boolean tokenMatches(String haystack, String token) {
        if (!StringUtils.hasText(haystack) || !StringUtils.hasText(token)) {
            return false;
        }
        String normalized = haystack.trim().replace("\"", "");
        if (normalized.equalsIgnoreCase(token)) {
            return true;
        }
        for (String line : normalized.split("\\R")) {
            String trimmed = line.trim().replace("\"", "");
            if (token.equalsIgnoreCase(trimmed)) {
                return true;
            }
            if (("domainify-site-verification=" + token).equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private DomainOwnershipChallengeDto toChallenge(Domain domain) {
        DomainOwnershipChallengeDto dto = new DomainOwnershipChallengeDto();
        dto.setDomainId(domain.getId());
        dto.setDomainName(domain.getName());
        dto.setOwnershipStatus(domain.getOwnershipStatus());
        dto.setMethod(domain.getOwnershipMethod());
        dto.setToken(domain.getOwnershipToken());
        dto.setTokenExpiresAt(domain.getOwnershipTokenExpiresAt());
        if (domain.getOwnershipMethod() == DomainOwnershipMethod.DNS_TXT) {
            dto.setDnsHost(DNS_HOST_PREFIX + "." + domain.getName());
            dto.setDnsType("TXT");
            dto.setDnsValue(domain.getOwnershipToken());
        } else if (domain.getOwnershipMethod() == DomainOwnershipMethod.HTTP_FILE) {
            dto.setHttpUrl("https://" + domain.getName() + HTTP_PATH);
            dto.setHttpBody(domain.getOwnershipToken());
        }
        return dto;
    }

    private Domain requireOwned(User owner, Long id) {
        if (owner == null || owner.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (id == null) {
            throw new ApiException(ErrorCode.DOMAIN_NOT_FOUND);
        }
        return domainRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.DOMAIN_NOT_FOUND));
    }

    private static byte[] randomBytes(int len) {
        byte[] bytes = new byte[len];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
