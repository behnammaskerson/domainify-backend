package com.domainify.service;

import com.domainify.dto.NotificationDto;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.UnreadCountDto;
import com.domainify.entity.Domain;
import com.domainify.entity.DomainListingOffer;
import com.domainify.entity.InAppNotification;
import com.domainify.entity.NotificationType;
import com.domainify.entity.PaymentIntent;
import com.domainify.entity.PaymentSettings;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.InAppNotificationRepository;
import com.domainify.repository.TicketWatcherRepository;
import com.domainify.repository.UserRepository;
import com.domainify.service.PaymentEmailNotificationService.PaymentNotificationPayload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class NotificationService {

    private final InAppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TicketWatcherRepository ticketWatcherRepository;
    private final TicketEmailNotificationService ticketEmailNotificationService;
    private final TicketSmsNotificationService ticketSmsNotificationService;
    private final OfferEmailNotificationService offerEmailNotificationService;
    private final OfferSmsNotificationService offerSmsNotificationService;
    private final PaymentSettingsService paymentSettingsService;
    private final PaymentEmailNotificationService paymentEmailNotificationService;
    private final PaymentSmsNotificationService paymentSmsNotificationService;

    public NotificationService(
            InAppNotificationRepository notificationRepository,
            UserRepository userRepository,
            TicketWatcherRepository ticketWatcherRepository,
            TicketEmailNotificationService ticketEmailNotificationService,
            TicketSmsNotificationService ticketSmsNotificationService,
            OfferEmailNotificationService offerEmailNotificationService,
            OfferSmsNotificationService offerSmsNotificationService,
            PaymentSettingsService paymentSettingsService,
            PaymentEmailNotificationService paymentEmailNotificationService,
            PaymentSmsNotificationService paymentSmsNotificationService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.ticketWatcherRepository = ticketWatcherRepository;
        this.ticketEmailNotificationService = ticketEmailNotificationService;
        this.ticketSmsNotificationService = ticketSmsNotificationService;
        this.offerEmailNotificationService = offerEmailNotificationService;
        this.offerSmsNotificationService = offerSmsNotificationService;
        this.paymentSettingsService = paymentSettingsService;
        this.paymentEmailNotificationService = paymentEmailNotificationService;
        this.paymentSmsNotificationService = paymentSmsNotificationService;
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
    public void notifyDomainRenewal(User owner, Domain domain, int windowDays) {
        if (owner == null || owner.getId() == null || domain == null || domain.getExpiresAt() == null) {
            return;
        }
        InAppNotification notification = new InAppNotification();
        notification.setRecipient(owner);
        notification.setActor(null);
        notification.setType(NotificationType.DOMAIN_RENEWAL);
        notification.setTicket(null);
        // Reuse ticket display slots: subject = domain name, public number = expiry date, window in unused path via subject only
        notification.setTicketSubject(truncate(domain.getName(), 200));
        notification.setTicketPublicNumber(domain.getExpiresAt().toString());
        notification.setStatusFrom(null);
        notification.setStatusTo(null);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyOfferReceived(DomainListingOffer offer, User actor) {
        notifyOfferParty(offer, actor, offer != null ? offer.getSeller() : null, NotificationType.OFFER_RECEIVED);
    }

    @Transactional
    public void notifyOfferCountered(DomainListingOffer offer, User actor) {
        if (offer == null || actor == null || actor.getId() == null) {
            return;
        }
        User recipient = actor.getId().equals(offer.getBuyer() != null ? offer.getBuyer().getId() : null)
                ? offer.getSeller()
                : offer.getBuyer();
        notifyOfferParty(offer, actor, recipient, NotificationType.OFFER_COUNTERED);
    }

    @Transactional
    public void notifyOfferAccepted(DomainListingOffer offer, User actor) {
        notifyOfferOtherParty(offer, actor, NotificationType.OFFER_ACCEPTED);
    }

    @Transactional
    public void notifyOfferRejected(DomainListingOffer offer, User actor) {
        notifyOfferOtherParty(offer, actor, NotificationType.OFFER_REJECTED);
    }

    @Transactional
    public void notifyOfferWithdrawn(DomainListingOffer offer, User actor) {
        notifyOfferParty(offer, actor, offer != null ? offer.getSeller() : null, NotificationType.OFFER_WITHDRAWN);
    }

    @Transactional
    public void notifyOfferExpired(DomainListingOffer offer) {
        if (offer == null) {
            return;
        }
        notifyOfferParty(offer, null, offer.getBuyer(), NotificationType.OFFER_EXPIRED);
        notifyOfferParty(offer, null, offer.getSeller(), NotificationType.OFFER_EXPIRED);
    }

    @Transactional
    public void notifyPaymentTopUpSuccess(User user, PaymentIntent intent) {
        if (intent == null) {
            return;
        }
        String amount = String.valueOf(intent.getAmountIrt());
        String ref = intent.getRefId() != null
                ? String.valueOf(intent.getRefId())
                : String.valueOf(intent.getId());
        notifyPayment(
                user,
                null,
                NotificationType.PAYMENT_TOP_UP_SUCCESS,
                PaymentNotificationPayload.of(amount, ref, null, null),
                amount,
                ref);
    }

    @Transactional
    public void notifyPaymentTopUpFailed(User user, PaymentIntent intent) {
        if (intent == null) {
            return;
        }
        String amount = String.valueOf(intent.getAmountIrt());
        String reason = StringUtils.hasText(intent.getFailureReason()) ? intent.getFailureReason() : "—";
        String publicRef = intent.getId() != null ? String.valueOf(intent.getId()) : "";
        notifyPayment(
                user,
                null,
                NotificationType.PAYMENT_TOP_UP_FAILED,
                PaymentNotificationPayload.of(amount, publicRef, reason, null),
                amount,
                publicRef);
    }

    @Transactional
    public void notifyPaymentTopUpCancelled(User user, PaymentIntent intent) {
        if (intent == null) {
            return;
        }
        String amount = String.valueOf(intent.getAmountIrt());
        String publicRef = intent.getId() != null ? String.valueOf(intent.getId()) : "";
        notifyPayment(
                user,
                null,
                NotificationType.PAYMENT_TOP_UP_CANCELLED,
                PaymentNotificationPayload.of(amount, publicRef, null, null),
                amount,
                publicRef);
    }

    @Transactional
    public void notifyWalletAdjusted(
            User user,
            User actor,
            String direction,
            BigDecimal amount,
            String note) {
        String amountText = amount != null ? amount.toPlainString() : "0";
        String directionKey = direction != null ? direction.trim().toLowerCase(Locale.ROOT) : "credit";
        if (!"debit".equals(directionKey)) {
            directionKey = "credit";
        }
        String noteText = StringUtils.hasText(note) ? note.trim() : "—";
        String subject = amountText + (StringUtils.hasText(note) ? " — " + note.trim() : "");
        notifyPayment(
                user,
                actor,
                NotificationType.PAYMENT_WALLET_ADJUSTED,
                PaymentNotificationPayload.of(amountText, directionKey, noteText, directionKey),
                subject,
                directionKey);
    }

    private void notifyPayment(
            User user,
            User actor,
            NotificationType type,
            PaymentNotificationPayload payload,
            String ticketSubject,
            String ticketPublicNumber) {
        if (user == null || user.getId() == null || !user.isEnabled()) {
            return;
        }
        if (!user.isPaymentNotificationsEnabled()) {
            return;
        }
        PaymentSettings settings = paymentSettingsService.getOrCreate();
        if (!settings.isPaymentNotificationsEnabled()) {
            return;
        }
        if (settings.isPaymentInAppNotificationsEnabled()) {
            InAppNotification notification = new InAppNotification();
            notification.setRecipient(user);
            notification.setActor(actor);
            notification.setType(type);
            notification.setTicket(null);
            notification.setTicketSubject(truncate(ticketSubject, 200));
            notification.setTicketPublicNumber(truncate(ticketPublicNumber, 32));
            notification.setStatusFrom(null);
            notification.setStatusTo(null);
            notification.setRead(false);
            notificationRepository.save(notification);
        }
        if (settings.isPaymentEmailNotificationsEnabled()) {
            paymentEmailNotificationService.sendIfConfigured(user, actor, type, payload);
        }
        if (settings.isPaymentSmsNotificationsEnabled()) {
            paymentSmsNotificationService.sendIfConfigured(user, actor, type, payload);
        }
    }

    private void notifyOfferOtherParty(DomainListingOffer offer, User actor, NotificationType type) {
        if (offer == null || actor == null || actor.getId() == null) {
            return;
        }
        User recipient = actor.getId().equals(offer.getBuyer() != null ? offer.getBuyer().getId() : null)
                ? offer.getSeller()
                : offer.getBuyer();
        notifyOfferParty(offer, actor, recipient, type);
    }

    private void notifyOfferParty(
            DomainListingOffer offer,
            User actor,
            User recipient,
            NotificationType type) {
        if (offer == null || recipient == null || recipient.getId() == null || !recipient.isEnabled()) {
            return;
        }
        if (actor != null && actor.getId() != null && actor.getId().equals(recipient.getId())) {
            return;
        }
        String domainName = "";
        if (offer.getListing() != null && offer.getListing().getDomain() != null) {
            domainName = offer.getListing().getDomain().getName();
        }
        String amountText = offer.getAmount() != null ? offer.getAmount().toPlainString() : "";
        InAppNotification notification = new InAppNotification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setTicket(null);
        notification.setTicketSubject(truncate(domainName, 200));
        notification.setTicketPublicNumber(truncate(amountText, 32));
        notification.setStatusFrom(null);
        notification.setStatusTo(null);
        notification.setRead(false);
        notificationRepository.save(notification);
        offerEmailNotificationService.sendIfConfigured(recipient, actor, type, offer);
        offerSmsNotificationService.sendIfConfigured(recipient, actor, type, offer);
    }

    @Transactional
    public void onTicketCreated(Ticket ticket, User requester) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        List<User> admins = userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN);
        for (User admin : admins) {
            if (isSameUser(admin, requester)) {
                continue;
            }
            if (notified.add(admin.getId())) {
                createNotification(admin, requester, NotificationType.TICKET_CREATED, ticket, null, null);
            }
        }
    }

    @Transactional
    public void onCustomerReply(Ticket ticket, User customer) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        User assignee = ticket.getAssignee();
        if (assignee != null && assignee.isEnabled()) {
            if (!isSameUser(assignee, customer) && notified.add(assignee.getId())) {
                createNotification(assignee, customer, NotificationType.TICKET_CUSTOMER_REPLY, ticket, null, null);
            }
        } else {
            notifyAllAdminsExcept(customer, customer, NotificationType.TICKET_CUSTOMER_REPLY, ticket, null, null, notified);
        }
        notifyWatchers(ticket, customer, NotificationType.TICKET_CUSTOMER_REPLY, null, null, notified);
    }

    @Transactional
    public void onStaffPublicReply(Ticket ticket, User staff) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        User requester = ticket.getRequester();
        if (requester != null && requester.isEnabled() && !isSameUser(requester, staff) && notified.add(requester.getId())) {
            createNotification(requester, staff, NotificationType.TICKET_STAFF_REPLY, ticket, null, null);
        }
        notifyWatchers(ticket, staff, NotificationType.TICKET_STAFF_REPLY, null, null, notified);
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
        Set<Long> notified = new HashSet<>();
        if (byStaff) {
            User requester = ticket.getRequester();
            if (requester != null && requester.isEnabled() && !isSameUser(requester, actor) && notified.add(requester.getId())) {
                createNotification(requester, actor, NotificationType.TICKET_STATUS_CHANGED, ticket, from, to);
            }
            notifyWatchers(ticket, actor, NotificationType.TICKET_STATUS_CHANGED, from, to, notified);
            return;
        }
        notifyStaffForTicket(ticket, actor, NotificationType.TICKET_STATUS_CHANGED, from, to, notified);
        notifyWatchers(ticket, actor, NotificationType.TICKET_STATUS_CHANGED, from, to, notified);
    }

    @Transactional
    public void onAssigned(Ticket ticket, User assignee, User actor) {
        if (ticket == null || assignee == null || !assignee.isEnabled()) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        if (!isSameUser(assignee, actor) && notified.add(assignee.getId())) {
            createNotification(assignee, actor, NotificationType.TICKET_ASSIGNED, ticket, null, null);
        }
        notifyWatchers(ticket, actor, NotificationType.TICKET_ASSIGNED, null, null, notified);
    }

    @Transactional
    public void onUnassigned(Ticket ticket, User previousAssignee, User actor) {
        if (ticket == null || previousAssignee == null || !previousAssignee.isEnabled()) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        if (!isSameUser(previousAssignee, actor) && notified.add(previousAssignee.getId())) {
            createNotification(previousAssignee, actor, NotificationType.TICKET_UNASSIGNED, ticket, null, null);
        }
        notifyWatchers(ticket, actor, NotificationType.TICKET_UNASSIGNED, null, null, notified);
    }

    @Transactional
    public void onClosed(Ticket ticket, User actor, boolean byStaff) {
        if (ticket == null) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        if (byStaff) {
            User requester = ticket.getRequester();
            if (requester != null && requester.isEnabled() && !isSameUser(requester, actor) && notified.add(requester.getId())) {
                createNotification(requester, actor, NotificationType.TICKET_CLOSED, ticket, null, TicketStatus.CLOSED);
            }
            notifyWatchers(ticket, actor, NotificationType.TICKET_CLOSED, null, TicketStatus.CLOSED, notified);
            return;
        }
        notifyStaffForTicket(ticket, actor, NotificationType.TICKET_CLOSED, null, TicketStatus.CLOSED, notified);
        notifyWatchers(ticket, actor, NotificationType.TICKET_CLOSED, null, TicketStatus.CLOSED, notified);
    }

    @Transactional
    public void onReopened(Ticket ticket, User actor, boolean byStaff) {
        if (ticket == null) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        if (byStaff) {
            User requester = ticket.getRequester();
            if (requester != null && requester.isEnabled() && !isSameUser(requester, actor)
                    && notified.add(requester.getId())) {
                createNotification(requester, actor, NotificationType.TICKET_REOPENED, ticket,
                        TicketStatus.CLOSED, TicketStatus.OPEN);
            }
            notifyWatchers(ticket, actor, NotificationType.TICKET_REOPENED, TicketStatus.CLOSED, TicketStatus.OPEN, notified);
            return;
        }
        notifyStaffForTicket(ticket, actor, NotificationType.TICKET_REOPENED, TicketStatus.CLOSED, TicketStatus.OPEN, notified);
        notifyWatchers(ticket, actor, NotificationType.TICKET_REOPENED, TicketStatus.CLOSED, TicketStatus.OPEN, notified);
    }

    @Transactional
    public void onWatcherAdded(Ticket ticket, User watcher, User actor) {
        if (ticket == null || watcher == null || !watcher.isEnabled()) {
            return;
        }
        if (isSameUser(watcher, actor)) {
            return;
        }
        createNotification(watcher, actor, NotificationType.TICKET_WATCHER_ADDED, ticket, null, null);
    }

    @Transactional
    public void onTransferred(Ticket ticket, User actor, User previousAssignee, User nextAssignee) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        if (previousAssignee != null && previousAssignee.isEnabled()
                && !isSameUser(previousAssignee, actor)
                && (nextAssignee == null || !isSameUser(previousAssignee, nextAssignee))
                && notified.add(previousAssignee.getId())) {
            createNotification(previousAssignee, actor, NotificationType.TICKET_UNASSIGNED, ticket, null, null);
        }
        if (nextAssignee != null && nextAssignee.isEnabled()
                && !isSameUser(nextAssignee, actor)
                && notified.add(nextAssignee.getId())) {
            createNotification(nextAssignee, actor, NotificationType.TICKET_TRANSFERRED, ticket, null, null);
        }
        notifyWatchers(ticket, actor, NotificationType.TICKET_TRANSFERRED, null, null, notified);
    }

    @Transactional
    public void onEscalated(Ticket ticket, User actor) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        Set<Long> notified = new HashSet<>();
        User assignee = ticket.getAssignee();
        if (assignee != null && assignee.isEnabled() && !isSameUser(assignee, actor) && notified.add(assignee.getId())) {
            createNotification(assignee, actor, NotificationType.TICKET_ESCALATED, ticket, null, null);
        } else if (assignee == null) {
            notifyAllAdminsExcept(actor, actor, NotificationType.TICKET_ESCALATED, ticket, null, null, notified);
        }
        notifyWatchers(ticket, actor, NotificationType.TICKET_ESCALATED, null, null, notified);
    }

    private void notifyWatchers(
            Ticket ticket,
            User actor,
            NotificationType type,
            TicketStatus from,
            TicketStatus to,
            Set<Long> alreadyNotified) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }
        Set<Long> notified = alreadyNotified != null ? alreadyNotified : new HashSet<>();
        for (User watcher : ticketWatcherRepository.findUsersByTicketId(ticket.getId())) {
            if (watcher == null || watcher.getId() == null || !watcher.isEnabled()) {
                continue;
            }
            if (isSameUser(watcher, actor)) {
                continue;
            }
            if (!notified.add(watcher.getId())) {
                continue;
            }
            createNotification(watcher, actor, type, ticket, from, to);
        }
    }

    private void notifyStaffForTicket(
            Ticket ticket,
            User actor,
            NotificationType type,
            TicketStatus from,
            TicketStatus to,
            Set<Long> notified) {
        User assignee = ticket.getAssignee();
        if (assignee != null && assignee.isEnabled() && !isSameUser(assignee, actor) && notified.add(assignee.getId())) {
            createNotification(assignee, actor, type, ticket, from, to);
            return;
        }
        if (assignee == null) {
            notifyAllAdminsExcept(actor, actor, type, ticket, from, to, notified);
        }
    }

    private void notifyAllAdminsExcept(
            User exclude,
            User actor,
            NotificationType type,
            Ticket ticket,
            TicketStatus from,
            TicketStatus to,
            Set<Long> notified) {
        List<User> admins = userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN);
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
