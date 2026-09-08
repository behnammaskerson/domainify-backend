package com.domainify.service;

import com.domainify.entity.EmailConfig;
import com.domainify.entity.NotificationType;
import com.domainify.entity.PaymentSettings;
import com.domainify.entity.User;
import com.domainify.util.UserPreferredLanguage;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Opt-in email alerts for payment / wallet events (user prefs + admin channel + SMTP).
 */
@Service
public class PaymentEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentEmailNotificationService.class);

    private static final Set<NotificationType> EMAIL_TYPES = EnumSet.of(
            NotificationType.PAYMENT_TOP_UP_SUCCESS,
            NotificationType.PAYMENT_TOP_UP_FAILED,
            NotificationType.PAYMENT_TOP_UP_CANCELLED,
            NotificationType.PAYMENT_WALLET_ADJUSTED
    );

    private final EmailConfigService emailConfigService;
    private final MailService mailService;
    private final MessageService messageService;
    private final PaymentSettingsService paymentSettingsService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public PaymentEmailNotificationService(
            EmailConfigService emailConfigService,
            MailService mailService,
            MessageService messageService,
            PaymentSettingsService paymentSettingsService) {
        this.emailConfigService = emailConfigService;
        this.mailService = mailService;
        this.messageService = messageService;
        this.paymentSettingsService = paymentSettingsService;
    }

    public void sendIfConfigured(
            User recipient,
            User actor,
            NotificationType type,
            PaymentNotificationPayload payload) {
        if (recipient == null || type == null || payload == null || !EMAIL_TYPES.contains(type)) {
            return;
        }
        if (!recipient.isEnabled() || !recipient.isEmailNotificationsEnabled()) {
            return;
        }
        if (!StringUtils.hasText(recipient.getEmail())) {
            return;
        }
        PaymentSettings settings = paymentSettingsService.getOrCreate();
        if (!settings.isPaymentEmailNotificationsEnabled()) {
            return;
        }
        if (!emailConfigService.isEnabled()) {
            return;
        }
        try {
            Locale locale = UserPreferredLanguage.toLocale(recipient.getPreferredLanguage());
            EmailConfig config = emailConfigService.getOrCreate();
            String subject = messageService.get(
                    "notification.email.subject." + type.name().toLowerCase(Locale.ROOT),
                    null,
                    locale);
            String body = buildBody(recipient, type, payload, locale);
            mailService.sendEmail(config, recipient.getEmail(), subject, body);
        } catch (MessagingException ex) {
            log.warn("Payment email notification failed for user {}: {}", recipient.getId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Payment email notification skipped for user {}: {}", recipient.getId(), ex.getMessage());
        }
    }

    private String buildBody(
            User recipient,
            NotificationType type,
            PaymentNotificationPayload payload,
            Locale locale) {
        String hello = messageService.get(
                "notification.email.hello",
                new Object[]{displayName(recipient, locale)},
                locale);
        String eventKey = "notification.email.event." + type.name().toLowerCase(Locale.ROOT);
        String event = eventMessage(type, payload, eventKey, locale);
        String openLabel = messageService.get("notification.email.open_wallet", null, locale);
        String footer = messageService.get("notification.email.footer.payment", null, locale);
        String link = normalizeFrontendUrl() + "/wallet";
        return hello + "\n\n" + event + "\n\n" + openLabel + "\n" + link + "\n\n" + footer + "\n";
    }

    private String eventMessage(
            NotificationType type,
            PaymentNotificationPayload payload,
            String eventKey,
            Locale locale) {
        return switch (type) {
            case PAYMENT_TOP_UP_SUCCESS -> messageService.get(
                    eventKey, new Object[]{payload.amount(), payload.ref()}, locale);
            case PAYMENT_TOP_UP_FAILED -> messageService.get(
                    eventKey, new Object[]{payload.amount(), payload.reasonOrNote()}, locale);
            case PAYMENT_TOP_UP_CANCELLED -> messageService.get(
                    eventKey, new Object[]{payload.amount()}, locale);
            case PAYMENT_WALLET_ADJUSTED -> {
                String dir = messageService.get(
                        "notification.payment.direction."
                                + ("debit".equalsIgnoreCase(payload.direction()) ? "debit" : "credit"),
                        null,
                        locale);
                yield messageService.get(
                        eventKey, new Object[]{dir, payload.amount(), payload.reasonOrNote()}, locale);
            }
            default -> messageService.get(eventKey, null, locale);
        };
    }

    private String normalizeFrontendUrl() {
        String base = frontendUrl != null ? frontendUrl.trim() : "http://localhost:4200";
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
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
        return StringUtils.hasText(user.getEmail())
                ? user.getEmail()
                : messageService.get("notification.someone", null, locale);
    }

    /**
     * Shared payload for payment email/SMS templates.
     */
    public record PaymentNotificationPayload(
            String amount,
            String ref,
            String reasonOrNote,
            String direction) {

        public static PaymentNotificationPayload of(
                String amount,
                String ref,
                String reasonOrNote,
                String direction) {
            return new PaymentNotificationPayload(
                    amount != null ? amount : "0",
                    ref != null ? ref : "",
                    reasonOrNote != null ? reasonOrNote : "",
                    direction != null ? direction : "");
        }
    }
}
