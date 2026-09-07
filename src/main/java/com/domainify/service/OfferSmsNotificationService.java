package com.domainify.service;

import com.domainify.dto.SmsBulkSendRequest;
import com.domainify.dto.SmsBulkSendResultDto;
import com.domainify.entity.DomainListingOffer;
import com.domainify.entity.NotificationType;
import com.domainify.entity.User;
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
 * Opt-in SMS alerts for marketplace offer events (verified phone + user SMS preference).
 */
@Service
public class OfferSmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OfferSmsNotificationService.class);

    private static final Set<NotificationType> SMS_TYPES = EnumSet.of(
            NotificationType.OFFER_RECEIVED,
            NotificationType.OFFER_COUNTERED,
            NotificationType.OFFER_ACCEPTED,
            NotificationType.OFFER_REJECTED,
            NotificationType.OFFER_WITHDRAWN,
            NotificationType.OFFER_EXPIRED
    );

    private final SmsService smsService;
    private final SmsConfigService smsConfigService;
    private final MessageService messageService;

    public OfferSmsNotificationService(
            SmsService smsService,
            SmsConfigService smsConfigService,
            MessageService messageService) {
        this.smsService = smsService;
        this.smsConfigService = smsConfigService;
        this.messageService = messageService;
    }

    public void sendIfConfigured(
            User recipient,
            User actor,
            NotificationType type,
            DomainListingOffer offer) {
        if (recipient == null || type == null || offer == null || !SMS_TYPES.contains(type)) {
            return;
        }
        if (!recipient.isEnabled() || !recipient.isSmsNotificationsEnabled()) {
            return;
        }
        if (!recipient.hasPhoneNumber() || !recipient.isPhoneVerified()) {
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
            String domainName = domainName(offer);
            String amount = amountText(offer);
            String actorName = displayName(actor, locale);
            String key = "notification.sms." + type.name().toLowerCase(Locale.ROOT);
            String text = type == NotificationType.OFFER_EXPIRED
                    ? messageService.get(key, new Object[]{domainName, amount}, locale)
                    : messageService.get(key, new Object[]{actorName, amount, domainName}, locale);
            SmsBulkSendRequest request = new SmsBulkSendRequest();
            request.setMobiles(List.of(mobile));
            request.setMessageText(text);
            SmsBulkSendResultDto result = smsService.sendBulk(request);
            if (result == null || !result.isSuccess()) {
                log.warn("Offer SMS notification rejected for user {}", recipient.getId());
            }
        } catch (Exception ex) {
            log.warn("Offer SMS notification skipped for user {}: {}", recipient.getId(), ex.getMessage());
        }
    }

    private boolean isSmsProviderConfigured() {
        return StringUtils.hasText(smsConfigService.getApiKey())
                && StringUtils.hasText(smsConfigService.getDefaultLine());
    }

    private static String domainName(DomainListingOffer offer) {
        if (offer.getListing() != null && offer.getListing().getDomain() != null
                && StringUtils.hasText(offer.getListing().getDomain().getName())) {
            return offer.getListing().getDomain().getName();
        }
        return "—";
    }

    private static String amountText(DomainListingOffer offer) {
        return offer.getAmount() != null ? offer.getAmount().toPlainString() : "0";
    }

    private String displayName(User user, Locale locale) {
        if (user == null) {
            return messageService.get("notification.someone", null, locale);
        }
        String name = ((StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : "")
                + " "
                + (StringUtils.hasText(user.getLastName()) ? user.getLastName() : "")).trim();
        if (StringUtils.hasText(name)) {
            return name;
        }
        return StringUtils.hasText(user.getEmail()) ? user.getEmail() : messageService.get("notification.someone", null, locale);
    }
}
