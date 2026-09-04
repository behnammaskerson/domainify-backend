package com.domainify.service;

import com.domainify.dto.SmsBulkSendRequest;
import com.domainify.dto.SmsBulkSendResultDto;
import com.domainify.entity.NotificationType;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketSettings;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.User;
import com.domainify.repository.TicketSettingsRepository;
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
 * Sends opt-in SMS alerts for ticket events at admin-configured priorities.
 * Gate order: provider → global SMS → category SMS → user opt-in → verified phone → priority.
 * Failures are logged and never fail the in-app notification path.
 */
@Service
public class TicketSmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TicketSmsNotificationService.class);

    private static final Set<NotificationType> SMS_TYPES = EnumSet.of(
            NotificationType.TICKET_CUSTOMER_REPLY,
            NotificationType.TICKET_STAFF_REPLY,
            NotificationType.TICKET_ASSIGNED,
            NotificationType.TICKET_TRANSFERRED,
            NotificationType.TICKET_STATUS_CHANGED,
            NotificationType.TICKET_CLOSED,
            NotificationType.TICKET_REOPENED
    );

    private final SmsService smsService;
    private final SmsConfigService smsConfigService;
    private final TicketSettingsRepository ticketSettingsRepository;
    private final MessageService messageService;

    public TicketSmsNotificationService(
            SmsService smsService,
            SmsConfigService smsConfigService,
            TicketSettingsRepository ticketSettingsRepository,
            MessageService messageService) {
        this.smsService = smsService;
        this.smsConfigService = smsConfigService;
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
        if (recipient == null || type == null || ticket == null || !SMS_TYPES.contains(type)) {
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
        if (!isGlobalTicketSmsEnabled()) {
            return;
        }
        if (!isCategorySmsEnabled(ticket.getCategory())) {
            return;
        }
        if (!isPriorityEnabledForSms(ticket.getPriority())) {
            return;
        }

        String mobile = PhoneSmsUtil.toSmsMobile(recipient.getPhoneCountryCode(), recipient.getPhoneNumber());
        if (!StringUtils.hasText(mobile)) {
            return;
        }

        try {
            Locale locale = UserPreferredLanguage.toLocale(recipient.getPreferredLanguage());
            SmsBulkSendRequest request = new SmsBulkSendRequest();
            request.setMobiles(List.of(mobile));
            request.setMessageText(buildMessage(actor, type, ticket, from, to, locale));
            SmsBulkSendResultDto result = smsService.sendBulk(request);
            if (result == null || !result.isSuccess()) {
                log.warn("Ticket SMS notification failed for user {}: provider rejected send", recipient.getId());
            }
        } catch (Exception ex) {
            log.warn("Ticket SMS notification skipped for user {}: {}", recipient.getId(), ex.getMessage());
        }
    }

    private boolean isSmsProviderConfigured() {
        return StringUtils.hasText(smsConfigService.getApiKey())
                && StringUtils.hasText(smsConfigService.getDefaultLine());
    }

    private boolean isGlobalTicketSmsEnabled() {
        return ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .map(TicketSettings::isTicketSmsNotificationsEnabled)
                .orElse(true);
    }

    private boolean isCategorySmsEnabled(TicketCategory category) {
        return category == null || category.isSmsNotificationsEnabled();
    }

    private boolean isPriorityEnabledForSms(TicketPriority priority) {
        return ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .map(settings -> settings.allowsSmsForPriority(priority))
                .orElse(priority == TicketPriority.URGENT);
    }

    private String buildMessage(
            User actor,
            NotificationType type,
            Ticket ticket,
            TicketStatus from,
            TicketStatus to,
            Locale locale) {
        String ref = ticketRef(ticket);
        String actorName = displayName(actor, locale);
        String event = switch (type) {
            case TICKET_CUSTOMER_REPLY, TICKET_STAFF_REPLY -> messageService.get(
                    "notification.sms.event.reply",
                    new Object[]{actorName},
                    locale);
            case TICKET_ASSIGNED, TICKET_TRANSFERRED -> messageService.get(
                    "notification.sms.event.assigned",
                    new Object[]{actorName},
                    locale);
            case TICKET_STATUS_CHANGED -> messageService.get(
                    "notification.sms.event.status",
                    new Object[]{label(from, locale), label(to, locale)},
                    locale);
            case TICKET_CLOSED -> messageService.get(
                    "notification.sms.event.closed",
                    new Object[]{actorName},
                    locale);
            case TICKET_REOPENED -> messageService.get(
                    "notification.sms.event.reopened",
                    new Object[]{actorName},
                    locale);
            default -> messageService.get("notification.sms.event.update", locale);
        };
        String prefix = ticket.getPriority() != null
                ? messageService.get("notification.ticket.priority." + ticket.getPriority().name(), locale) + " "
                : "";
        String text = prefix + ref + ": " + event;
        return text.length() <= 160 ? text : text.substring(0, 157) + "...";
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
        return StringUtils.hasText(name) ? name : messageService.get("notification.someone", locale);
    }
}
