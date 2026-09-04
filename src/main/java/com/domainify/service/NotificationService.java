package com.domainify.service;

import com.domainify.dto.NotificationDto;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.UnreadCountDto;
import com.domainify.entity.InAppNotification;
import com.domainify.entity.NotificationType;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.InAppNotificationRepository;
import com.domainify.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private final InAppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TicketEmailNotificationService ticketEmailNotificationService;
    private final TicketSmsNotificationService ticketSmsNotificationService;

    public NotificationService(
            InAppNotificationRepository notificationRepository,
            UserRepository userRepository,
            TicketEmailNotificationService ticketEmailNotificationService,
            TicketSmsNotificationService ticketSmsNotificationService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.ticketEmailNotificationService = ticketEmailNotificationService;
        this.ticketSmsNotificationService = ticketSmsNotificationService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> listForUser(User user, Pageable pageable, Boolean unreadOnly) {
        requireUser(user);
        Page<InAppNotification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(user, pageable);
        } else {
            page = notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);
        }
        return PagedResponse.from(page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public UnreadCountDto unreadCount(User user) {
        requireUser(user);
        return new UnreadCountDto(notificationRepository.countByRecipientAndReadFalse(user));
    }

    @Transactional
    public NotificationDto markRead(User user, Long notificationId) {
        requireUser(user);
        InAppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return toDto(notification);
    }

    @Transactional
    public UnreadCountDto markAllRead(User user) {
        requireUser(user);
        notificationRepository.markAllReadForRecipient(user);
        return new UnreadCountDto(0);
    }

    @Transactional
    public void onTicketCreated(Ticket ticket, User requester) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        List<User> admins = userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN);
        for (User admin : admins) {
            if (isSameUser(admin, requester)) {
                continue;
            }
            createNotification(admin, requester, NotificationType.TICKET_CREATED, ticket, null, null);
        }
    }

    @Transactional
    public void onCustomerReply(Ticket ticket, User customer) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        User assignee = ticket.getAssignee();
        if (assignee != null && assignee.isEnabled()) {
            if (!isSameUser(assignee, customer)) {
                createNotification(assignee, customer, NotificationType.TICKET_CUSTOMER_REPLY, ticket, null, null);
            }
            return;
        }
        notifyAllAdminsExcept(customer, customer, NotificationType.TICKET_CUSTOMER_REPLY, ticket, null, null);
    }

    @Transactional
    public void onStaffPublicReply(Ticket ticket, User staff) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        User requester = ticket.getRequester();
        if (requester != null && requester.isEnabled() && !isSameUser(requester, staff)) {
            createNotification(requester, staff, NotificationType.TICKET_STAFF_REPLY, ticket, null, null);
        }
    }

    @Transactional
    public void onMention(Ticket ticket, User mentionedUser, User author) {
        if (ticket == null || mentionedUser == null || !mentionedUser.isEnabled()) {
            return;
        }
        if (isSameUser(mentionedUser, author)) {
            return;
        }
        createNotification(mentionedUser, author, NotificationType.TICKET_MENTION, ticket, null, null);
    }

    @Transactional
    public void onStatusChanged(Ticket ticket, User actor, TicketStatus from, TicketStatus to, boolean byStaff) {
        if (ticket == null || from == to) {
            return;
        }
        if (byStaff) {
            User requester = ticket.getRequester();
            if (requester != null && requester.isEnabled() && !isSameUser(requester, actor)) {
                createNotification(requester, actor, NotificationType.TICKET_STATUS_CHANGED, ticket, from, to);
            }
            return;
        }
        notifyStaffForTicket(ticket, actor, NotificationType.TICKET_STATUS_CHANGED, from, to);
    }

    @Transactional
    public void onAssigned(Ticket ticket, User assignee, User actor) {
        if (ticket == null || assignee == null || !assignee.isEnabled()) {
            return;
        }
        if (isSameUser(assignee, actor)) {
            return;
        }
        createNotification(assignee, actor, NotificationType.TICKET_ASSIGNED, ticket, null, null);
    }

    @Transactional
    public void onUnassigned(Ticket ticket, User previousAssignee, User actor) {
        if (ticket == null || previousAssignee == null || !previousAssignee.isEnabled()) {
            return;
        }
        if (isSameUser(previousAssignee, actor)) {
            return;
        }
        createNotification(previousAssignee, actor, NotificationType.TICKET_UNASSIGNED, ticket, null, null);
    }

    @Transactional
    public void onClosed(Ticket ticket, User actor, boolean byStaff) {
        if (ticket == null) {
            return;
        }
        if (byStaff) {
            User requester = ticket.getRequester();
            if (requester != null && requester.isEnabled() && !isSameUser(requester, actor)) {
                createNotification(requester, actor, NotificationType.TICKET_CLOSED, ticket, null, TicketStatus.CLOSED);
            }
            return;
        }
        notifyStaffForTicket(ticket, actor, NotificationType.TICKET_CLOSED, null, TicketStatus.CLOSED);
    }

    @Transactional
    public void onReopened(Ticket ticket, User actor, boolean byStaff) {
        if (ticket == null) {
            return;
        }
        if (byStaff) {
            User requester = ticket.getRequester();
            if (requester != null && requester.isEnabled() && !isSameUser(requester, actor)) {
                createNotification(requester, actor, NotificationType.TICKET_REOPENED, ticket, TicketStatus.CLOSED, TicketStatus.OPEN);
            }
            return;
        }
        notifyStaffForTicket(ticket, actor, NotificationType.TICKET_REOPENED, TicketStatus.CLOSED, TicketStatus.OPEN);
    }

    private void notifyStaffForTicket(
            Ticket ticket,
            User actor,
            NotificationType type,
            TicketStatus from,
            TicketStatus to) {
        User assignee = ticket.getAssignee();
        if (assignee != null && assignee.isEnabled() && !isSameUser(assignee, actor)) {
            createNotification(assignee, actor, type, ticket, from, to);
            return;
        }
        notifyAllAdminsExcept(actor, actor, type, ticket, from, to);
    }

    private void notifyAllAdminsExcept(
            User exclude,
            User actor,
            NotificationType type,
            Ticket ticket,
            TicketStatus from,
            TicketStatus to) {
        List<User> admins = userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN);
        Set<Long> notified = new LinkedHashSet<>();
        for (User admin : admins) {
            if (isSameUser(admin, exclude)) {
                continue;
            }
            if (notified.add(admin.getId())) {
                createNotification(admin, actor, type, ticket, from, to);
            }
        }
    }

    private void createNotification(
            User recipient,
            User actor,
            NotificationType type,
            Ticket ticket,
            TicketStatus from,
            TicketStatus to) {
        if (recipient == null || recipient.getId() == null) {
            return;
        }
        InAppNotification notification = new InAppNotification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setTicket(ticket);
        if (ticket != null) {
            notification.setTicketPublicNumber(ticket.getPublicNumber());
            notification.setTicketSubject(truncate(ticket.getSubject(), 200));
        }
        notification.setStatusFrom(from);
        notification.setStatusTo(to);
        notification.setRead(false);
        notificationRepository.save(notification);
        ticketEmailNotificationService.sendIfConfigured(recipient, actor, type, ticket, from, to);
        ticketSmsNotificationService.sendIfConfigured(recipient, actor, type, ticket, from, to);
    }

    private NotificationDto toDto(InAppNotification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());
        if (notification.getActor() != null) {
            dto.setActorId(notification.getActor().getId());
            dto.setActorName(displayName(notification.getActor()));
        }
        if (notification.getTicket() != null) {
            dto.setTicketId(notification.getTicket().getId());
        }
        dto.setTicketPublicNumber(notification.getTicketPublicNumber());
        dto.setTicketSubject(notification.getTicketSubject());
        dto.setStatusFrom(notification.getStatusFrom());
        dto.setStatusTo(notification.getStatusTo());
        return dto;
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        String name = (StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : "")
                + " "
                + (StringUtils.hasText(user.getLastName()) ? user.getLastName() : "");
        name = name.trim();
        return StringUtils.hasText(name) ? name : user.getEmail();
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private boolean isSameUser(User a, User b) {
        if (a == null || b == null || a.getId() == null || b.getId() == null) {
            return false;
        }
        return a.getId().equals(b.getId());
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
