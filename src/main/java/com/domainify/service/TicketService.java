package com.domainify.service;

import com.domainify.dto.LinkTicketsRequest;
import com.domainify.dto.MergeTicketRequest;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.RelatedTicketDto;
import com.domainify.dto.SplitTicketRequest;
import com.domainify.dto.SplitTicketResultDto;
import com.domainify.dto.SaveTicketReplyDraftRequest;
import com.domainify.dto.TicketAssigneeOptionDto;
import com.domainify.dto.TicketAttachmentDto;
import com.domainify.dto.TicketReplyDraftDto;
import com.domainify.dto.TicketDetailDto;
import com.domainify.dto.TicketDto;
import com.domainify.dto.TicketMessageDto;
import com.domainify.dto.TicketMessageRevisionDto;
import com.domainify.dto.EscalateTicketRequest;
import com.domainify.dto.TicketEscalationDto;
import com.domainify.dto.TicketTransferDto;
import com.domainify.dto.TransferTicketRequest;
import com.domainify.dto.UpdateTicketDueDateRequest;
import com.domainify.dto.UpdateTicketMessageRequest;
import com.domainify.dto.UpdateTicketTagsRequest;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketAttachment;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketChannel;
import com.domainify.entity.TicketEscalation;
import com.domainify.entity.TicketEscalationTrigger;
import com.domainify.entity.TicketMention;
import com.domainify.entity.TicketMessage;
import com.domainify.entity.TicketMessageAttachment;
import com.domainify.entity.TicketMessageRevision;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketQueue;
import com.domainify.entity.TicketReplyDraft;
import com.domainify.entity.TicketRelatedLink;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.TicketTag;
import com.domainify.entity.TicketTransfer;
import com.domainify.entity.TicketWatcher;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketAttachmentRepository;
import com.domainify.repository.TicketEscalationRepository;
import com.domainify.repository.TicketMentionRepository;
import com.domainify.repository.TicketMessageAttachmentRepository;
import com.domainify.repository.TicketMessageRepository;
import com.domainify.repository.TicketMessageRevisionRepository;
import com.domainify.repository.TicketReplyDraftRepository;
import com.domainify.repository.TicketRelatedLinkRepository;
import com.domainify.repository.TicketRepository;
import com.domainify.repository.TicketTransferRepository;
import com.domainify.repository.TicketWatcherRepository;
import com.domainify.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TicketService {

    private static final int SUBJECT_MAX = 200;
    private static final int DESCRIPTION_MAX = 10000;
    private static final int REPLY_MAX = 10000;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "subject", "status", "priority", "publicNumber", "id"
    );

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketMessageRevisionRepository ticketMessageRevisionRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TicketMessageAttachmentRepository ticketMessageAttachmentRepository;
    private final TicketMentionRepository ticketMentionRepository;
    private final TicketReplyDraftRepository ticketReplyDraftRepository;
    private final TicketRelatedLinkRepository ticketRelatedLinkRepository;
    private final TicketWatcherRepository ticketWatcherRepository;
    private final TicketTransferRepository ticketTransferRepository;
    private final TicketEscalationRepository ticketEscalationRepository;
    private final TicketCategoryService ticketCategoryService;
    private final TicketQueueService ticketQueueService;
    private final TicketMentionService ticketMentionService;
    private final TicketStatusWorkflowService ticketStatusWorkflowService;
    private final TicketTagService ticketTagService;
    private final TicketSettingsService ticketSettingsService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final TicketAutoAssignService ticketAutoAssignService;

    public TicketService(
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            TicketMessageRevisionRepository ticketMessageRevisionRepository,
            TicketAttachmentRepository ticketAttachmentRepository,
            TicketMessageAttachmentRepository ticketMessageAttachmentRepository,
            TicketMentionRepository ticketMentionRepository,
            TicketReplyDraftRepository ticketReplyDraftRepository,
            TicketRelatedLinkRepository ticketRelatedLinkRepository,
            TicketWatcherRepository ticketWatcherRepository,
            TicketTransferRepository ticketTransferRepository,
            TicketEscalationRepository ticketEscalationRepository,
            TicketCategoryService ticketCategoryService,
            TicketQueueService ticketQueueService,
            TicketMentionService ticketMentionService,
            TicketStatusWorkflowService ticketStatusWorkflowService,
            TicketTagService ticketTagService,
            TicketSettingsService ticketSettingsService,
            NotificationService notificationService,
            UserRepository userRepository,
            TicketAutoAssignService ticketAutoAssignService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketMessageRevisionRepository = ticketMessageRevisionRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.ticketMessageAttachmentRepository = ticketMessageAttachmentRepository;
        this.ticketMentionRepository = ticketMentionRepository;
        this.ticketReplyDraftRepository = ticketReplyDraftRepository;
        this.ticketRelatedLinkRepository = ticketRelatedLinkRepository;
        this.ticketWatcherRepository = ticketWatcherRepository;
        this.ticketTransferRepository = ticketTransferRepository;
        this.ticketEscalationRepository = ticketEscalationRepository;
        this.ticketCategoryService = ticketCategoryService;
        this.ticketQueueService = ticketQueueService;
        this.ticketMentionService = ticketMentionService;
        this.ticketStatusWorkflowService = ticketStatusWorkflowService;
        this.ticketTagService = ticketTagService;
        this.ticketSettingsService = ticketSettingsService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.ticketAutoAssignService = ticketAutoAssignService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<TicketDto> listMine(User requester, TicketStatus status, String q, Pageable pageable) {
        if (requester == null || requester.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        Specification<Ticket> spec = buildMineSpec(requester.getId(), status, q);
        Pageable safePageable = sanitizePageable(pageable);
        Page<TicketDto> page = ticketRepository.findAll(spec, safePageable).map(this::toListDto);
        return PagedResponse.from(page);
    }

    @Transactional
    public TicketDetailDto getMine(User requester, Long ticketId) {
        Ticket ticket = requireOwnedTicket(requester, ticketId);
        markReadByCustomer(ticket);
        return toDetailDto(ticket, requester, false);
    }

    @Transactional
    public TicketDetailDto getForStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, true);
        markReadByStaff(ticket);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketReplyDraftDto saveReplyDraft(User requester, Long ticketId, SaveTicketReplyDraftRequest request) {
        Ticket ticket = requireOwnedTicket(requester, ticketId);
        return saveReplyDraft(ticket, requester, request, false);
    }

    @Transactional
    public TicketReplyDraftDto saveReplyDraftAsStaff(User agent, Long ticketId, SaveTicketReplyDraftRequest request) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        return saveReplyDraft(ticket, agent, request, true);
    }

    @Transactional
    public TicketDetailDto reply(User requester, Long ticketId, String body, MultipartFile[] attachments) {
        Ticket ticket = requireOwnedTicket(requester, ticketId);
        return addReply(ticket, requester, body, false, attachments, false);
    }

    @Transactional
    public TicketDetailDto replyAsStaff(
            User agent,
            Long ticketId,
            String body,
            boolean internalNote,
            MultipartFile[] attachments) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        return addReply(ticket, agent, body, internalNote, attachments, true);
    }

    private TicketDetailDto addReply(
            Ticket ticket,
            User author,
            String body,
            boolean internalNote,
            MultipartFile[] attachments,
            boolean asStaff) {
        assertNotDeleted(ticket);
        if (ticket.isArchived() || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ApiException(ErrorCode.TICKET_CLOSED_NO_REPLY);
        }

        String trimmedBody = body == null ? "" : body.trim();
        if (!StringUtils.hasText(trimmedBody)) {
            throw new ApiException(ErrorCode.TICKET_REPLY_BODY_REQUIRED);
        }
        if (trimmedBody.length() > REPLY_MAX) {
            throw new ApiException(ErrorCode.TICKET_REPLY_BODY_TOO_LONG);
        }

        List<MultipartFile> files = normalizeFiles(attachments);
        ticketSettingsService.validateAttachmentBatch(files);

        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setAuthor(author);
        message.setBody(trimmedBody);
        message.setInternalNote(asStaff && internalNote);
        for (MultipartFile file : files) {
            message.addAttachment(toMessageAttachment(file));
        }
        ticketMessageRepository.save(message);
        ticketMentionService.syncMentions(ticket, message, trimmedBody, author);

        User previousAssignee = ticket.getAssignee();
        if (asStaff && !internalNote) {
            if (ticket.getStatus() == TicketStatus.NEW || ticket.getStatus() == TicketStatus.OPEN) {
                maybeAutoTransition(ticket, TicketStatus.PENDING);
            }
            if (ticket.getAssignee() == null) {
                ticket.setAssignee(author);
            }
        } else if (!asStaff) {
            if (ticket.getStatus() == TicketStatus.RESOLVED
                    || ticket.getStatus() == TicketStatus.PENDING
                    || ticket.getStatus() == TicketStatus.ON_HOLD) {
                maybeAutoTransition(ticket, TicketStatus.OPEN);
            } else if (ticket.getStatus() == TicketStatus.NEW) {
                maybeAutoTransition(ticket, TicketStatus.OPEN);
            }
        }
        ticketRepository.save(ticket);
        clearReplyDraft(ticket, author);

        if (asStaff && !internalNote) {
            notificationService.onStaffPublicReply(ticket, author);
            if (previousAssignee == null && ticket.getAssignee() != null) {
                notificationService.onAssigned(ticket, ticket.getAssignee(), author);
            }
        } else if (!asStaff) {
            notificationService.onCustomerReply(ticket, author);
        }

        return toDetailDto(ticket, author, isStaffUser(author));
    }

    @Transactional
    public TicketDetailDto editMessage(
            User actor,
            Long ticketId,
            Long messageId,
            UpdateTicketMessageRequest request,
            boolean asStaff) {
        Ticket ticket = asStaff ? requireStaffTicket(ticketId, false) : requireOwnedTicket(actor, ticketId);
        TicketMessage message = requireEditableMessage(actor, ticket, messageId, asStaff);

        String trimmedBody = request.getBody() == null ? "" : request.getBody().trim();
        if (!StringUtils.hasText(trimmedBody)) {
            throw new ApiException(ErrorCode.TICKET_REPLY_BODY_REQUIRED);
        }
        if (trimmedBody.length() > REPLY_MAX) {
            throw new ApiException(ErrorCode.TICKET_REPLY_BODY_TOO_LONG);
        }
        if (trimmedBody.equals(message.getBody())) {
            return toDetailDto(ticket, actor, asStaff);
        }

        recordRevision(message, actor, TicketMessageRevision.Action.EDIT, message.getBody(), trimmedBody);
        message.setBody(trimmedBody);
        message.setEditedAt(Instant.now());
        ticketMessageRepository.save(message);
        ticketMentionService.syncMentions(ticket, message, trimmedBody, actor);
        ticketRepository.save(ticket);
        return toDetailDto(ticket, actor, asStaff);
    }

    @Transactional
    public TicketDetailDto deleteMessage(User actor, Long ticketId, Long messageId, boolean asStaff) {
        Ticket ticket = asStaff ? requireStaffTicket(ticketId, false) : requireOwnedTicket(actor, ticketId);
        TicketMessage message = requireEditableMessage(actor, ticket, messageId, asStaff);

        recordRevision(message, actor, TicketMessageRevision.Action.DELETE, message.getBody(), null);
        message.getAttachments().clear();
        message.setDeletedAt(Instant.now());
        ticketMessageRepository.save(message);
        ticketRepository.save(ticket);
        return toDetailDto(ticket, actor, asStaff);
    }

    @Transactional(readOnly = true)
    public List<TicketMessageRevisionDto> listMessageRevisions(User agent, Long ticketId, Long messageId) {
        requireAgent(agent);
        requireStaffTicket(ticketId, true);
        TicketMessage message = ticketMessageRepository.findByIdAndTicketId(messageId, ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_MESSAGE_NOT_FOUND));
        return ticketMessageRevisionRepository.findByMessageIdOrderByCreatedAtAscIdAsc(message.getId())
                .stream()
                .map(this::toRevisionDto)
                .toList();
    }

    @Transactional
    public TicketDetailDto editDescription(User actor, Long ticketId, String body, boolean asStaff) {
        Ticket ticket = asStaff ? requireStaffTicket(ticketId, false) : requireOwnedTicket(actor, ticketId);
        if (!canEditInitialDescription(ticket, actor)) {
            if (ticket.getRequester() == null
                    || actor.getId() == null
                    || !actor.getId().equals(ticket.getRequester().getId())) {
                throw new ApiException(ErrorCode.TICKET_MESSAGE_NOT_OWNED);
            }
            throw new ApiException(ErrorCode.TICKET_MESSAGE_NOT_EDITABLE);
        }

        String trimmedBody = body == null ? "" : body.trim();
        if (!StringUtils.hasText(trimmedBody)) {
            throw new ApiException(ErrorCode.TICKET_REPLY_BODY_REQUIRED);
        }
        if (trimmedBody.length() > DESCRIPTION_MAX) {
            throw new ApiException(ErrorCode.TICKET_REPLY_BODY_TOO_LONG);
        }
        if (trimmedBody.equals(ticket.getDescription())) {
            return toDetailDto(ticket, actor, asStaff);
        }

        recordDescriptionRevision(ticket, actor, ticket.getDescription(), trimmedBody);
        ticket.setDescription(trimmedBody);
        ticketRepository.save(ticket);
        return toDetailDto(ticket, actor, asStaff);
    }

    @Transactional(readOnly = true)
    public List<TicketMessageRevisionDto> listDescriptionRevisions(User agent, Long ticketId) {
        requireAgent(agent);
        requireStaffTicket(ticketId, true);
        return ticketMessageRevisionRepository.findByTicketIdAndMessageIsNullOrderByCreatedAtAscIdAsc(ticketId)
                .stream()
                .map(this::toRevisionDto)
                .toList();
    }

    @Transactional
    public TicketDetailDto assignAsStaff(User agent, Long ticketId, Long assigneeId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        assertNotDeleted(ticket);

        User previousAssignee = ticket.getAssignee();
        Long previousId = previousAssignee != null ? previousAssignee.getId() : null;

        User nextAssignee = null;
        if (assigneeId != null) {
            nextAssignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ASSIGNEE_NOT_FOUND));
            if (nextAssignee.getRole() != User.Role.ADMIN || !nextAssignee.isEnabled()) {
                throw new ApiException(ErrorCode.TICKET_ASSIGNEE_INVALID);
            }
        }

        Long nextId = nextAssignee != null ? nextAssignee.getId() : null;
        if ((previousId == null && nextId == null)
                || (previousId != null && previousId.equals(nextId))) {
            return toDetailDto(ticket, agent, true);
        }

        ticket.setAssignee(nextAssignee);
        ticketRepository.save(ticket);

        if (previousAssignee != null
                && previousAssignee.getId() != null
                && agent.getId() != null
                && !previousAssignee.getId().equals(agent.getId())
                && (nextAssignee == null || nextAssignee.getId() == null
                    || !previousAssignee.getId().equals(nextAssignee.getId()))) {
            notificationService.onUnassigned(ticket, previousAssignee, agent);
        }
        if (nextAssignee != null) {
            notificationService.onAssigned(ticket, nextAssignee, agent);
        }

        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto updateQueueAsStaff(User agent, Long ticketId, Long queueId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        assertNotDeleted(ticket);

        Long previousId = ticket.getQueue() != null ? ticket.getQueue().getId() : null;
        TicketQueue nextQueue = queueId == null ? null : ticketQueueService.requireActiveQueue(queueId);
        Long nextId = nextQueue != null ? nextQueue.getId() : null;
        if ((previousId == null && nextId == null)
                || (previousId != null && previousId.equals(nextId))) {
            return toDetailDto(ticket, agent, true);
        }

        ticket.setQueue(nextQueue);
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto transferAsStaff(User agent, Long ticketId, TransferTicketRequest request) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        assertNotDeleted(ticket);
        if (request == null || (!request.isAssigneeChanged() && !request.isQueueChanged())) {
            throw new ApiException(ErrorCode.TICKET_TRANSFER_INVALID);
        }

        String note = request.getNote() == null ? null : request.getNote().trim();
        if (StringUtils.hasText(note) && note.length() > 2000) {
            throw new ApiException(ErrorCode.TICKET_TRANSFER_NOTE_TOO_LONG);
        }

        User fromAssignee = ticket.getAssignee();
        TicketQueue fromQueue = ticket.getQueue();
        User toAssignee = fromAssignee;
        TicketQueue toQueue = fromQueue;
        boolean assigneeChanged = false;
        boolean queueChanged = false;

        if (request.isAssigneeChanged()) {
            if (request.getAssigneeId() != null) {
                toAssignee = userRepository.findById(request.getAssigneeId())
                        .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ASSIGNEE_NOT_FOUND));
                if (toAssignee.getRole() != User.Role.ADMIN || !toAssignee.isEnabled()) {
                    throw new ApiException(ErrorCode.TICKET_ASSIGNEE_INVALID);
                }
            } else {
                toAssignee = null;
            }
            Long fromId = fromAssignee != null ? fromAssignee.getId() : null;
            Long toId = toAssignee != null ? toAssignee.getId() : null;
            assigneeChanged = (fromId == null) != (toId == null)
                    || (fromId != null && !fromId.equals(toId));
        }

        if (request.isQueueChanged()) {
            toQueue = request.getQueueId() == null
                    ? null
                    : ticketQueueService.requireActiveQueue(request.getQueueId());
            Long fromQueueId = fromQueue != null ? fromQueue.getId() : null;
            Long toQueueId = toQueue != null ? toQueue.getId() : null;
            queueChanged = (fromQueueId == null) != (toQueueId == null)
                    || (fromQueueId != null && !fromQueueId.equals(toQueueId));
        }

        if (!assigneeChanged && !queueChanged) {
            throw new ApiException(ErrorCode.TICKET_TRANSFER_NO_CHANGE);
        }

        if (assigneeChanged) {
            ticket.setAssignee(toAssignee);
        }
        if (queueChanged) {
            ticket.setQueue(toQueue);
        }
        ticketRepository.save(ticket);

        TicketTransfer transfer = new TicketTransfer();
        transfer.setTicket(ticket);
        transfer.setTransferredBy(agent);
        transfer.setFromAssignee(fromAssignee);
        transfer.setToAssignee(assigneeChanged ? toAssignee : fromAssignee);
        transfer.setFromQueue(fromQueue);
        transfer.setToQueue(queueChanged ? toQueue : fromQueue);
        transfer.setNote(StringUtils.hasText(note) ? note : null);
        ticketTransferRepository.save(transfer);

        if (StringUtils.hasText(note)) {
            TicketMessage message = new TicketMessage();
            message.setTicket(ticket);
            message.setAuthor(agent);
            message.setBody(note);
            message.setInternalNote(true);
            ticketMessageRepository.save(message);
        }

        if (assigneeChanged) {
            notificationService.onTransferred(ticket, agent, fromAssignee, toAssignee);
        } else {
            notificationService.onTransferred(ticket, agent, null, null);
        }

        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto escalateAsStaff(User agent, Long ticketId, EscalateTicketRequest request) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        assertNotDeleted(ticket);
        if (ticket.isArchived() || ticket.isMerged()) {
            throw new ApiException(ErrorCode.TICKET_ESCALATION_INVALID);
        }
        applyEscalation(ticket, agent, request, TicketEscalationTrigger.MANUAL);
        return toDetailDto(ticket, agent, true);
    }

    private void applyEscalation(
            Ticket ticket,
            User actor,
            EscalateTicketRequest request,
            TicketEscalationTrigger triggerType) {
        if (request == null) {
            throw new ApiException(ErrorCode.TICKET_ESCALATION_INVALID);
        }

        String note = request.getNote() == null ? null : request.getNote().trim();
        if (StringUtils.hasText(note) && note.length() > 2000) {
            throw new ApiException(ErrorCode.TICKET_ESCALATION_NOTE_TOO_LONG);
        }

        TicketPriority fromPriority = ticket.getPriority();
        User fromAssignee = ticket.getAssignee();
        TicketQueue fromQueue = ticket.getQueue();

        TicketPriority toPriority = fromPriority;
        User toAssignee = fromAssignee;
        TicketQueue toQueue = fromQueue;
        boolean priorityChanged = false;
        boolean assigneeChanged = false;
        boolean queueChanged = false;

        if (request.isPriorityChanged()) {
            if (request.getPriority() == null) {
                throw new ApiException(ErrorCode.TICKET_ESCALATION_INVALID);
            }
            toPriority = request.getPriority();
            priorityChanged = toPriority != fromPriority;
        } else if (request.isBumpPriority()) {
            toPriority = nextHigherPriority(fromPriority);
            priorityChanged = toPriority != fromPriority;
        }

        if (request.isAssigneeChanged()) {
            if (request.getAssigneeId() != null) {
                toAssignee = userRepository.findById(request.getAssigneeId())
                        .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ASSIGNEE_NOT_FOUND));
                if (toAssignee.getRole() != User.Role.ADMIN || !toAssignee.isEnabled()) {
                    throw new ApiException(ErrorCode.TICKET_ASSIGNEE_INVALID);
                }
            } else {
                toAssignee = null;
            }
            Long fromId = fromAssignee != null ? fromAssignee.getId() : null;
            Long toId = toAssignee != null ? toAssignee.getId() : null;
            assigneeChanged = (fromId == null) != (toId == null)
                    || (fromId != null && !fromId.equals(toId));
        }

        if (request.isQueueChanged()) {
            toQueue = request.getQueueId() == null
                    ? null
                    : ticketQueueService.requireActiveQueue(request.getQueueId());
            Long fromQueueId = fromQueue != null ? fromQueue.getId() : null;
            Long toQueueId = toQueue != null ? toQueue.getId() : null;
            queueChanged = (fromQueueId == null) != (toQueueId == null)
                    || (fromQueueId != null && !fromQueueId.equals(toQueueId));
        }

        // SLA auto-escalate always records even if already URGENT (priority unchanged).
        boolean allowNoFieldChange = triggerType == TicketEscalationTrigger.SLA_BREACH;
        if (!priorityChanged && !assigneeChanged && !queueChanged && !allowNoFieldChange) {
            if (request.isBumpPriority() || request.isPriorityChanged()
                    || request.isAssigneeChanged() || request.isQueueChanged()) {
                throw new ApiException(ErrorCode.TICKET_ESCALATION_NO_CHANGE);
            }
            throw new ApiException(ErrorCode.TICKET_ESCALATION_INVALID);
        }

        if (priorityChanged) {
            ticket.setPriority(toPriority);
            if (triggerType == TicketEscalationTrigger.MANUAL) {
                Instant base = ticket.getCreatedAt() != null ? ticket.getCreatedAt() : Instant.now();
                ticket.setDueAt(ticketSettingsService.computeDueAt(toPriority, base));
            }
        }
        if (assigneeChanged) {
            ticket.setAssignee(toAssignee);
        }
        if (queueChanged) {
            ticket.setQueue(toQueue);
        }
        ticket.setEscalatedAt(Instant.now());
        ticketRepository.save(ticket);

        TicketEscalation escalation = new TicketEscalation();
        escalation.setTicket(ticket);
        escalation.setEscalatedBy(actor);
        escalation.setTriggerType(triggerType);
        escalation.setFromPriority(fromPriority);
        escalation.setToPriority(toPriority);
        escalation.setFromAssignee(fromAssignee);
        escalation.setToAssignee(toAssignee);
        escalation.setFromQueue(fromQueue);
        escalation.setToQueue(toQueue);
        escalation.setNote(StringUtils.hasText(note) ? note : null);
        ticketEscalationRepository.save(escalation);

        if (StringUtils.hasText(note) && actor != null) {
            TicketMessage message = new TicketMessage();
            message.setTicket(ticket);
            message.setAuthor(actor);
            message.setBody(note);
            message.setInternalNote(true);
            ticketMessageRepository.save(message);
        }

        if (assigneeChanged && fromAssignee != null && !isSameUser(fromAssignee, toAssignee)
                && actor != null && !isSameUser(fromAssignee, actor)) {
            notificationService.onUnassigned(ticket, fromAssignee, actor);
        }
        notificationService.onEscalated(ticket, actor);
    }

    private TicketPriority nextHigherPriority(TicketPriority current) {
        if (current == null) {
            return TicketPriority.HIGH;
        }
        return switch (current) {
            case LOW -> TicketPriority.MEDIUM;
            case MEDIUM -> TicketPriority.HIGH;
            case HIGH, URGENT -> TicketPriority.URGENT;
        };
    }

    private boolean isSameUser(User a, User b) {
        if (a == null || b == null || a.getId() == null || b.getId() == null) {
            return false;
        }
        return a.getId().equals(b.getId());
    }

    @Transactional
    public TicketDetailDto updateStatusAsStaff(User agent, Long ticketId, TicketStatus nextStatus) {
        requireAgent(agent);
        if (nextStatus == null) {
            throw new ApiException(ErrorCode.TICKET_STATUS_INVALID);
        }
        Ticket ticket = requireStaffTicket(ticketId, false);
        TicketStatus previous = ticket.getStatus();
        if (ticket.getStatus() == TicketStatus.CLOSED && nextStatus != TicketStatus.CLOSED) {
            assertReopenAllowed(ticket);
        }
        ticketStatusWorkflowService.assertTransitionAllowed(ticket.getStatus(), nextStatus);
        applyStatus(ticket, nextStatus);
        if (nextStatus != TicketStatus.CLOSED) {
            ticket.setArchivedAt(null);
        }
        ticketRepository.save(ticket);
        notificationService.onStatusChanged(ticket, agent, previous, nextStatus, true);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto closeAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        return closeTicket(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto closeMine(User requester, Long ticketId) {
        Ticket ticket = requireOwnedTicket(requester, ticketId);
        return closeTicket(ticket, requester, false);
    }

    @Transactional
    public TicketDetailDto reopenAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        return reopenTicket(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto reopenMine(User requester, Long ticketId) {
        Ticket ticket = requireOwnedTicket(requester, ticketId);
        return reopenTicket(ticket, requester, false);
    }

    @Transactional
    public TicketDetailDto updateTagsAsStaff(User agent, Long ticketId, UpdateTicketTagsRequest request) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        Set<TicketTag> tags = ticketTagService.resolveTags(
                request != null ? request.getTagIds() : null,
                request != null ? request.getNames() : null
        );
        ticket.setTags(new HashSet<>(tags));
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto archiveAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        if (ticket.isArchived()) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_ARCHIVED);
        }
        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new ApiException(ErrorCode.TICKET_ARCHIVE_REQUIRES_CLOSED);
        }
        ticket.setArchivedAt(Instant.now());
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto unarchiveAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        if (!ticket.isArchived()) {
            throw new ApiException(ErrorCode.TICKET_NOT_ARCHIVED);
        }
        ticket.setArchivedAt(null);
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto softDeleteAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        if (ticket.isDeleted()) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_DELETED);
        }
        ticket.setDeletedAt(Instant.now());
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto restoreAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, true);
        if (!ticket.isDeleted()) {
            throw new ApiException(ErrorCode.TICKET_NOT_DELETED);
        }
        ticket.setDeletedAt(null);
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto mergeAsStaff(User agent, Long targetTicketId, MergeTicketRequest request) {
        requireAgent(agent);
        if (request == null || !request.isSourceProvided()) {
            throw new ApiException(ErrorCode.TICKET_MERGE_SOURCE_REQUIRED);
        }

        Ticket target = requireStaffTicket(targetTicketId, false);
        if (target.isMerged()) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_MERGED);
        }
        if (target.isArchived()) {
            throw new ApiException(ErrorCode.TICKET_MERGE_INVALID);
        }

        Ticket source = resolveMergeSource(request);
        assertNotDeleted(source);
        if (source.getId().equals(target.getId())) {
            throw new ApiException(ErrorCode.TICKET_MERGE_SAME);
        }
        if (source.isMerged()) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_MERGED);
        }
        if (source.isArchived()) {
            throw new ApiException(ErrorCode.TICKET_MERGE_INVALID);
        }

        TicketMessage notice = new TicketMessage();
        notice.setTicket(target);
        notice.setAuthor(agent);
        notice.setBody("Merged ticket " + source.getPublicNumber()
                + " (\"" + source.getSubject() + "\") into this ticket.");
        notice.setInternalNote(false);
        ticketMessageRepository.save(notice);

        TicketMessage imported = new TicketMessage();
        imported.setTicket(target);
        imported.setAuthor(source.getRequester() != null ? source.getRequester() : agent);
        imported.setBody("[From " + source.getPublicNumber() + "]\n\n" + source.getDescription());
        imported.setInternalNote(false);
        imported.setCreatedAt(source.getCreatedAt() != null ? source.getCreatedAt() : Instant.now());

        List<TicketAttachment> sourceAttachments = new ArrayList<>(source.getAttachments());
        for (TicketAttachment attachment : sourceAttachments) {
            TicketMessageAttachment messageAttachment = new TicketMessageAttachment();
            messageAttachment.setMessage(imported);
            messageAttachment.setFileName(attachment.getFileName());
            messageAttachment.setContentType(attachment.getContentType());
            messageAttachment.setSizeBytes(attachment.getSizeBytes());
            messageAttachment.setData(attachment.getData());
            imported.getAttachments().add(messageAttachment);
        }
        source.getAttachments().clear();
        ticketAttachmentRepository.deleteAll(sourceAttachments);
        ticketMessageRepository.save(imported);

        for (TicketMessage message : ticketMessageRepository.findByTicketOrderByCreatedAtAscIdAsc(source)) {
            message.setTicket(target);
            ticketMessageRepository.save(message);
        }

        for (TicketMention mention : ticketMentionRepository.findByTicketId(source.getId())) {
            mention.setTicket(target);
            ticketMentionRepository.save(mention);
        }

        if (source.getTags() != null && !source.getTags().isEmpty()) {
            target.getTags().addAll(source.getTags());
            source.getTags().clear();
        }

        if (source.getStatus() != TicketStatus.CLOSED) {
            applyStatus(source, TicketStatus.CLOSED);
        }
        source.setMergedInto(target);
        source.setArchivedAt(Instant.now());
        ticketRepository.save(source);
        ticketRepository.save(target);

        return toDetailDto(target, agent, true);
    }

    @Transactional
    public SplitTicketResultDto splitAsStaff(User agent, Long sourceTicketId, SplitTicketRequest request) {
        requireAgent(agent);
        if (request == null) {
            throw new ApiException(ErrorCode.TICKET_SPLIT_INVALID);
        }

        String subject = request.getSubject() == null ? "" : request.getSubject().trim();
        if (!StringUtils.hasText(subject)) {
            throw new ApiException(ErrorCode.TICKET_SPLIT_SUBJECT_REQUIRED);
        }
        if (subject.length() > SUBJECT_MAX) {
            throw new ApiException(ErrorCode.TICKET_SUBJECT_TOO_LONG);
        }

        List<Long> messageIds = request.getMessageIds() == null
                ? List.of()
                : request.getMessageIds().stream().filter(id -> id != null).distinct().toList();
        if (messageIds.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_SPLIT_MESSAGES_REQUIRED);
        }

        Ticket source = requireStaffTicket(sourceTicketId, false);
        if (source.isMerged()) {
            throw new ApiException(ErrorCode.TICKET_SPLIT_INVALID);
        }
        if (source.isArchived()) {
            throw new ApiException(ErrorCode.TICKET_SPLIT_INVALID);
        }

        List<TicketMessage> toMove = ticketMessageRepository
                .findByTicketIdAndIdInOrderByCreatedAtAscIdAsc(source.getId(), messageIds);
        if (toMove.size() != messageIds.size()) {
            throw new ApiException(ErrorCode.TICKET_SPLIT_MESSAGE_NOT_FOUND);
        }

        List<TicketMessage> allReplies = ticketMessageRepository
                .findByTicketOrderByCreatedAtAscIdAsc(source);
        if (toMove.size() >= allReplies.size()) {
            throw new ApiException(ErrorCode.TICKET_SPLIT_INVALID);
        }

        String description = toMove.get(0).getBody();
        if (description != null && description.length() > DESCRIPTION_MAX) {
            description = description.substring(0, DESCRIPTION_MAX);
        }
        if (!StringUtils.hasText(description)) {
            description = "Split from ticket " + source.getPublicNumber();
        }

        Ticket child = new Ticket();
        child.setSubject(subject);
        child.setDescription(description);
        child.setCategory(source.getCategory());
        child.setQueue(source.getQueue());
        child.setPriority(source.getPriority());
        child.setStatus(TicketStatus.NEW);
        child.setChannel(source.getChannel());
        child.setRequester(source.getRequester());
        child.setAssignee(source.getAssignee());
        child.setSplitFrom(source);
        child.setPublicNumber("TMP-" + System.nanoTime());
        child.setDueAt(ticketSettingsService.computeDueAt(source.getPriority(), Instant.now()));

        Ticket savedChild = ticketRepository.saveAndFlush(child);
        savedChild.setPublicNumber(buildPublicNumber(savedChild.getId()));
        savedChild = ticketRepository.save(savedChild);

        for (TicketMessage message : toMove) {
            message.setTicket(savedChild);
            ticketMessageRepository.save(message);
        }

        for (TicketMention mention : ticketMentionRepository.findByTicketId(source.getId())) {
            if (mention.getMessage() != null && messageIds.contains(mention.getMessage().getId())) {
                mention.setTicket(savedChild);
                ticketMentionRepository.save(mention);
            }
        }

        TicketMessage sourceNotice = new TicketMessage();
        sourceNotice.setTicket(source);
        sourceNotice.setAuthor(agent);
        sourceNotice.setBody("Split " + toMove.size() + " message(s) into ticket "
                + savedChild.getPublicNumber() + " (\"" + savedChild.getSubject() + "\").");
        sourceNotice.setInternalNote(false);
        ticketMessageRepository.save(sourceNotice);

        TicketMessage childNotice = new TicketMessage();
        childNotice.setTicket(savedChild);
        childNotice.setAuthor(agent);
        childNotice.setBody("Split from ticket " + source.getPublicNumber()
                + " (\"" + source.getSubject() + "\").");
        childNotice.setInternalNote(false);
        ticketMessageRepository.save(childNotice);

        ticketRepository.save(source);

        return new SplitTicketResultDto(
                toDetailDto(source, agent, true),
                toDto(savedChild));
    }

    @Transactional
    public TicketDetailDto linkRelatedAsStaff(User agent, Long ticketId, LinkTicketsRequest request) {
        requireAgent(agent);
        if (request == null || request.getRelatedTicketIds() == null || request.getRelatedTicketIds().isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_LINK_TARGET_REQUIRED);
        }

        Ticket ticket = requireStaffTicket(ticketId, false);
        if (ticket.isMerged()) {
            throw new ApiException(ErrorCode.TICKET_LINK_INVALID);
        }

        List<Long> relatedIds = request.getRelatedTicketIds().stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (relatedIds.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_LINK_TARGET_REQUIRED);
        }

        for (Long relatedId : relatedIds) {
            if (relatedId.equals(ticketId)) {
                throw new ApiException(ErrorCode.TICKET_LINK_SAME);
            }
            Ticket related = ticketRepository.findById(relatedId)
                    .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
            assertNotDeleted(related);
            if (related.isMerged()) {
                throw new ApiException(ErrorCode.TICKET_LINK_INVALID);
            }
            if (ticketRelatedLinkRepository.existsByTicketIdAndRelatedTicketId(ticketId, relatedId)) {
                throw new ApiException(ErrorCode.TICKET_LINK_ALREADY_EXISTS);
            }

            TicketRelatedLink forward = new TicketRelatedLink();
            forward.setTicket(ticket);
            forward.setRelatedTicket(related);
            ticketRelatedLinkRepository.save(forward);

            TicketRelatedLink reverse = new TicketRelatedLink();
            reverse.setTicket(related);
            reverse.setRelatedTicket(ticket);
            ticketRelatedLinkRepository.save(reverse);
        }

        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto unlinkRelatedAsStaff(User agent, Long ticketId, Long relatedTicketId) {
        requireAgent(agent);
        if (relatedTicketId == null) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }
        if (relatedTicketId.equals(ticketId)) {
            throw new ApiException(ErrorCode.TICKET_LINK_SAME);
        }

        Ticket ticket = requireStaffTicket(ticketId, false);
        if (!ticketRelatedLinkRepository.existsByTicketIdAndRelatedTicketId(ticketId, relatedTicketId)) {
            throw new ApiException(ErrorCode.TICKET_LINK_INVALID);
        }

        ticketRelatedLinkRepository.deleteBidirectional(ticketId, relatedTicketId);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto watchAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        addWatcher(ticket, agent, agent, false);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto unwatchAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        ticketWatcherRepository.deleteByTicketIdAndUserId(ticket.getId(), agent.getId());
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto addWatcherAsStaff(User agent, Long ticketId, Long userId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        if (userId == null) {
            throw new ApiException(ErrorCode.TICKET_WATCHER_NOT_FOUND);
        }
        User watcher = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_WATCHER_NOT_FOUND));
        addWatcher(ticket, watcher, agent, true);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto removeWatcherAsStaff(User agent, Long ticketId, Long userId) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        if (userId == null || !ticketWatcherRepository.existsByTicketIdAndUserId(ticket.getId(), userId)) {
            throw new ApiException(ErrorCode.TICKET_WATCHER_NOT_FOUND);
        }
        ticketWatcherRepository.deleteByTicketIdAndUserId(ticket.getId(), userId);
        return toDetailDto(ticket, agent, true);
    }

    private void addWatcher(Ticket ticket, User watcher, User actor, boolean notifyWatcher) {
        if (ticket.isDeleted()) {
            throw new ApiException(ErrorCode.TICKET_DELETED);
        }
        requireValidWatcher(watcher);
        if (ticketWatcherRepository.existsByTicketIdAndUserId(ticket.getId(), watcher.getId())) {
            throw new ApiException(ErrorCode.TICKET_WATCHER_ALREADY);
        }
        TicketWatcher link = new TicketWatcher();
        link.setTicket(ticket);
        link.setUser(watcher);
        ticketWatcherRepository.save(link);
        if (notifyWatcher) {
            notificationService.onWatcherAdded(ticket, watcher, actor);
        }
    }

    private void requireValidWatcher(User watcher) {
        if (watcher == null || watcher.getId() == null) {
            throw new ApiException(ErrorCode.TICKET_WATCHER_NOT_FOUND);
        }
        if (watcher.getRole() != User.Role.ADMIN || !watcher.isEnabled()) {
            throw new ApiException(ErrorCode.TICKET_WATCHER_INVALID);
        }
    }

    @Transactional
    public TicketDetailDto updateDueDateAsStaff(User agent, Long ticketId, UpdateTicketDueDateRequest request) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId, false);
        if (ticket.isMerged()) {
            throw new ApiException(ErrorCode.TICKET_DUE_DATE_INVALID);
        }

        Instant nextDueAt;
        if (request != null && Boolean.TRUE.equals(request.getRecalculateFromPriority())) {
            Instant base = ticket.getCreatedAt() != null ? ticket.getCreatedAt() : Instant.now();
            nextDueAt = ticketSettingsService.computeDueAt(ticket.getPriority(), base);
        } else if (request != null) {
            nextDueAt = request.getDueAt();
        } else {
            throw new ApiException(ErrorCode.TICKET_DUE_DATE_INVALID);
        }

        ticket.setDueAt(nextDueAt);
        if (nextDueAt == null || nextDueAt.isAfter(Instant.now())) {
            ticket.setEscalatedAt(null);
        }
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    private Ticket resolveMergeSource(MergeTicketRequest request) {
        if (request.getSourceTicketId() != null) {
            return ticketRepository.findById(request.getSourceTicketId())
                    .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        }
        String publicNumber = request.getSourcePublicNumber() != null
                ? request.getSourcePublicNumber().trim()
                : "";
        if (!StringUtils.hasText(publicNumber)) {
            throw new ApiException(ErrorCode.TICKET_MERGE_SOURCE_REQUIRED);
        }
        return ticketRepository.findByPublicNumberIgnoreCase(publicNumber)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
    }

    @Transactional
    public int autoArchiveClosedTickets() {
        int days = ticketSettingsService.getAutoArchiveClosedAfterDays();
        if (days <= 0) {
            return 0;
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        List<Ticket> eligible = ticketRepository.findClosedEligibleForAutoArchive(cutoff);
        if (eligible.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now();
        for (Ticket ticket : eligible) {
            ticket.setArchivedAt(now);
        }
        ticketRepository.saveAll(eligible);
        return eligible.size();
    }

    @Transactional
    public int autoEscalateOverdueTickets() {
        Instant now = Instant.now();
        List<Ticket> eligible = ticketRepository.findEligibleForSlaEscalation(now);
        if (eligible.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Ticket candidate : eligible) {
            if (candidate.getId() == null) {
                continue;
            }
            // Lock so inbox load + scheduler cannot double-insert escalation history.
            Ticket ticket = ticketRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (ticket == null || ticket.isEscalated()) {
                continue;
            }
            if (ticket.getDeletedAt() != null || ticket.getArchivedAt() != null) {
                continue;
            }
            if (ticket.getDueAt() == null || !ticket.getDueAt().isBefore(now)) {
                continue;
            }
            TicketStatus status = ticket.getStatus();
            if (status != TicketStatus.NEW && status != TicketStatus.OPEN
                    && status != TicketStatus.PENDING && status != TicketStatus.ON_HOLD) {
                continue;
            }
            EscalateTicketRequest request = new EscalateTicketRequest();
            request.setBumpPriority(true);
            request.setNote(null);
            applyEscalation(ticket, null, request, TicketEscalationTrigger.SLA_BREACH);
            count++;
        }
        return count;
    }

    private TicketDetailDto closeTicket(Ticket ticket, User viewer, boolean asStaff) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_CLOSED);
        }
        applyStatus(ticket, TicketStatus.CLOSED);
        ticketRepository.save(ticket);
        notificationService.onClosed(ticket, viewer, asStaff);
        return toDetailDto(ticket, viewer, asStaff);
    }

    private TicketDetailDto reopenTicket(Ticket ticket, User viewer, boolean asStaff) {
        assertNotDeleted(ticket);
        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new ApiException(ErrorCode.TICKET_NOT_CLOSED);
        }
        assertReopenAllowed(ticket);
        applyStatus(ticket, TicketStatus.OPEN);
        ticket.setArchivedAt(null);
        ticketRepository.save(ticket);
        notificationService.onReopened(ticket, viewer, asStaff);
        return toDetailDto(ticket, viewer, asStaff);
    }

    private void maybeAutoTransition(Ticket ticket, TicketStatus nextStatus) {
        if (ticket.getStatus() == nextStatus) {
            return;
        }
        if (ticketStatusWorkflowService.isTransitionAllowed(ticket.getStatus(), nextStatus)) {
            applyStatus(ticket, nextStatus);
        }
    }

    private void applyStatus(Ticket ticket, TicketStatus nextStatus) {
        TicketStatus previous = ticket.getStatus();
        if (previous == nextStatus) {
            return;
        }
        ticket.setStatus(nextStatus);
        if (nextStatus == TicketStatus.CLOSED) {
            if (ticket.getClosedAt() == null) {
                ticket.setClosedAt(Instant.now());
            }
        } else if (previous == TicketStatus.CLOSED) {
            ticket.setClosedAt(null);
        }
    }

    private void assertReopenAllowed(Ticket ticket) {
        Instant deadline = reopenDeadline(ticket);
        if (deadline == null || Instant.now().isAfter(deadline)) {
            throw new ApiException(ErrorCode.TICKET_REOPEN_WINDOW_EXPIRED);
        }
    }

    private Instant reopenDeadline(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.CLOSED) {
            return null;
        }
        Instant closedAt = ticket.getClosedAt() != null ? ticket.getClosedAt() : ticket.getUpdatedAt();
        if (closedAt == null) {
            return null;
        }
        return closedAt.plus(Duration.ofDays(ticketSettingsService.getReopenWindowDays()));
    }

    private boolean canReopen(Ticket ticket) {
        if (ticket.isDeleted() || ticket.isArchived()) {
            return false;
        }
        Instant deadline = reopenDeadline(ticket);
        return deadline != null && !Instant.now().isAfter(deadline);
    }

    private void requireAgent(User agent) {
        if (agent == null || agent.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadTicketAttachmentForStaff(User agent, Long ticketId, Long attachmentId) {
        requireAgent(agent);
        if (!ticketRepository.existsById(ticketId)) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }
        TicketAttachment attachment = ticketAttachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND));
        return toDownloadResponse(attachment.getFileName(), attachment.getContentType(), attachment.getData());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadMessageAttachmentForStaff(
            User agent, Long ticketId, Long messageId, Long attachmentId) {
        requireAgent(agent);
        if (!ticketRepository.existsById(ticketId)) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }
        TicketMessage message = ticketMessageRepository.findByIdAndTicketId(messageId, ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        if (message.isDeleted()) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
        }
        TicketMessageAttachment attachment = ticketMessageAttachmentRepository
                .findByIdAndMessageId(attachmentId, messageId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND));
        return toDownloadResponse(attachment.getFileName(), attachment.getContentType(), attachment.getData());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadTicketAttachment(User requester, Long ticketId, Long attachmentId) {
        requireOwnedTicket(requester, ticketId);
        TicketAttachment attachment = ticketAttachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND));
        return toDownloadResponse(attachment.getFileName(), attachment.getContentType(), attachment.getData());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadMessageAttachment(
            User requester, Long ticketId, Long messageId, Long attachmentId) {
        requireOwnedTicket(requester, ticketId);
        TicketMessage message = ticketMessageRepository.findByIdAndTicketId(messageId, ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        if (message.isInternalNote()) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
        }
        if (message.isDeleted()) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND);
        }
        TicketMessageAttachment attachment = ticketMessageAttachmentRepository
                .findByIdAndMessageId(attachmentId, messageId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ATTACHMENT_NOT_FOUND));
        return toDownloadResponse(attachment.getFileName(), attachment.getContentType(), attachment.getData());
    }

    private ResponseEntity<Resource> toDownloadResponse(String fileName, String contentType, byte[] data) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(mediaType)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    private Ticket requireOwnedTicket(User requester, Long ticketId) {
        if (requester == null || requester.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (ticketId == null) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }
        Ticket ticket = ticketRepository.findByIdAndRequesterId(ticketId, requester.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        assertNotDeleted(ticket);
        return ticket;
    }

    private Ticket requireStaffTicket(Long ticketId, boolean allowDeleted) {
        if (ticketId == null) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        if (!allowDeleted) {
            assertNotDeleted(ticket);
        }
        return ticket;
    }

    private void assertNotDeleted(Ticket ticket) {
        if (ticket != null && ticket.isDeleted()) {
            throw new ApiException(ErrorCode.TICKET_DELETED);
        }
    }

    private Specification<Ticket> buildMineSpec(Long requesterId, TicketStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("requester").get("id"), requesterId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("publicNumber")), pattern),
                        cb.like(cb.lower(root.get("subject")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? 10 : Math.min(pageable.getPageSize(), 100);

        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                continue;
            }
            Sort.Order next = new Sort.Order(order.getDirection(), order.getProperty());
            if ("subject".equals(order.getProperty()) || "publicNumber".equals(order.getProperty())) {
                next = next.ignoreCase();
            }
            orders.add(next);
        }
        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("createdAt"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    @Transactional
    public TicketDto create(
            User requester,
            String subject,
            String description,
            Long categoryId,
            TicketPriority priority,
            MultipartFile[] attachments) {
        if (requester == null || requester.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        String trimmedSubject = subject == null ? "" : subject.trim();
        String trimmedDescription = description == null ? "" : description.trim();

        if (!StringUtils.hasText(trimmedSubject)) {
            throw new ApiException(ErrorCode.TICKET_SUBJECT_REQUIRED);
        }
        if (trimmedSubject.length() > SUBJECT_MAX) {
            throw new ApiException(ErrorCode.TICKET_SUBJECT_TOO_LONG);
        }
        if (!StringUtils.hasText(trimmedDescription)) {
            throw new ApiException(ErrorCode.TICKET_DESCRIPTION_REQUIRED);
        }
        if (trimmedDescription.length() > DESCRIPTION_MAX) {
            throw new ApiException(ErrorCode.TICKET_DESCRIPTION_TOO_LONG);
        }
        if (priority == null) {
            throw new ApiException(ErrorCode.TICKET_PRIORITY_REQUIRED);
        }

        TicketCategory category = ticketCategoryService.requireActiveCategory(categoryId);

        List<MultipartFile> files = normalizeFiles(attachments);
        ticketSettingsService.validateAttachmentBatch(files);

        Ticket ticket = new Ticket();
        ticket.setSubject(trimmedSubject);
        ticket.setDescription(trimmedDescription);
        ticket.setCategory(category);
        Long defaultQueueId = ticketSettingsService.getOrCreate().getDefaultQueueId();
        if (defaultQueueId != null) {
            try {
                ticket.setQueue(ticketQueueService.requireActiveQueue(defaultQueueId));
            } catch (ApiException ignored) {
                // Invalid/inactive default queue — leave unset.
            }
        }
        ticket.setPriority(priority);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setChannel(TicketChannel.PORTAL);
        ticket.setRequester(requester);
        ticket.setPublicNumber("TMP-" + System.nanoTime());
        ticket.setDueAt(ticketSettingsService.computeDueAt(priority, Instant.now()));

        for (MultipartFile file : files) {
            ticket.addAttachment(toTicketAttachment(file));
        }

        Ticket saved = ticketRepository.saveAndFlush(ticket);
        saved.setPublicNumber(buildPublicNumber(saved.getId()));
        User autoAssignee = ticketAutoAssignService.assignIfConfigured(saved);
        saved = ticketRepository.save(saved);
        if (autoAssignee != null) {
            notificationService.onAssigned(saved, autoAssignee, requester);
        } else {
            notificationService.onTicketCreated(saved, requester);
        }
        return toDto(saved);
    }

    private List<MultipartFile> normalizeFiles(MultipartFile[] attachments) {
        List<MultipartFile> files = new ArrayList<>();
        if (attachments == null) {
            return files;
        }
        for (MultipartFile file : attachments) {
            if (file != null && !file.isEmpty()) {
                files.add(file);
            }
        }
        return files;
    }

    private void validateAttachmentFile(MultipartFile file) {
        ticketSettingsService.validateAttachmentFile(file, ticketSettingsService.getOrCreate());
    }

    private byte[] readAttachmentBytes(MultipartFile file) {
        validateAttachmentFile(file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_UPLOAD_FAILED);
        }
        long maxBytes = ticketSettingsService.getOrCreate().maxAttachmentBytes();
        if (bytes.length == 0 || bytes.length > maxBytes) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }
        return bytes;
    }

    private String normalizeFileName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String fileName = StringUtils.hasText(originalName) ? originalName.trim() : "attachment";
        if (fileName.length() > 255) {
            fileName = fileName.substring(fileName.length() - 255);
        }
        return fileName;
    }

    private TicketAttachment toTicketAttachment(MultipartFile file) {
        byte[] bytes = readAttachmentBytes(file);
        TicketAttachment attachment = new TicketAttachment();
        attachment.setFileName(normalizeFileName(file));
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(bytes.length);
        attachment.setData(bytes);
        return attachment;
    }

    private TicketMessageAttachment toMessageAttachment(MultipartFile file) {
        byte[] bytes = readAttachmentBytes(file);
        TicketMessageAttachment attachment = new TicketMessageAttachment();
        attachment.setFileName(normalizeFileName(file));
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(bytes.length);
        attachment.setData(bytes);
        return attachment;
    }

    private String buildPublicNumber(Long id) {
        return String.format(Locale.ROOT, "TCK-%d-%06d", Year.now().getValue(), id);
    }

    private TicketDetailDto toDetailDto(Ticket ticket, User viewer, boolean includeWorkflow) {
        List<TicketMessageDto> messages = new ArrayList<>();
        messages.add(toInitialMessageDto(ticket, viewer));

        List<TicketMessage> replies = includeWorkflow
                ? ticketMessageRepository.findByTicketOrderByCreatedAtAscIdAsc(ticket)
                : ticketMessageRepository.findByTicketAndInternalNoteFalseOrderByCreatedAtAscIdAsc(ticket);
        for (TicketMessage message : replies) {
            messages.add(toMessageDto(message, ticket, viewer, includeWorkflow));
        }

        boolean closed = ticket.getStatus() == TicketStatus.CLOSED;
        boolean deleted = ticket.isDeleted();
        boolean archived = ticket.isArchived();
        boolean canReply = !closed && !archived && !deleted;
        List<TicketStatus> allowedNext = includeWorkflow && !deleted
                ? ticketStatusWorkflowService.allowedNextStatuses(ticket.getStatus())
                : List.of();
        if ((closed && !canReopen(ticket)) || archived || deleted) {
            allowedNext = List.of();
        }
        TicketDetailDto detail = new TicketDetailDto(toDto(ticket), messages, canReply, allowedNext);
        detail.setCanClose(!closed && !deleted && !archived);
        detail.setCanReopen(canReopen(ticket) && !deleted);
        detail.setCanArchive(includeWorkflow && closed && !archived && !deleted);
        detail.setCanUnarchive(includeWorkflow && archived && !deleted);
        detail.setCanSoftDelete(includeWorkflow && !deleted);
        detail.setCanRestore(includeWorkflow && deleted);
        detail.setCanMerge(includeWorkflow && !deleted && !archived && !ticket.isMerged());
        long replyCount = ticketMessageRepository.findByTicketOrderByCreatedAtAscIdAsc(ticket).size();
        detail.setCanSplit(includeWorkflow && !deleted && !archived && !ticket.isMerged() && replyCount > 0);
        detail.setCanLinkRelated(includeWorkflow && !deleted && !ticket.isMerged());
        detail.setCanEditDueDate(includeWorkflow && !deleted && !ticket.isMerged());
        detail.setCanWatch(includeWorkflow && !deleted);
        detail.setCanTransfer(includeWorkflow && !deleted && !archived && !ticket.isMerged());
        detail.setCanEscalate(includeWorkflow && !deleted && !archived && !ticket.isMerged()
                && ticket.getStatus() != TicketStatus.CLOSED);
        if (includeWorkflow && ticket.getId() != null) {
            boolean watching = viewer != null && viewer.getId() != null
                    && ticketWatcherRepository.existsByTicketIdAndUserId(ticket.getId(), viewer.getId());
            detail.setWatching(watching);
            List<TicketAssigneeOptionDto> watchers = ticketWatcherRepository
                    .findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                    .stream()
                    .map(TicketWatcher::getUser)
                    .filter(user -> user != null && user.isEnabled())
                    .map(user -> new TicketAssigneeOptionDto(user.getId(), displayName(user), user.getEmail()))
                    .toList();
            detail.setWatchers(watchers);
            detail.setTransfers(ticketTransferRepository
                    .findByTicketIdOrderByCreatedAtDescIdDesc(ticket.getId())
                    .stream()
                    .map(this::toTransferDto)
                    .toList());
            detail.setEscalations(dedupeEscalationHistory(ticketEscalationRepository
                    .findByTicketIdOrderByCreatedAtDescIdDesc(ticket.getId()))
                    .stream()
                    .map(this::toEscalationDto)
                    .toList());
        }
        detail.setReopenUntil(reopenDeadline(ticket));
        detail.setReopenWindowDays(ticketSettingsService.getReopenWindowDays());
        if (viewer != null) {
            ticketReplyDraftRepository.findByTicketAndAuthor(ticket, viewer)
                    .filter(draft -> StringUtils.hasText(draft.getBody()))
                    .ifPresent(draft -> detail.setReplyDraft(toDraftDto(draft)));
        }
        return detail;
    }

    private TicketMessageDto toInitialMessageDto(Ticket ticket, User viewer) {
        TicketMessageDto dto = new TicketMessageDto();
        dto.setId(null);
        dto.setBody(ticket.getDescription());
        dto.setInitial(true);
        dto.setStaff(false);
        if (ticket.getRequester() != null) {
            dto.setAuthorId(ticket.getRequester().getId());
            dto.setAuthorEmail(ticket.getRequester().getEmail());
            dto.setAuthorName(displayName(ticket.getRequester()));
            dto.setMine(viewer != null && viewer.getId() != null
                    && viewer.getId().equals(ticket.getRequester().getId()));
        }
        dto.setCreatedAt(ticket.getCreatedAt());
        boolean editable = canEditInitialDescription(ticket, viewer);
        dto.setCanEdit(editable);
        dto.setCanDelete(false);
        if (ticket.getId() != null) {
            dto.setHasRevisions(ticketMessageRevisionRepository.existsByTicketIdAndMessageIsNull(ticket.getId()));
        }
        dto.setEdited(ticket.getId() != null
                && ticketMessageRevisionRepository.existsByTicketIdAndMessageIsNull(ticket.getId()));

        List<TicketAttachmentDto> attachmentDtos = new ArrayList<>();
        for (TicketAttachment attachment : ticket.getAttachments()) {
            attachmentDtos.add(new TicketAttachmentDto(
                    attachment.getId(),
                    attachment.getFileName(),
                    attachment.getContentType(),
                    attachment.getSizeBytes()
            ));
        }
        dto.setAttachments(attachmentDtos);
        applyReadReceipts(dto, ticket, false, ticket.getCreatedAt());
        return dto;
    }

    private TicketMessageDto toMessageDto(
            TicketMessage message,
            Ticket ticket,
            User viewer,
            boolean includeWorkflow) {
        TicketMessageDto dto = new TicketMessageDto();
        dto.setId(message.getId());
        boolean deleted = message.isDeleted();
        dto.setDeleted(deleted);
        dto.setBody(deleted ? null : message.getBody());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setEditedAt(message.getEditedAt());
        dto.setDeletedAt(message.getDeletedAt());
        dto.setEdited(message.getEditedAt() != null);
        dto.setInitial(false);
        if (message.getAuthor() != null) {
            dto.setAuthorId(message.getAuthor().getId());
            dto.setAuthorEmail(message.getAuthor().getEmail());
            dto.setAuthorName(displayName(message.getAuthor()));
            dto.setMine(viewer != null && viewer.getId() != null
                    && viewer.getId().equals(message.getAuthor().getId()));
            dto.setStaff(isStaffUser(message.getAuthor()));
        }
        dto.setInternalNote(message.isInternalNote());
        boolean editable = canEditMessage(message, ticket, viewer, includeWorkflow);
        dto.setCanEdit(editable);
        dto.setCanDelete(editable);
        dto.setHasRevisions(ticketMessageRevisionRepository.existsByMessageId(message.getId()));

        List<TicketAttachmentDto> attachmentDtos = new ArrayList<>();
        if (!deleted) {
            for (TicketMessageAttachment attachment : message.getAttachments()) {
                attachmentDtos.add(new TicketAttachmentDto(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getContentType(),
                        attachment.getSizeBytes()
                ));
            }
        }
        dto.setAttachments(attachmentDtos);
        applyReadReceipts(dto, ticket, dto.isStaff(), message.getCreatedAt());
        return dto;
    }

    private void applyReadReceipts(
            TicketMessageDto dto,
            Ticket ticket,
            boolean staffMessage,
            Instant messageAt) {
        if (dto.isDeleted() || dto.isInternalNote() || messageAt == null) {
            return;
        }
        if (staffMessage) {
            dto.setSeenByCustomer(isReadAtOrAfter(ticket.getCustomerLastReadAt(), messageAt));
        } else {
            dto.setSeenByStaff(isReadAtOrAfter(ticket.getStaffLastReadAt(), messageAt));
        }
    }

    private boolean isReadAtOrAfter(Instant readAt, Instant messageAt) {
        return readAt != null && !readAt.isBefore(messageAt);
    }

    private void markReadByCustomer(Ticket ticket) {
        ticket.setCustomerLastReadAt(Instant.now());
        ticketRepository.save(ticket);
    }

    private void markReadByStaff(Ticket ticket) {
        ticket.setStaffLastReadAt(Instant.now());
        ticketRepository.save(ticket);
    }

    private TicketReplyDraftDto saveReplyDraft(
            Ticket ticket,
            User author,
            SaveTicketReplyDraftRequest request,
            boolean asStaff) {
        assertNotDeleted(ticket);
        String body = request != null && request.getBody() != null ? request.getBody().trim() : "";
        if (body.length() > REPLY_MAX) {
            throw new ApiException(ErrorCode.TICKET_REPLY_BODY_TOO_LONG);
        }
        boolean internalNote = asStaff && request != null && Boolean.TRUE.equals(request.getInternalNote());

        if (!StringUtils.hasText(body)) {
            clearReplyDraft(ticket, author);
            return null;
        }

        TicketReplyDraft draft = ticketReplyDraftRepository.findByTicketAndAuthor(ticket, author)
                .orElseGet(() -> {
                    TicketReplyDraft created = new TicketReplyDraft();
                    created.setTicket(ticket);
                    created.setAuthor(author);
                    return created;
                });
        draft.setBody(body);
        draft.setInternalNote(internalNote);
        return toDraftDto(ticketReplyDraftRepository.save(draft));
    }

    private void clearReplyDraft(Ticket ticket, User author) {
        if (ticket == null || author == null) {
            return;
        }
        ticketReplyDraftRepository.deleteByTicketAndAuthor(ticket, author);
    }

    private TicketReplyDraftDto toDraftDto(TicketReplyDraft draft) {
        return new TicketReplyDraftDto(draft.getBody(), draft.isInternalNote(), draft.getUpdatedAt());
    }

    private boolean canEditMessage(
            TicketMessage message,
            Ticket ticket,
            User viewer,
            boolean asStaff) {
        if (message.isDeleted()) {
            return false;
        }
        if (viewer == null || viewer.getId() == null || message.getAuthor() == null) {
            return false;
        }
        if (!viewer.getId().equals(message.getAuthor().getId())) {
            return false;
        }
        assertNotDeleted(ticket);
        if (ticket.isArchived() || ticket.getStatus() == TicketStatus.CLOSED) {
            return false;
        }
        if (!asStaff) {
            if (message.isInternalNote()) {
                return false;
            }
        }
        return true;
    }

    private boolean canEditInitialDescription(Ticket ticket, User viewer) {
        if (ticket == null || ticket.isDeleted() || ticket.isArchived()
                || ticket.getStatus() == TicketStatus.CLOSED) {
            return false;
        }
        if (viewer == null || viewer.getId() == null || ticket.getRequester() == null) {
            return false;
        }
        return viewer.getId().equals(ticket.getRequester().getId());
    }

    private TicketMessage requireEditableMessage(
            User actor,
            Ticket ticket,
            Long messageId,
            boolean asStaff) {
        TicketMessage message = ticketMessageRepository.findByIdAndTicketId(messageId, ticket.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_MESSAGE_NOT_FOUND));
        if (!canEditMessage(message, ticket, actor, asStaff)) {
            if (message.getAuthor() == null
                    || actor.getId() == null
                    || !actor.getId().equals(message.getAuthor().getId())) {
                throw new ApiException(ErrorCode.TICKET_MESSAGE_NOT_OWNED);
            }
            throw new ApiException(ErrorCode.TICKET_MESSAGE_NOT_EDITABLE);
        }
        return message;
    }

    private void recordRevision(
            TicketMessage message,
            User actor,
            TicketMessageRevision.Action action,
            String previousBody,
            String newBody) {
        TicketMessageRevision revision = new TicketMessageRevision();
        revision.setTicket(message.getTicket());
        revision.setMessage(message);
        revision.setActor(actor);
        revision.setAction(action);
        revision.setPreviousBody(previousBody);
        revision.setNewBody(newBody);
        ticketMessageRevisionRepository.save(revision);
    }

    private void recordDescriptionRevision(
            Ticket ticket,
            User actor,
            String previousBody,
            String newBody) {
        TicketMessageRevision revision = new TicketMessageRevision();
        revision.setTicket(ticket);
        revision.setActor(actor);
        revision.setAction(TicketMessageRevision.Action.EDIT);
        revision.setPreviousBody(previousBody);
        revision.setNewBody(newBody);
        ticketMessageRevisionRepository.save(revision);
    }

    private TicketMessageRevisionDto toRevisionDto(TicketMessageRevision revision) {
        TicketMessageRevisionDto dto = new TicketMessageRevisionDto();
        dto.setId(revision.getId());
        dto.setAction(revision.getAction());
        dto.setPreviousBody(revision.getPreviousBody());
        dto.setNewBody(revision.getNewBody());
        dto.setCreatedAt(revision.getCreatedAt());
        if (revision.getActor() != null) {
            dto.setActorId(revision.getActor().getId());
            dto.setActorEmail(revision.getActor().getEmail());
            dto.setActorName(displayName(revision.getActor()));
        }
        return dto;
    }

    private TicketMessageDto toMessageDto(TicketMessage message, User viewer) {
        return toMessageDto(message, message.getTicket(), viewer, isStaffUser(viewer));
    }

    private boolean isStaffUser(User user) {
        return user != null && user.getRole() == User.Role.ADMIN;
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return StringUtils.hasText(full) ? full : user.getEmail();
    }

    private TicketTransferDto toTransferDto(TicketTransfer transfer) {
        TicketTransferDto dto = new TicketTransferDto();
        dto.setId(transfer.getId());
        dto.setNote(transfer.getNote());
        dto.setCreatedAt(transfer.getCreatedAt());
        if (transfer.getTransferredBy() != null) {
            dto.setTransferredById(transfer.getTransferredBy().getId());
            dto.setTransferredByName(displayName(transfer.getTransferredBy()));
        }
        if (transfer.getFromAssignee() != null) {
            dto.setFromAssigneeId(transfer.getFromAssignee().getId());
            dto.setFromAssigneeName(displayName(transfer.getFromAssignee()));
        }
        if (transfer.getToAssignee() != null) {
            dto.setToAssigneeId(transfer.getToAssignee().getId());
            dto.setToAssigneeName(displayName(transfer.getToAssignee()));
        }
        if (transfer.getFromQueue() != null) {
            dto.setFromQueueId(transfer.getFromQueue().getId());
            dto.setFromQueueName(transfer.getFromQueue().getName());
        }
        if (transfer.getToQueue() != null) {
            dto.setToQueueId(transfer.getToQueue().getId());
            dto.setToQueueName(transfer.getToQueue().getName());
        }
        return dto;
    }

    private List<TicketEscalation> dedupeEscalationHistory(List<TicketEscalation> escalations) {
        if (escalations == null || escalations.isEmpty()) {
            return List.of();
        }
        List<TicketEscalation> out = new ArrayList<>();
        for (TicketEscalation current : escalations) {
            if (!out.isEmpty() && isNearDuplicateSlaEscalation(out.get(out.size() - 1), current)) {
                continue;
            }
            out.add(current);
        }
        return out;
    }

    /**
     * Hides race duplicates when inbox load and the SLA scheduler escalated the same ticket
     * within a few seconds before escalatedAt locking was added.
     */
    private boolean isNearDuplicateSlaEscalation(TicketEscalation newer, TicketEscalation older) {
        if (newer == null || older == null) {
            return false;
        }
        if (newer.getTriggerType() != TicketEscalationTrigger.SLA_BREACH
                || older.getTriggerType() != TicketEscalationTrigger.SLA_BREACH) {
            return false;
        }
        if (newer.getCreatedAt() == null || older.getCreatedAt() == null) {
            return false;
        }
        long seconds = Math.abs(Duration.between(newer.getCreatedAt(), older.getCreatedAt()).getSeconds());
        if (seconds > 10) {
            return false;
        }
        return newer.getFromPriority() == older.getFromPriority()
                && newer.getToPriority() == older.getToPriority()
                && sameNullableUser(newer.getFromAssignee(), older.getFromAssignee())
                && sameNullableUser(newer.getToAssignee(), older.getToAssignee())
                && isSameQueue(newer.getFromQueue(), older.getFromQueue())
                && isSameQueue(newer.getToQueue(), older.getToQueue());
    }

    private boolean sameNullableUser(User a, User b) {
        if (a == null && b == null) {
            return true;
        }
        return isSameUser(a, b);
    }

    private boolean isSameQueue(TicketQueue a, TicketQueue b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null || a.getId() == null || b.getId() == null) {
            return false;
        }
        return a.getId().equals(b.getId());
    }

    private TicketEscalationDto toEscalationDto(TicketEscalation escalation) {
        TicketEscalationDto dto = new TicketEscalationDto();
        dto.setId(escalation.getId());
        dto.setTriggerType(escalation.getTriggerType());
        dto.setFromPriority(escalation.getFromPriority());
        dto.setToPriority(escalation.getToPriority());
        dto.setNote(escalation.getNote());
        dto.setCreatedAt(escalation.getCreatedAt());
        if (escalation.getEscalatedBy() != null) {
            dto.setEscalatedById(escalation.getEscalatedBy().getId());
            dto.setEscalatedByName(displayName(escalation.getEscalatedBy()));
        }
        if (escalation.getFromAssignee() != null) {
            dto.setFromAssigneeId(escalation.getFromAssignee().getId());
            dto.setFromAssigneeName(displayName(escalation.getFromAssignee()));
        }
        if (escalation.getToAssignee() != null) {
            dto.setToAssigneeId(escalation.getToAssignee().getId());
            dto.setToAssigneeName(displayName(escalation.getToAssignee()));
        }
        if (escalation.getFromQueue() != null) {
            dto.setFromQueueId(escalation.getFromQueue().getId());
            dto.setFromQueueName(escalation.getFromQueue().getName());
        }
        if (escalation.getToQueue() != null) {
            dto.setToQueueId(escalation.getToQueue().getId());
            dto.setToQueueName(escalation.getToQueue().getName());
        }
        return dto;
    }

    private TicketDto toListDto(Ticket ticket) {
        TicketDto dto = toDtoBase(ticket);
        dto.setAttachments(List.of());
        return dto;
    }

    private TicketDto toDto(Ticket ticket) {
        TicketDto dto = toDtoBase(ticket);

        List<TicketAttachmentDto> attachmentDtos = new ArrayList<>();
        for (TicketAttachment attachment : ticket.getAttachments()) {
            attachmentDtos.add(new TicketAttachmentDto(
                    attachment.getId(),
                    attachment.getFileName(),
                    attachment.getContentType(),
                    attachment.getSizeBytes()
            ));
        }
        dto.setAttachments(attachmentDtos);

        if (ticket.getId() != null) {
            List<String> mergedSources = ticketRepository
                    .findByMergedIntoIdAndDeletedAtIsNullOrderByCreatedAtAsc(ticket.getId())
                    .stream()
                    .map(Ticket::getPublicNumber)
                    .filter(StringUtils::hasText)
                    .toList();
            dto.setMergedSourcePublicNumbers(mergedSources);

            List<String> splitChildren = ticketRepository
                    .findBySplitFromIdAndDeletedAtIsNullOrderByCreatedAtAsc(ticket.getId())
                    .stream()
                    .map(Ticket::getPublicNumber)
                    .filter(StringUtils::hasText)
                    .toList();
            dto.setSplitChildPublicNumbers(splitChildren);

            List<RelatedTicketDto> relatedTickets = ticketRelatedLinkRepository
                    .findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                    .stream()
                    .map(link -> toRelatedDto(link.getRelatedTicket()))
                    .toList();
            dto.setRelatedTickets(relatedTickets);
        }
        return dto;
    }

    private RelatedTicketDto toRelatedDto(Ticket related) {
        RelatedTicketDto dto = new RelatedTicketDto();
        dto.setId(related.getId());
        dto.setPublicNumber(related.getPublicNumber());
        dto.setSubject(related.getSubject());
        dto.setStatus(related.getStatus());
        if (related.getRequester() != null) {
            dto.setRequesterName(displayName(related.getRequester()));
        }
        return dto;
    }

    private TicketDto toDtoBase(Ticket ticket) {
        TicketDto dto = new TicketDto();
        dto.setId(ticket.getId());
        dto.setPublicNumber(ticket.getPublicNumber());
        dto.setSubject(ticket.getSubject());
        dto.setDescription(ticket.getDescription());
        if (ticket.getCategory() != null) {
            dto.setCategory(ticketCategoryService.toDto(ticket.getCategory()));
        }
        if (ticket.getQueue() != null) {
            dto.setQueue(ticketQueueService.toSummaryDto(ticket.getQueue()));
        }
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());
        dto.setChannel(ticket.getChannel());
        dto.setDueAt(ticket.getDueAt());
        dto.setEscalatedAt(ticket.getEscalatedAt());
        dto.setEscalated(ticket.isEscalated());
        dto.setClosedAt(ticket.getClosedAt());
        dto.setArchivedAt(ticket.getArchivedAt());
        dto.setDeletedAt(ticket.getDeletedAt());
        dto.setArchived(ticket.isArchived());
        dto.setDeleted(ticket.isDeleted());
        if (ticket.getMergedInto() != null) {
            dto.setMergedIntoId(ticket.getMergedInto().getId());
            dto.setMergedIntoPublicNumber(ticket.getMergedInto().getPublicNumber());
        }
        if (ticket.getSplitFrom() != null) {
            dto.setSplitFromId(ticket.getSplitFrom().getId());
            dto.setSplitFromPublicNumber(ticket.getSplitFrom().getPublicNumber());
        }
        dto.setOverdue(AdminTicketService.isOverdue(ticket, Instant.now()));
        if (ticket.getRequester() != null) {
            dto.setRequesterId(ticket.getRequester().getId());
            dto.setRequesterEmail(ticket.getRequester().getEmail());
            dto.setRequesterName(displayName(ticket.getRequester()));
        }
        if (ticket.getAssignee() != null) {
            dto.setAssigneeId(ticket.getAssignee().getId());
            dto.setAssigneeEmail(ticket.getAssignee().getEmail());
            dto.setAssigneeName(displayName(ticket.getAssignee()));
        }
        if (ticket.getTags() != null && !ticket.getTags().isEmpty()) {
            dto.setTags(ticket.getTags().stream()
                    .sorted(Comparator.comparing(TicketTag::getName, String.CASE_INSENSITIVE_ORDER))
                    .map(ticketTagService::toDto)
                    .toList());
        }
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        return dto;
    }
}
