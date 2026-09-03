package com.domainify.service;

import com.domainify.dto.PagedResponse;
import com.domainify.dto.TicketAttachmentDto;
import com.domainify.dto.TicketDetailDto;
import com.domainify.dto.TicketDto;
import com.domainify.dto.TicketMessageDto;
import com.domainify.dto.UpdateTicketTagsRequest;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketAttachment;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketChannel;
import com.domainify.entity.TicketMessage;
import com.domainify.entity.TicketMessageAttachment;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.TicketTag;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketAttachmentRepository;
import com.domainify.repository.TicketMessageAttachmentRepository;
import com.domainify.repository.TicketMessageRepository;
import com.domainify.repository.TicketRepository;
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
    private static final int MAX_ATTACHMENTS = 5;
    private static final long MAX_ATTACHMENT_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "subject", "status", "priority", "publicNumber", "id"
    );

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TicketMessageAttachmentRepository ticketMessageAttachmentRepository;
    private final TicketCategoryService ticketCategoryService;
    private final TicketMentionService ticketMentionService;
    private final TicketStatusWorkflowService ticketStatusWorkflowService;
    private final TicketTagService ticketTagService;
    private final TicketSettingsService ticketSettingsService;

    public TicketService(
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            TicketAttachmentRepository ticketAttachmentRepository,
            TicketMessageAttachmentRepository ticketMessageAttachmentRepository,
            TicketCategoryService ticketCategoryService,
            TicketMentionService ticketMentionService,
            TicketStatusWorkflowService ticketStatusWorkflowService,
            TicketTagService ticketTagService,
            TicketSettingsService ticketSettingsService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.ticketMessageAttachmentRepository = ticketMessageAttachmentRepository;
        this.ticketCategoryService = ticketCategoryService;
        this.ticketMentionService = ticketMentionService;
        this.ticketStatusWorkflowService = ticketStatusWorkflowService;
        this.ticketTagService = ticketTagService;
        this.ticketSettingsService = ticketSettingsService;
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

    @Transactional(readOnly = true)
    public TicketDetailDto getMine(User requester, Long ticketId) {
        Ticket ticket = requireOwnedTicket(requester, ticketId);
        return toDetailDto(ticket, requester, false);
    }

    @Transactional(readOnly = true)
    public TicketDetailDto getForStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto reply(User requester, Long ticketId, String body, MultipartFile[] attachments) {
        Ticket ticket = requireOwnedTicket(requester, ticketId);
        return addReply(ticket, requester, body, attachments, false);
    }

    @Transactional
    public TicketDetailDto replyAsStaff(User agent, Long ticketId, String body, MultipartFile[] attachments) {
        requireAgent(agent);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        return addReply(ticket, agent, body, attachments, true);
    }

    private TicketDetailDto addReply(
            Ticket ticket,
            User author,
            String body,
            MultipartFile[] attachments,
            boolean asStaff) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
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
        if (files.size() > MAX_ATTACHMENTS) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENTS_LIMIT);
        }

        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setAuthor(author);
        message.setBody(trimmedBody);
        message.setInternalNote(false);
        for (MultipartFile file : files) {
            message.addAttachment(toMessageAttachment(file));
        }
        ticketMessageRepository.save(message);
        ticketMentionService.syncMentions(ticket, message, trimmedBody, author);

        if (asStaff) {
            if (ticket.getStatus() == TicketStatus.NEW || ticket.getStatus() == TicketStatus.OPEN) {
                maybeAutoTransition(ticket, TicketStatus.PENDING);
            }
            if (ticket.getAssignee() == null) {
                ticket.setAssignee(author);
            }
        } else {
            if (ticket.getStatus() == TicketStatus.RESOLVED
                    || ticket.getStatus() == TicketStatus.PENDING
                    || ticket.getStatus() == TicketStatus.ON_HOLD) {
                maybeAutoTransition(ticket, TicketStatus.OPEN);
            } else if (ticket.getStatus() == TicketStatus.NEW) {
                maybeAutoTransition(ticket, TicketStatus.OPEN);
            }
        }
        ticketRepository.save(ticket);

        return toDetailDto(ticket, author, isStaffUser(author));
    }

    @Transactional
    public TicketDetailDto updateStatusAsStaff(User agent, Long ticketId, TicketStatus nextStatus) {
        requireAgent(agent);
        if (nextStatus == null) {
            throw new ApiException(ErrorCode.TICKET_STATUS_INVALID);
        }
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        if (ticket.getStatus() == TicketStatus.CLOSED && nextStatus != TicketStatus.CLOSED) {
            assertReopenAllowed(ticket);
        }
        ticketStatusWorkflowService.assertTransitionAllowed(ticket.getStatus(), nextStatus);
        applyStatus(ticket, nextStatus);
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    @Transactional
    public TicketDetailDto closeAsStaff(User agent, Long ticketId) {
        requireAgent(agent);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
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
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
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
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        Set<TicketTag> tags = ticketTagService.resolveTags(
                request != null ? request.getTagIds() : null,
                request != null ? request.getNames() : null
        );
        ticket.setTags(new HashSet<>(tags));
        ticketRepository.save(ticket);
        return toDetailDto(ticket, agent, true);
    }

    private TicketDetailDto closeTicket(Ticket ticket, User viewer, boolean asStaff) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_CLOSED);
        }
        applyStatus(ticket, TicketStatus.CLOSED);
        ticketRepository.save(ticket);
        return toDetailDto(ticket, viewer, asStaff);
    }

    private TicketDetailDto reopenTicket(Ticket ticket, User viewer, boolean asStaff) {
        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new ApiException(ErrorCode.TICKET_NOT_CLOSED);
        }
        assertReopenAllowed(ticket);
        applyStatus(ticket, TicketStatus.OPEN);
        ticketRepository.save(ticket);
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
        if (message.isInternalNote()) {
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
        return ticketRepository.findByIdAndRequesterId(ticketId, requester.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
    }

    private Specification<Ticket> buildMineSpec(Long requesterId, TicketStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("requester").get("id"), requesterId));

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
        if (files.size() > MAX_ATTACHMENTS) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENTS_LIMIT);
        }

        Ticket ticket = new Ticket();
        ticket.setSubject(trimmedSubject);
        ticket.setDescription(trimmedDescription);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setChannel(TicketChannel.PORTAL);
        ticket.setRequester(requester);
        ticket.setPublicNumber("TMP-" + System.nanoTime());
        ticket.setDueAt(AdminTicketService.computeDueAt(priority, Instant.now()));

        for (MultipartFile file : files) {
            ticket.addAttachment(toTicketAttachment(file));
        }

        Ticket saved = ticketRepository.saveAndFlush(ticket);
        saved.setPublicNumber(buildPublicNumber(saved.getId()));
        saved = ticketRepository.save(saved);
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
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }
    }

    private byte[] readAttachmentBytes(MultipartFile file) {
        validateAttachmentFile(file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_UPLOAD_FAILED);
        }
        if (bytes.length == 0 || bytes.length > MAX_ATTACHMENT_BYTES) {
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

        for (TicketMessage message : ticketMessageRepository
                .findByTicketAndInternalNoteFalseOrderByCreatedAtAscIdAsc(ticket)) {
            messages.add(toMessageDto(message, viewer));
        }

        boolean closed = ticket.getStatus() == TicketStatus.CLOSED;
        boolean canReply = !closed;
        List<TicketStatus> allowedNext = includeWorkflow
                ? ticketStatusWorkflowService.allowedNextStatuses(ticket.getStatus())
                : List.of();
        if (closed && !canReopen(ticket)) {
            allowedNext = List.of();
        }
        TicketDetailDto detail = new TicketDetailDto(toDto(ticket), messages, canReply, allowedNext);
        detail.setCanClose(!closed);
        detail.setCanReopen(canReopen(ticket));
        detail.setReopenUntil(reopenDeadline(ticket));
        detail.setReopenWindowDays(ticketSettingsService.getReopenWindowDays());
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
        return dto;
    }

    private TicketMessageDto toMessageDto(TicketMessage message, User viewer) {
        TicketMessageDto dto = new TicketMessageDto();
        dto.setId(message.getId());
        dto.setBody(message.getBody());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setInitial(false);
        if (message.getAuthor() != null) {
            dto.setAuthorId(message.getAuthor().getId());
            dto.setAuthorEmail(message.getAuthor().getEmail());
            dto.setAuthorName(displayName(message.getAuthor()));
            dto.setMine(viewer != null && viewer.getId() != null
                    && viewer.getId().equals(message.getAuthor().getId()));
            dto.setStaff(isStaffUser(message.getAuthor()));
        }

        List<TicketAttachmentDto> attachmentDtos = new ArrayList<>();
        for (TicketMessageAttachment attachment : message.getAttachments()) {
            attachmentDtos.add(new TicketAttachmentDto(
                    attachment.getId(),
                    attachment.getFileName(),
                    attachment.getContentType(),
                    attachment.getSizeBytes()
            ));
        }
        dto.setAttachments(attachmentDtos);
        return dto;
    }

    private boolean isStaffUser(User user) {
        return user != null && user.getRole() == User.Role.ADMIN;
    }

    private String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return StringUtils.hasText(full) ? full : user.getEmail();
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
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());
        dto.setChannel(ticket.getChannel());
        dto.setDueAt(ticket.getDueAt());
        dto.setClosedAt(ticket.getClosedAt());
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
