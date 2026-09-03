package com.domainify.service;

import com.domainify.dto.EmailConfigDto;
import com.domainify.dto.EmailConfigUpdateRequest;
import com.domainify.dto.EmailTestResultDto;
import com.domainify.entity.EmailConfig;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.EmailConfigRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmailConfigService {

    private final EmailConfigRepository configRepository;
    private final MailService mailService;

    public EmailConfigService(EmailConfigRepository configRepository, MailService mailService) {
        this.configRepository = configRepository;
        this.mailService = mailService;
    }

    @Transactional
    public EmailConfig getOrCreate() {
        return configRepository.findById(EmailConfig.SINGLETON_ID)
                .orElseGet(() -> configRepository.save(EmailConfig.defaults()));
    }

    @Transactional(readOnly = true)
    public EmailConfigDto getDto() {
        return toDto(getOrCreate());
    }

    @Transactional
    public EmailConfigDto update(EmailConfigUpdateRequest request) {
        EmailConfig config = getOrCreate();
        config.setEnabled(request.isEnabled());
        config.setUseTls(request.isUseTls());

        String host = normalize(request.getHost());
        String username = normalize(request.getUsername());
        String fromEmail = normalize(request.getFromEmail());
        String fromName = normalize(request.getFromName());

        if (request.isEnabled()) {
            if (!StringUtils.hasText(host)) {
                throw new ApiException(ErrorCode.EMAIL_HOST_REQUIRED);
            }
            if (request.getPort() < 1 || request.getPort() > 65535) {
                throw new ApiException(ErrorCode.EMAIL_CONFIG_INVALID);
            }
            if (!StringUtils.hasText(fromEmail)) {
                throw new ApiException(ErrorCode.EMAIL_FROM_REQUIRED);
            }
            if (!isValidEmail(fromEmail)) {
                throw new ApiException(ErrorCode.EMAIL_CONFIG_INVALID);
            }
            if (StringUtils.hasText(username)) {
                if (StringUtils.hasText(request.getPassword())) {
                    config.setPassword(request.getPassword().trim());
                } else if (!StringUtils.hasText(config.getPassword())) {
                    throw new ApiException(ErrorCode.EMAIL_PASSWORD_REQUIRED);
                }
            }
        }

        config.setHost(host);
        config.setPort(request.getPort());
        config.setUsername(username);
        config.setFromEmail(fromEmail);
        config.setFromName(fromName);

        if (StringUtils.hasText(request.getPassword())) {
            config.setPassword(request.getPassword().trim());
        }

        return toDto(configRepository.save(config));
    }

    @Transactional(readOnly = true)
    public EmailTestResultDto sendTestEmail(String to) {
        EmailConfig config = getOrCreate();
        if (!config.isEnabled()) {
            throw new ApiException(ErrorCode.EMAIL_CONFIG_DISABLED);
        }
        if (!StringUtils.hasText(config.getHost()) || !StringUtils.hasText(config.getFromEmail())) {
            throw new ApiException(ErrorCode.EMAIL_CONFIG_INVALID);
        }
        try {
            mailService.sendTestEmail(config, to.trim());
            return new EmailTestResultDto(true, null);
        } catch (MessagingException ex) {
            return new EmailTestResultDto(false, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public boolean isEnabled() {
        EmailConfig config = getOrCreate();
        return config.isEnabled()
                && StringUtils.hasText(config.getHost())
                && StringUtils.hasText(config.getFromEmail());
    }

    private EmailConfigDto toDto(EmailConfig config) {
        EmailConfigDto dto = new EmailConfigDto();
        dto.setEnabled(config.isEnabled());
        dto.setHost(config.getHost());
        dto.setPort(config.getPort());
        dto.setUsername(config.getUsername());
        dto.setPasswordConfigured(StringUtils.hasText(config.getPassword()));
        dto.setFromEmail(config.getFromEmail());
        dto.setFromName(config.getFromName());
        dto.setUseTls(config.isUseTls());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
