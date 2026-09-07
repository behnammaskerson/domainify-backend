package com.domainify.service;

import com.domainify.entity.DomainListingOffer;
import com.domainify.entity.EmailConfig;
import com.domainify.entity.NotificationType;
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
 * Opt-in email alerts for marketplace offer events (user email preference + SMTP enabled).
 */
@Service
public class OfferEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OfferEmailNotificationService.class);

    private static final Set<NotificationType> EMAIL_TYPES = EnumSet.of(
            NotificationType.OFFER_RECEIVED,
            NotificationType.OFFER_COUNTERED,
            NotificationType.OFFER_ACCEPTED,
            NotificationType.OFFER_REJECTED,
            NotificationType.OFFER_WITHDRAWN,
            NotificationType.OFFER_EXPIRED
    );

    private final EmailConfigService emailConfigService;
    private final MailService mailService;
    private final MessageService messageService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public OfferEmailNotificationService(
            EmailConfigService emailConfigService,
            MailService mailService,
            MessageService messageService) {
        this.emailConfigService = emailConfigService;
        this.mailService = mailService;
        this.messageService = messageService;
    }

    public void sendIfConfigured(
            User recipient,
            User actor,
            NotificationType type,
            DomainListingOffer offer) {
        if (recipient == null || type == null || offer == null || !EMAIL_TYPES.contains(type)) {
            return;
        }
        if (!recipient.isEnabled() || !recipient.isEmailNotificationsEnabled()) {
            return;
        }
        if (!StringUtils.hasText(recipient.getEmail())) {
            return;
        }
        if (!emailConfigService.isEnabled()) {
            return;
        }
        try {
            Locale locale = UserPreferredLanguage.toLocale(recipient.getPreferredLanguage());
            EmailConfig config = emailConfigService.getOrCreate();
            String domainName = domainName(offer);
            String amount = amountText(offer);
            String actorName = displayName(actor, locale);
            String subject = messageService.get(
                    "notification.email.subject." + type.name().toLowerCase(Locale.ROOT),
                    new Object[]{domainName},
                    locale);
            String body = buildBody(recipient, actorName, type, domainName, amount, locale);
            mailService.sendEmail(config, recipient.getEmail(), subject, body);
        } catch (MessagingException ex) {
            log.warn("Offer email notification failed for user {}: {}", recipient.getId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Offer email notification skipped for user {}: {}", recipient.getId(), ex.getMessage());
        }
    }

    private String buildBody(
            User recipient,
            String actorName,
            NotificationType type,
            String domainName,
            String amount,
            Locale locale) {
        String hello = messageService.get(
                "notification.email.hello",
                new Object[]{displayName(recipient, locale)},
                locale);
        String eventKey = "notification.email.event." + type.name().toLowerCase(Locale.ROOT);
        String event = type == NotificationType.OFFER_EXPIRED
                ? messageService.get(eventKey, new Object[]{domainName, amount}, locale)
                : messageService.get(eventKey, new Object[]{actorName, amount, domainName}, locale);
        String openLabel = messageService.get("notification.email.open_offers", null, locale);
        String footer = messageService.get("notification.email.footer.offer", null, locale);
        String link = normalizeFrontendUrl() + offerPath(type);
        return hello + "\n\n" + event + "\n\n" + openLabel + "\n" + link + "\n\n" + footer + "\n";
    }

    private String offerPath(NotificationType type) {
        if (type == NotificationType.OFFER_RECEIVED || type == NotificationType.OFFER_WITHDRAWN) {
            return "/marketplace/my-listings";
        }
        if (type == NotificationType.OFFER_EXPIRED) {
            return "/marketplace/my-offers";
        }
        return "/marketplace/my-offers";
    }

    private String normalizeFrontendUrl() {
        String base = frontendUrl != null ? frontendUrl.trim() : "http://localhost:4200";
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
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
