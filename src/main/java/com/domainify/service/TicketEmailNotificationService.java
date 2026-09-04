package com.domainify.service;

import com.domainify.entity.EmailConfig;
import com.domainify.entity.NotificationType;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketSettings;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.User;
import com.domainify.repository.TicketSettingsRepository;
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
 * Sends opt-in email alerts for ticket reply, assignment, and status-change events.
 * Gate order: SMTP → global ticket emails → category → user preference.
 * Failures are logged and never fail the in-app notification path.
 */
@Service
public class TicketEmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TicketEmailNotificationService.class);

    private static final Set<NotificationType> EMAIL_TYPES = EnumSet.of(
            NotificationType.TICKET_CUSTOMER_REPLY,
            NotificationType.TICKET_STAFF_REPLY,
            NotificationType.TICKET_ASSIGNED,
            NotificationType.TICKET_TRANSFERRED,
            NotificationType.TICKET_STATUS_CHANGED,
            NotificationType.TICKET_CLOSED,
            NotificationType.TICKET_REOPENED
    );

    private final EmailConfigService emailConfigService;
    private final MailService mailService;
    private final TicketSettingsRepository ticketSettingsRepository;
    private final MessageService messageService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public TicketEmailNotificationService(
            EmailConfigService emailConfigService,
            MailService mailService,
            TicketSettingsRepository ticketSettingsRepository,
            MessageService messageService) {
        this.emailConfigService = emailConfigService;
        this.mailService = mailService;
        this.ticketSettingsRepository = ticketSettingsRepository;
        this.messageService = messageService;
    }

    public void sendIfConfigured(
            User recipient,
            User actor,
            NotificationType type,
            Ticket ticket,
            TicketStatus from,
            TicketStatus to) {
        if (recipient == null || type == null || ticket == null || !EMAIL_TYPES.contains(type)) {
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
        if (!isGlobalTicketEmailEnabled()) {
            return;
        }
        if (!isCategoryEmailEnabled(ticket.getCategory())) {
            return;
        }
        if (!isPriorityEnabledForEmail(ticket.getPriority())) {
            return;
        }

        try {
            Locale locale = UserPreferredLanguage.toLocale(recipient.getPreferredLanguage());
            EmailConfig config = emailConfigService.getOrCreate();
            String subject = buildSubject(type, ticket, locale);
            String body = buildBody(recipient, actor, type, ticket, from, to, locale);
            mailService.sendEmail(config, recipient.getEmail(), subject, body);
        } catch (MessagingException ex) {
            log.warn("Ticket email notification failed for user {}: {}", recipient.getId(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("Ticket email notification skipped for user {}: {}", recipient.getId(), ex.getMessage());
        }
    }

    private boolean isGlobalTicketEmailEnabled() {
        return ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .map(TicketSettings::isTicketEmailNotificationsEnabled)
                .orElse(true);
    }

    private boolean isCategoryEmailEnabled(TicketCategory category) {
        // No category on ticket → allow (global + user still apply).
        return category == null || category.isEmailNotificationsEnabled();
    }

    private boolean isPriorityEnabledForEmail(TicketPriority priority) {
        return ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .map(settings -> settings.allowsEmailForPriority(priority))
                .orElse(true);
    }

    private String buildSubject(NotificationType type, Ticket ticket, Locale locale) {
        String ref = ticketRef(ticket);
        String key = switch (type) {
            case TICKET_CUSTOMER_REPLY, TICKET_STAFF_REPLY -> "notification.email.subject.reply";
            case TICKET_ASSIGNED, TICKET_TRANSFERRED -> "notification.email.subject.assigned";
            case TICKET_STATUS_CHANGED -> "notification.email.subject.status";
            case TICKET_CLOSED -> "notification.email.subject.closed";
            case TICKET_REOPENED -> "notification.email.subject.reopened";
            default -> "notification.email.subject.update";
        };
        return messageService.get(key, new Object[]{ref}, locale);
    }

    private String buildBody(
            User recipient,
            User actor,
            NotificationType type,
            Ticket ticket,
            TicketStatus from,
            TicketStatus to,
            Locale locale) {
        String actorName = displayName(actor, locale);
        String subjectLine = StringUtils.hasText(ticket.getSubject())
                ? ticket.getSubject()
                : messageService.get("notification.email.no_subject", locale);
        String link = ticketLink(recipient, ticket);
        String eventLine = switch (type) {
            case TICKET_CUSTOMER_REPLY -> messageService.get(
                    "notification.email.event.customer_reply",
                    new Object[]{actorName, ticketRef(ticket)},
                    locale);
            case TICKET_STAFF_REPLY -> messageService.get(
                    "notification.email.event.staff_reply",
                    new Object[]{actorName},
                    locale);
            case TICKET_ASSIGNED -> messageService.get(
                    "notification.email.event.assigned",
                    new Object[]{actorName},
                    locale);
            case TICKET_TRANSFERRED -> messageService.get(
                    "notification.email.event.transferred",
                    new Object[]{actorName},
                    locale);
            case TICKET_STATUS_CHANGED -> messageService.get(
                    "notification.email.event.status",
                    new Object[]{label(from, locale), label(to, locale), actorName},
                    locale);
            case TICKET_CLOSED -> messageService.get(
                    "notification.email.event.closed",
                    new Object[]{actorName},
                    locale);
            case TICKET_REOPENED -> messageService.get(
                    "notification.email.event.reopened",
                    new Object[]{actorName},
                    locale);
            default -> messageService.get("notification.email.event.update", locale);
        };

        return messageService.get("notification.email.hello", new Object[]{displayName(recipient, locale)}, locale)
                + "\n\n"
                + eventLine
                + "\n\n"
                + messageService.get("notification.email.ticket_label", new Object[]{ticketRef(ticket)}, locale)
                + "\n"
                + messageService.get("notification.email.subject_label", new Object[]{subjectLine}, locale)
                + "\n\n"
                + messageService.get("notification.email.open_link", locale)
                + "\n"
                + link
                + "\n\n"
                + messageService.get("notification.email.footer", locale)
                + "\n\n"
                + messageService.get("notification.email.signature", locale);
    }

    private String ticketLink(User recipient, Ticket ticket) {
        String base = frontendUrl == null ? "http://localhost:4200" : frontendUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (recipient.getRole() == User.Role.ADMIN) {
            return base + "/admin/tickets/" + ticket.getId();
        }
        return base + "/tickets/mine/" + ticket.getId();
    }

    private String ticketRef(Ticket ticket) {
        if (StringUtils.hasText(ticket.getPublicNumber())) {
            return "#" + ticket.getPublicNumber();
        }
        return "#" + ticket.getId();
    }

    private String label(TicketStatus status, Locale locale) {
        if (status == null) {
            return messageService.get("notification.ticket.status.unknown", locale);
        }
        return messageService.get("notification.ticket.status." + status.name(), locale);
    }

    private String displayName(User user, Locale locale) {
        if (user == null) {
            return messageService.get("notification.someone", locale);
        }
        String name = (StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : "")
                + " "
                + (StringUtils.hasText(user.getLastName()) ? user.getLastName() : "");
        name = name.trim();
        return StringUtils.hasText(name) ? name : user.getEmail();
    }
}
