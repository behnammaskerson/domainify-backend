package com.domainify.service;

import com.domainify.dto.SmsBulkSendRequest;
import com.domainify.dto.SmsBulkSendResultDto;
import com.domainify.entity.NotificationType;
import com.domainify.entity.PaymentSettings;
import com.domainify.entity.User;
import com.domainify.service.PaymentEmailNotificationService.PaymentNotificationPayload;
import com.domainify.util.PhoneSmsUtil;
import com.domainify.util.UserPreferredLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Opt-in SMS alerts for payment / wallet events (verified phone + user SMS preference + admin channel).
 */
@Service
public class PaymentSmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSmsNotificationService.class);

    private static final Set<NotificationType> SMS_TYPES = EnumSet.of(
            NotificationType.PAYMENT_TOP_UP_SUCCESS,
            NotificationType.PAYMENT_TOP_UP_FAILED,
            NotificationType.PAYMENT_TOP_UP_CANCELLED,
            NotificationType.PAYMENT_WALLET_ADJUSTED
    );

    private final SmsService smsService;
    private final SmsConfigService smsConfigService;
    private final MessageService messageService;
    private final PaymentSettingsService paymentSettingsService;

    public PaymentSmsNotificationService(
            SmsService smsService,
            SmsConfigService smsConfigService,
            MessageService messageService,
            PaymentSettingsService paymentSettingsService) {
        this.smsService = smsService;
        this.smsConfigService = smsConfigService;
        this.messageService = messageService;
        this.paymentSettingsService = paymentSettingsService;
    }

    public void sendIfConfigured(
            User recipient,
            User actor,
            NotificationType type,
            PaymentNotificationPayload payload) {
        if (recipient == null || type == null || payload == null || !SMS_TYPES.contains(type)) {
            return;
        }
        if (!recipient.isEnabled() || !recipient.isSmsNotificationsEnabled()) {
            return;
        }
        if (!recipient.hasPhoneNumber() || !recipient.isPhoneVerified()) {
            return;
        }
        PaymentSettings settings = paymentSettingsService.getOrCreate();
        if (!settings.isPaymentSmsNotificationsEnabled()) {
            return;
        }
        if (!isSmsProviderConfigured()) {
            return;
        }

        String mobile = PhoneSmsUtil.toSmsMobile(recipient.getPhoneCountryCode(), recipient.getPhoneNumber());
        if (!StringUtils.hasText(mobile)) {
            return;
        }

        try {
            Locale locale = UserPreferredLanguage.toLocale(recipient.getPreferredLanguage());
            String key = "notification.sms." + type.name().toLowerCase(Locale.ROOT);
            String text = smsMessage(type, payload, key, locale);
            SmsBulkSendRequest request = new SmsBulkSendRequest();
            request.setMobiles(List.of(mobile));
            request.setMessageText(text);
            SmsBulkSendResultDto result = smsService.sendBulk(request);
            if (result == null || !result.isSuccess()) {
                log.warn("Payment SMS notification rejected for user {}", recipient.getId());
            }
        } catch (Exception ex) {
            log.warn("Payment SMS notification skipped for user {}: {}", recipient.getId(), ex.getMessage());
        }
    }

    private String smsMessage(
            NotificationType type,
            PaymentNotificationPayload payload,
            String key,
            Locale locale) {
        return switch (type) {
            case PAYMENT_TOP_UP_SUCCESS -> messageService.get(
                    key, new Object[]{payload.amount(), payload.ref()}, locale);
            case PAYMENT_TOP_UP_FAILED -> messageService.get(
                    key, new Object[]{payload.amount(), payload.reasonOrNote()}, locale);
            case PAYMENT_TOP_UP_CANCELLED -> messageService.get(
                    key, new Object[]{payload.amount()}, locale);
            case PAYMENT_WALLET_ADJUSTED -> {
                String dir = messageService.get(
                        "notification.payment.direction."
                                + ("debit".equalsIgnoreCase(payload.direction()) ? "debit" : "credit"),
                        null,
                        locale);
                yield messageService.get(
                        key, new Object[]{dir, payload.amount(), payload.reasonOrNote()}, locale);
            }
            default -> messageService.get(key, null, locale);
        };
    }

    private boolean isSmsProviderConfigured() {
        return StringUtils.hasText(smsConfigService.getApiKey())
                && StringUtils.hasText(smsConfigService.getDefaultLine());
    }
}
