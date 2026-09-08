package com.domainify.service;

import com.domainify.dto.PaymentSettingsDto;
import com.domainify.dto.PaymentSettingsUpdateRequest;
import com.domainify.entity.PaymentSettings;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.PaymentSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;

@Service
public class PaymentSettingsService {

    private final PaymentSettingsRepository paymentSettingsRepository;
    private final String frontendUrl;

    public PaymentSettingsService(
            PaymentSettingsRepository paymentSettingsRepository,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.paymentSettingsRepository = paymentSettingsRepository;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public PaymentSettings getOrCreate() {
        return paymentSettingsRepository.findById(PaymentSettings.SINGLETON_ID)
                .orElseGet(() -> paymentSettingsRepository.save(PaymentSettings.defaults()));
    }

    @Transactional(readOnly = true)
    public PaymentSettingsDto getDto() {
        return toDto(getOrCreate());
    }

    @Transactional
    public PaymentSettingsDto update(PaymentSettingsUpdateRequest request) {
        PaymentSettings settings = getOrCreate();

        if (request.getMerchantId() != null) {
            settings.setMerchantId(request.getMerchantId().trim());
        }

        settings.setSandbox(Boolean.TRUE.equals(request.getSandbox()));
        settings.setEnabled(Boolean.TRUE.equals(request.getEnabled()));

        if (StringUtils.hasText(request.getAccessToken())) {
            settings.setAccessToken(request.getAccessToken().trim());
        }

        BigDecimal commission = request.getCommissionPercent().setScale(2, RoundingMode.HALF_UP);
        settings.setCommissionPercent(commission);
        settings.setMinTopUpIrt(request.getMinTopUpIrt());
        settings.setFeaturedListingPriceIrt(request.getFeaturedListingPriceIrt());
        settings.setEscrowHoldDays(request.getEscrowHoldDays());

        String callbackBase = request.getCallbackPublicBaseUrl() == null
                ? ""
                : request.getCallbackPublicBaseUrl().trim();
        if (StringUtils.hasText(callbackBase)) {
            validateUrl(callbackBase);
            if (callbackBase.endsWith("/")) {
                callbackBase = callbackBase.substring(0, callbackBase.length() - 1);
            }
        }
        settings.setCallbackPublicBaseUrl(callbackBase);

        boolean master = Boolean.TRUE.equals(request.getPaymentNotificationsEnabled());
        boolean inApp = Boolean.TRUE.equals(request.getPaymentInAppNotificationsEnabled());
        boolean email = Boolean.TRUE.equals(request.getPaymentEmailNotificationsEnabled());
        boolean sms = Boolean.TRUE.equals(request.getPaymentSmsNotificationsEnabled());
        if (master && !inApp && !email && !sms) {
            throw new ApiException(ErrorCode.PAYMENT_NOTIFICATION_SETTINGS_INVALID);
        }
        settings.setPaymentNotificationsEnabled(master);
        settings.setPaymentInAppNotificationsEnabled(inApp);
        settings.setPaymentEmailNotificationsEnabled(email);
        settings.setPaymentSmsNotificationsEnabled(sms);

        if (settings.isEnabled() && !StringUtils.hasText(settings.getMerchantId())) {
            throw new ApiException(ErrorCode.PAYMENT_MERCHANT_REQUIRED);
        }

        return toDto(paymentSettingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public void requireEnabledConfigured() {
        PaymentSettings settings = getOrCreate();
        if (!settings.isEnabled()) {
            throw new ApiException(ErrorCode.PAYMENT_DISABLED);
        }
        if (!StringUtils.hasText(settings.getMerchantId())) {
            throw new ApiException(ErrorCode.PAYMENT_MERCHANT_REQUIRED);
        }
    }

    @Transactional(readOnly = true)
    public String resolveCallbackUrl() {
        PaymentSettings settings = getOrCreate();
        String base = StringUtils.hasText(settings.getCallbackPublicBaseUrl())
                ? settings.getCallbackPublicBaseUrl().trim()
                : frontendUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/payments/return";
    }

    private PaymentSettingsDto toDto(PaymentSettings settings) {
        PaymentSettingsDto dto = new PaymentSettingsDto();
        dto.setMerchantId(settings.getMerchantId());
        dto.setSandbox(settings.isSandbox());
        dto.setAccessTokenConfigured(StringUtils.hasText(settings.getAccessToken()));
        dto.setEnabled(settings.isEnabled());
        dto.setCommissionPercent(settings.getCommissionPercent());
        dto.setMinTopUpIrt(settings.getMinTopUpIrt());
        dto.setFeaturedListingPriceIrt(settings.getFeaturedListingPriceIrt());
        dto.setEscrowHoldDays(settings.getEscrowHoldDays());
        dto.setCallbackPublicBaseUrl(settings.getCallbackPublicBaseUrl());
        dto.setPaymentNotificationsEnabled(settings.isPaymentNotificationsEnabled());
        dto.setPaymentInAppNotificationsEnabled(settings.isPaymentInAppNotificationsEnabled());
        dto.setPaymentEmailNotificationsEnabled(settings.isPaymentEmailNotificationsEnabled());
        dto.setPaymentSmsNotificationsEnabled(settings.isPaymentSmsNotificationsEnabled());
        dto.setUpdatedAt(settings.getUpdatedAt());
        return dto;
    }

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new ApiException(ErrorCode.PAYMENT_CALLBACK_URL_INVALID);
            }
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ApiException(ErrorCode.PAYMENT_CALLBACK_URL_INVALID);
            }
        } catch (URISyntaxException ex) {
            throw new ApiException(ErrorCode.PAYMENT_CALLBACK_URL_INVALID);
        }
    }
}
