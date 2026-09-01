package com.domainify.service;

import com.domainify.dto.SmsConfigDto;
import com.domainify.dto.SmsConfigUpdateRequest;
import com.domainify.entity.SmsConfig;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.SmsConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class SmsConfigService {

    private final SmsConfigRepository configRepository;

    public SmsConfigService(SmsConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Transactional
    public SmsConfig getOrCreate() {
        return configRepository.findById(SmsConfig.SINGLETON_ID)
                .orElseGet(() -> configRepository.save(SmsConfig.defaults()));
    }

    @Transactional(readOnly = true)
    public SmsConfigDto getDto() {
        return toDto(getOrCreate());
    }

    @Transactional
    public SmsConfigDto update(SmsConfigUpdateRequest request) {
        String serverUrl = normalizeUrl(request.getServerUrl());
        validateUrl(serverUrl);

        SmsConfig config = getOrCreate();
        config.setServerUrl(serverUrl);

        if (StringUtils.hasText(request.getApiKey())) {
            config.setApiKey(request.getApiKey().trim());
        } else if (!StringUtils.hasText(config.getApiKey())) {
            throw new ApiException(ErrorCode.SMS_API_KEY_REQUIRED);
        }

        return toDto(configRepository.save(config));
    }

    @Transactional
    public SmsConfigDto updateDefaultLine(String defaultLine) {
        if (!StringUtils.hasText(defaultLine)) {
            throw new ApiException(ErrorCode.SMS_DEFAULT_LINE_INVALID);
        }
        SmsConfig config = getOrCreate();
        config.setDefaultLine(defaultLine.trim());
        return toDto(configRepository.save(config));
    }

    /** Returns the stored API key for internal SMS sending (never expose via REST). */
    @Transactional(readOnly = true)
    public String getApiKey() {
        SmsConfig config = getOrCreate();
        return StringUtils.hasText(config.getApiKey()) ? config.getApiKey() : null;
    }

    @Transactional(readOnly = true)
    public String getServerUrl() {
        return getOrCreate().getServerUrl();
    }

    @Transactional(readOnly = true)
    public String getDefaultLine() {
        SmsConfig config = getOrCreate();
        return StringUtils.hasText(config.getDefaultLine()) ? config.getDefaultLine() : null;
    }

    private SmsConfigDto toDto(SmsConfig config) {
        SmsConfigDto dto = new SmsConfigDto();
        dto.setServerUrl(config.getServerUrl());
        dto.setApiKeyConfigured(StringUtils.hasText(config.getApiKey()));
        dto.setDefaultLine(config.getDefaultLine());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    private String normalizeUrl(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.endsWith("/")) {
            trimmed += "/";
        }
        return trimmed;
    }

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new ApiException(ErrorCode.SMS_CONFIG_INVALID);
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new ApiException(ErrorCode.SMS_CONFIG_INVALID);
            }
        } catch (URISyntaxException ex) {
            throw new ApiException(ErrorCode.SMS_CONFIG_INVALID);
        }
    }
}
