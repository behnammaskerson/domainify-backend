package com.domainify.service;

import com.domainify.dto.TicketImportResultDto;
import com.domainify.dto.TicketImportRowFailureDto;
import com.domainify.dto.TicketImportSucceededDto;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketChannel;
import com.domainify.entity.TicketMessage;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketQueue;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.TicketTag;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketCategoryRepository;
import com.domainify.repository.TicketMessageRepository;
import com.domainify.repository.TicketQueueRepository;
import com.domainify.repository.TicketRepository;
import com.domainify.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Migrates tickets from a helpdesk CSV export into Domainify.
 * Skips notifications, auto-assign, and business rules.
 */
@Service
public class TicketImportService {

    public static final String TICKETS_TEMPLATE_CSV = """
            external_id,public_number,subject,description,status,priority,channel,category,queue,tags,requester_email,requester_name,guest_email,guest_name,assignee_email,created_at,updated_at,closed_at,resolved_at
            HD-1001,,Cannot renew domain,Customer cannot renew example.com,OPEN,HIGH,EMAIL,Technical,,domains;billing,customer@example.com,Jane Customer,,,admin@example.com,2024-06-01T10:00:00Z,2024-06-02T12:00:00Z,,
            """;

    public static final String MESSAGES_TEMPLATE_CSV = """
            external_id,public_number,seq,author_email,body,internal_note,created_at
            HD-1001,,1,customer@example.com,I still cannot renew my domain,false,2024-06-01T10:05:00Z
            HD-1001,,2,admin@example.com,We are checking registrar status,false,2024-06-01T11:00:00Z
            """;

    private static final int MAX_TICKET_ROWS = 2000;
    private static final int MAX_MESSAGE_ROWS = 20000;
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final int SUBJECT_MAX = 200;
    private static final int DESCRIPTION_MAX = 10000;
    private static final int REPLY_MAX = 10000;
    private static final int PUBLIC_NUMBER_MAX = 32;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final TicketQueueRepository ticketQueueRepository;
    private final UserRepository userRepository;
    private final TicketTagService ticketTagService;
    private final PasswordPolicyService passwordPolicyService;
    private final MessageService messageService;
    private final TransactionTemplate transactionTemplate;

    public TicketImportService(
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            TicketCategoryRepository ticketCategoryRepository,
            TicketQueueRepository ticketQueueRepository,
            UserRepository userRepository,
            TicketTagService ticketTagService,
            PasswordPolicyService passwordPolicyService,
            MessageService messageService,
            PlatformTransactionManager transactionManager) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.ticketQueueRepository = ticketQueueRepository;
        this.userRepository = userRepository;
        this.ticketTagService = ticketTagService;
        this.passwordPolicyService = passwordPolicyService;
        this.messageService = messageService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public TicketImportResultDto importTickets(
            User agent,
            MultipartFile ticketsFile,
            MultipartFile messagesFile,
            boolean dryRun,
            boolean createMissingRequesters) {
        if (agent == null || agent.getId() == null || agent.getRole() != User.Role.ADMIN) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        validateUpload(ticketsFile);

        List<TicketCsvRow> ticketRows = parseTicketRows(ticketsFile);
        if (ticketRows.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_EMPTY);
        }
        if (ticketRows.size() > MAX_TICKET_ROWS) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_TOO_MANY_ROWS);
        }

        Map<String, List<MessageCsvRow>> messagesByKey = Map.of();
        if (messagesFile != null && !messagesFile.isEmpty()) {
            validateUpload(messagesFile);
            messagesByKey = parseMessageRows(messagesFile);
        }

        TicketImportResultDto result = new TicketImportResultDto();
        result.setDryRun(dryRun);
        result.setTotalRows(ticketRows.size());

        List<TicketImportSucceededDto> succeeded = new ArrayList<>();
        List<TicketImportRowFailureDto> failed = new ArrayList<>();

        for (TicketCsvRow row : ticketRows) {
            try {
                List<MessageCsvRow> messages = resolveMessagesForTicket(row, messagesByKey);
                if (dryRun) {
                    validateRow(row, messages, createMissingRequesters, true);
                    succeeded.add(new TicketImportSucceededDto(
                            row.rowNumber,
                            blankToNull(row.externalId),
                            null,
                            blankToNull(row.publicNumber)));
                } else {
                    TicketImportSucceededDto imported = transactionTemplate.execute(status ->
                            importOne(agent, row, messages, createMissingRequesters));
                    if (imported != null) {
                        succeeded.add(imported);
                    }
                }
            } catch (ApiException ex) {
                failed.add(new TicketImportRowFailureDto(
                        row.rowNumber,
                        blankToNull(row.externalId),
                        ex.getCode().name(),
                        messageService.get(ex.getCode(), ex.getArgs())));
            } catch (RuntimeException ex) {
                ApiException api = findApiException(ex);
                if (api != null) {
                    failed.add(new TicketImportRowFailureDto(
                            row.rowNumber,
                            blankToNull(row.externalId),
                            api.getCode().name(),
                            messageService.get(api.getCode(), api.getArgs())));
                } else {
                    failed.add(new TicketImportRowFailureDto(
                            row.rowNumber,
                            blankToNull(row.externalId),
                            ErrorCode.UNEXPECTED_ERROR.name(),
                            messageService.get(ErrorCode.UNEXPECTED_ERROR)));
                }
            }
        }

        result.setSucceeded(succeeded);
        result.setFailed(failed);
        result.setValidRows(succeeded.size());
        result.setImportedCount(dryRun ? 0 : succeeded.size());
        result.setFailedCount(failed.size());
        return result;
    }

    private TicketImportSucceededDto importOne(
            User agent,
            TicketCsvRow row,
            List<MessageCsvRow> messages,
            boolean createMissingRequesters) {
        ResolvedTicket resolved = validateRow(row, messages, createMissingRequesters, false);

        Ticket ticket = new Ticket();
        ticket.setSubject(resolved.subject);
        ticket.setDescription(resolved.description);
        ticket.setCategory(resolved.category);
        ticket.setQueue(resolved.queue);
        ticket.setPriority(resolved.priority);
        ticket.setStatus(resolved.status);
        ticket.setChannel(resolved.channel);
        ticket.setRequester(resolved.requester);
        if (resolved.requester == null) {
            ticket.setGuestName(resolved.guestName);
            ticket.setGuestEmail(resolved.guestEmail);
            ticket.setGuestEmailVerified(true);
        }
        ticket.setAssignee(resolved.assignee);
        if (!resolved.tags.isEmpty()) {
            ticket.setTags(resolved.tags);
        }
        Instant createdAt = resolved.createdAt != null ? resolved.createdAt : Instant.now();
        Instant updatedAt = resolved.updatedAt != null ? resolved.updatedAt : createdAt;
        ticket.setCreatedAt(createdAt);
        ticket.setUpdatedAt(updatedAt);
        if (resolved.closedAt != null) {
            ticket.setClosedAt(resolved.closedAt);
        } else if (resolved.status == TicketStatus.CLOSED) {
            ticket.setClosedAt(updatedAt);
        }
        if (resolved.resolvedAt != null) {
            ticket.setResolvedAt(resolved.resolvedAt);
        } else if (resolved.status == TicketStatus.RESOLVED || resolved.status == TicketStatus.CLOSED) {
            ticket.setResolvedAt(updatedAt);
        }

        ticket.setPublicNumber("TMP-" + System.nanoTime());
        Ticket saved = ticketRepository.saveAndFlush(ticket);
        final Long savedId = saved.getId();

        String publicNumber = StringUtils.hasText(resolved.publicNumber)
                ? resolved.publicNumber
                : buildPublicNumber(savedId);
        if (ticketRepository.findByPublicNumberIgnoreCase(publicNumber)
                .filter(existing -> !existing.getId().equals(savedId))
                .isPresent()) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_PUBLIC_NUMBER_EXISTS);
        }
        saved.setPublicNumber(publicNumber);
        saved = ticketRepository.saveAndFlush(saved);

        for (MessageCsvRow messageRow : messages) {
            TicketMessage message = new TicketMessage();
            message.setTicket(saved);
            message.setBody(messageRow.body);
            message.setInternalNote(messageRow.internalNote);
            message.setAuthor(resolveMessageAuthor(messageRow.authorEmail, saved, agent));
            if (messageRow.createdAt != null) {
                message.setCreatedAt(messageRow.createdAt);
            }
            ticketMessageRepository.save(message);
        }

        if (StringUtils.hasText(row.externalId)) {
            TicketMessage note = new TicketMessage();
            note.setTicket(saved);
            note.setAuthor(agent);
            note.setInternalNote(true);
            note.setBody("Imported from helpdesk migration. External ID: " + row.externalId.trim());
            note.setCreatedAt(createdAt);
            ticketMessageRepository.save(note);
        }

        ticketRepository.patchImportTimestamps(
                saved.getId(),
                createdAt,
                updatedAt,
                saved.getClosedAt(),
                saved.getResolvedAt());

        return new TicketImportSucceededDto(
                row.rowNumber,
                blankToNull(row.externalId),
                saved.getId(),
                publicNumber);
    }

    private ResolvedTicket validateRow(
            TicketCsvRow row,
            List<MessageCsvRow> messages,
            boolean createMissingRequesters,
            boolean dryRun) {
        String subject = trim(row.subject);
        String description = trim(row.description);
        if (!StringUtils.hasText(subject)) {
            throw new ApiException(ErrorCode.TICKET_SUBJECT_REQUIRED);
        }
        if (subject.length() > SUBJECT_MAX) {
            throw new ApiException(ErrorCode.TICKET_SUBJECT_TOO_LONG);
        }
        if (!StringUtils.hasText(description)) {
            throw new ApiException(ErrorCode.TICKET_DESCRIPTION_REQUIRED);
        }
        if (description.length() > DESCRIPTION_MAX) {
            throw new ApiException(ErrorCode.TICKET_DESCRIPTION_TOO_LONG);
        }

        String publicNumber = trim(row.publicNumber);
        if (StringUtils.hasText(publicNumber)) {
            if (publicNumber.length() > PUBLIC_NUMBER_MAX) {
                throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_INVALID);
            }
            if (ticketRepository.findByPublicNumberIgnoreCase(publicNumber).isPresent()) {
                throw new ApiException(ErrorCode.TICKET_IMPORT_PUBLIC_NUMBER_EXISTS);
            }
        }

        TicketCategory category = resolveCategory(row.category);
        TicketQueue queue = resolveQueue(row.queue);
        TicketPriority priority = parsePriority(row.priority);
        TicketStatus status = parseStatus(row.status);
        TicketChannel channel = parseChannel(row.channel);

        User requester = null;
        String guestEmail = null;
        String guestName = null;
        String requesterEmail = normalizeEmail(row.requesterEmail);
        String guestEmailRaw = normalizeEmail(row.guestEmail);

        if (StringUtils.hasText(requesterEmail)) {
            requester = userRepository.findByEmailIgnoreCase(requesterEmail).orElse(null);
            if (requester == null) {
                if (!createMissingRequesters) {
                    throw new ApiException(ErrorCode.TICKET_REQUESTER_NOT_FOUND);
                }
                if (!dryRun) {
                    requester = createRequesterStub(requesterEmail, row.requesterName);
                }
            } else if (!requester.isEnabled() || requester.getRole() == User.Role.ADMIN) {
                throw new ApiException(ErrorCode.TICKET_REQUESTER_INVALID);
            }
        } else if (StringUtils.hasText(guestEmailRaw)) {
            guestEmail = guestEmailRaw;
            guestName = StringUtils.hasText(trim(row.guestName))
                    ? trim(row.guestName)
                    : guestEmailRaw;
            if (guestName.length() > 120) {
                throw new ApiException(ErrorCode.TICKET_GUEST_NAME_REQUIRED);
            }
        } else {
            throw new ApiException(ErrorCode.TICKET_REQUESTER_REQUIRED);
        }

        User assignee = null;
        String assigneeEmail = normalizeEmail(row.assigneeEmail);
        if (StringUtils.hasText(assigneeEmail)) {
            assignee = userRepository.findByEmailIgnoreCase(assigneeEmail)
                    .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ASSIGNEE_NOT_FOUND));
            if (assignee.getRole() != User.Role.ADMIN || !assignee.isEnabled()) {
                throw new ApiException(ErrorCode.TICKET_ASSIGNEE_INVALID);
            }
        }

        Set<TicketTag> tags = Set.of();
        List<String> tagNames = splitTags(row.tags);
        if (!tagNames.isEmpty()) {
            if (dryRun) {
                // Resolve without creating new tags when possible; still allow create-on-demand names length check.
                for (String name : tagNames) {
                    if (name.length() > 64) {
                        throw new ApiException(ErrorCode.TICKET_TAG_NAME_INVALID);
                    }
                }
            } else {
                tags = ticketTagService.resolveTags(null, tagNames);
            }
        }

        Instant createdAt = parseInstant(row.createdAt);
        Instant updatedAt = parseInstant(row.updatedAt);
        Instant closedAt = parseInstant(row.closedAt);
        Instant resolvedAt = parseInstant(row.resolvedAt);

        for (MessageCsvRow message : messages) {
            if (!StringUtils.hasText(message.body)) {
                throw new ApiException(ErrorCode.TICKET_REPLY_BODY_REQUIRED);
            }
            if (message.body.length() > REPLY_MAX) {
                throw new ApiException(ErrorCode.TICKET_REPLY_BODY_TOO_LONG);
            }
            String authorEmail = normalizeEmail(message.authorEmail);
            if (StringUtils.hasText(authorEmail)
                    && userRepository.findByEmailIgnoreCase(authorEmail).isEmpty()
                    && (requesterEmail == null || !authorEmail.equalsIgnoreCase(requesterEmail))
                    && (guestEmail == null || !authorEmail.equalsIgnoreCase(guestEmail))) {
                // Unknown authors are allowed; author will be left null / agent at write time.
            }
        }

        ResolvedTicket resolved = new ResolvedTicket();
        resolved.subject = subject;
        resolved.description = description;
        resolved.publicNumber = publicNumber;
        resolved.category = category;
        resolved.queue = queue;
        resolved.priority = priority;
        resolved.status = status;
        resolved.channel = channel;
        resolved.requester = requester;
        resolved.guestEmail = guestEmail;
        resolved.guestName = guestName;
        resolved.assignee = assignee;
        resolved.tags = tags;
        resolved.createdAt = createdAt;
        resolved.updatedAt = updatedAt;
        resolved.closedAt = closedAt;
        resolved.resolvedAt = resolvedAt;
        return resolved;
    }

    private User createRequesterStub(String email, String requesterName) {
        String name = trim(requesterName);
        String firstName = "Imported";
        String lastName = "User";
        if (StringUtils.hasText(name)) {
            String[] parts = name.split("\\s+", 2);
            firstName = parts[0];
            if (parts.length > 1 && StringUtils.hasText(parts[1])) {
                lastName = parts[1];
            } else {
                lastName = "-";
            }
        } else {
            int at = email.indexOf('@');
            firstName = at > 0 ? email.substring(0, at) : email;
            lastName = "-";
        }
        if (firstName.length() > 50) {
            firstName = firstName.substring(0, 50);
        }
        if (lastName.length() > 50) {
            lastName = lastName.substring(0, 50);
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        passwordPolicyService.applyNewPassword(user, UUID.randomUUID() + "Aa1!");
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        user.setCreateMethod(User.CreateMethod.ADMIN);
        user.setEmailVerified(true);
        user.setCreatorUsername("ticket-import");
        return userRepository.save(user);
    }

    private User resolveMessageAuthor(String authorEmailRaw, Ticket ticket, User agent) {
        String authorEmail = normalizeEmail(authorEmailRaw);
        if (!StringUtils.hasText(authorEmail)) {
            return ticket.getRequester() != null ? ticket.getRequester() : agent;
        }
        return userRepository.findByEmailIgnoreCase(authorEmail).orElse(null);
    }

    private TicketCategory resolveCategory(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            throw new ApiException(ErrorCode.TICKET_CATEGORY_REQUIRED);
        }
        TicketCategory category = ticketCategoryRepository.findByCodeIgnoreCase(value)
                .or(() -> ticketCategoryRepository.findFirstByNameIgnoreCase(value))
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new ApiException(ErrorCode.TICKET_CATEGORY_INACTIVE);
        }
        return category;
    }

    private TicketQueue resolveQueue(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        TicketQueue queue = ticketQueueRepository.findByCodeIgnoreCase(value)
                .or(() -> ticketQueueRepository.findFirstByNameIgnoreCase(value))
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_QUEUE_NOT_FOUND));
        if (!queue.isActive()) {
            throw new ApiException(ErrorCode.TICKET_QUEUE_INACTIVE);
        }
        return queue;
    }

    private TicketPriority parsePriority(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            return TicketPriority.MEDIUM;
        }
        try {
            return TicketPriority.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.TICKET_PRIORITY_REQUIRED);
        }
    }

    private TicketStatus parseStatus(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            return TicketStatus.OPEN;
        }
        try {
            return TicketStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.TICKET_STATUS_INVALID);
        }
    }

    private TicketChannel parseChannel(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            return TicketChannel.EMAIL;
        }
        try {
            TicketChannel channel = TicketChannel.valueOf(value.toUpperCase(Locale.ROOT));
            return channel == TicketChannel.OUTBOUND ? TicketChannel.EMAIL : channel;
        } catch (IllegalArgumentException ex) {
            return TicketChannel.EMAIL;
        }
    }

    private List<String> splitTags(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        String[] parts = value.split("[|;,]");
        List<String> names = new ArrayList<>();
        for (String part : parts) {
            String name = trim(part);
            if (StringUtils.hasText(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private Instant parseInstant(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_INVALID);
        }
    }

    private List<MessageCsvRow> resolveMessagesForTicket(
            TicketCsvRow ticket,
            Map<String, List<MessageCsvRow>> messagesByKey) {
        List<MessageCsvRow> found = new ArrayList<>();
        if (StringUtils.hasText(ticket.externalId)) {
            List<MessageCsvRow> byExternal = messagesByKey.get(key("ext", ticket.externalId));
            if (byExternal != null) {
                found.addAll(byExternal);
            }
        }
        if (StringUtils.hasText(ticket.publicNumber)) {
            List<MessageCsvRow> byPublic = messagesByKey.get(key("pub", ticket.publicNumber));
            if (byPublic != null) {
                found.addAll(byPublic);
            }
        }
        found.sort(Comparator
                .comparingInt((MessageCsvRow m) -> m.seq == null ? Integer.MAX_VALUE : m.seq)
                .thenComparingInt(m -> m.rowNumber));
        return found;
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_REQUIRED);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_INVALID);
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv") && !isCsvContentType(file.getContentType())) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_INVALID);
        }
    }

    private boolean isCsvContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        return lower.contains("csv") || lower.contains("text/plain");
    }

    private List<TicketCsvRow> parseTicketRows(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            Map<String, String> headerMap = normalizeHeaderMap(parser.getHeaderMap());
            requireHeaders(headerMap, "subject", "description");

            List<TicketCsvRow> rows = new ArrayList<>();
            int rowNumber = 1; // header
            for (CSVRecord record : parser) {
                rowNumber++;
                if (isBlankRecord(record)) {
                    continue;
                }
                TicketCsvRow row = new TicketCsvRow();
                row.rowNumber = rowNumber;
                row.externalId = cell(record, headerMap, "external_id", "externalid", "id");
                row.publicNumber = cell(record, headerMap, "public_number", "publicnumber", "ticket_number", "ticketnumber");
                row.subject = cell(record, headerMap, "subject");
                row.description = cell(record, headerMap, "description", "body", "message");
                row.status = cell(record, headerMap, "status");
                row.priority = cell(record, headerMap, "priority");
                row.channel = cell(record, headerMap, "channel");
                row.category = cell(record, headerMap, "category", "category_code", "category_name");
                row.queue = cell(record, headerMap, "queue", "queue_code", "queue_name");
                row.tags = cell(record, headerMap, "tags", "labels");
                row.requesterEmail = cell(record, headerMap, "requester_email", "customer_email", "email");
                row.requesterName = cell(record, headerMap, "requester_name", "customer_name", "name");
                row.guestEmail = cell(record, headerMap, "guest_email");
                row.guestName = cell(record, headerMap, "guest_name");
                row.assigneeEmail = cell(record, headerMap, "assignee_email", "agent_email");
                row.createdAt = cell(record, headerMap, "created_at", "created", "createdat");
                row.updatedAt = cell(record, headerMap, "updated_at", "updated", "updatedat");
                row.closedAt = cell(record, headerMap, "closed_at", "closed");
                row.resolvedAt = cell(record, headerMap, "resolved_at", "resolved");
                rows.add(row);
            }
            return rows;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_INVALID);
        }
    }

    private Map<String, List<MessageCsvRow>> parseMessageRows(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            Map<String, String> headerMap = normalizeHeaderMap(parser.getHeaderMap());
            requireHeaders(headerMap, "body");

            Map<String, List<MessageCsvRow>> byKey = new LinkedHashMap<>();
            int rowNumber = 1;
            int count = 0;
            for (CSVRecord record : parser) {
                rowNumber++;
                if (isBlankRecord(record)) {
                    continue;
                }
                count++;
                if (count > MAX_MESSAGE_ROWS) {
                    throw new ApiException(ErrorCode.TICKET_IMPORT_TOO_MANY_ROWS);
                }
                MessageCsvRow row = new MessageCsvRow();
                row.rowNumber = rowNumber;
                row.externalId = cell(record, headerMap, "external_id", "externalid");
                row.publicNumber = cell(record, headerMap, "public_number", "publicnumber", "ticket_number");
                row.body = cell(record, headerMap, "body", "message", "content");
                row.authorEmail = cell(record, headerMap, "author_email", "email");
                row.internalNote = parseBoolean(cell(record, headerMap, "internal_note", "internal", "private"));
                row.createdAt = parseInstant(cell(record, headerMap, "created_at", "created"));
                String seqRaw = cell(record, headerMap, "seq", "sequence", "order");
                if (StringUtils.hasText(seqRaw)) {
                    try {
                        row.seq = Integer.parseInt(seqRaw.trim());
                    } catch (NumberFormatException ex) {
                        row.seq = null;
                    }
                }
                if (!StringUtils.hasText(row.externalId) && !StringUtils.hasText(row.publicNumber)) {
                    throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_INVALID);
                }
                if (StringUtils.hasText(row.externalId)) {
                    byKey.computeIfAbsent(key("ext", row.externalId), k -> new ArrayList<>()).add(row);
                }
                if (StringUtils.hasText(row.publicNumber)) {
                    byKey.computeIfAbsent(key("pub", row.publicNumber), k -> new ArrayList<>()).add(row);
                }
            }
            return byKey;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_INVALID);
        }
    }

    private Map<String, String> normalizeHeaderMap(Map<String, Integer> headerMap) {
        Map<String, String> normalized = new HashMap<>();
        if (headerMap == null) {
            return normalized;
        }
        for (String header : headerMap.keySet()) {
            if (header == null) {
                continue;
            }
            String key = header.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
            normalized.put(key, header);
        }
        return normalized;
    }

    private void requireHeaders(Map<String, String> headerMap, String... required) {
        for (String req : required) {
            boolean found = false;
            for (String key : headerMap.keySet()) {
                if (key.equals(req) || key.replace("_", "").equals(req.replace("_", ""))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ApiException(ErrorCode.TICKET_IMPORT_FILE_INVALID);
            }
        }
    }

    private String cell(CSVRecord record, Map<String, String> headerMap, String... aliases) {
        for (String alias : aliases) {
            String header = headerMap.get(alias);
            if (header == null) {
                header = headerMap.get(alias.replace("_", ""));
            }
            if (header != null && record.isMapped(header)) {
                try {
                    return record.get(header);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isBlankRecord(CSVRecord record) {
        for (String value : record) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    private boolean parseBoolean(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("1")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("y");
    }

    private String normalizeEmail(String raw) {
        String value = trim(raw);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        value = value.toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new ApiException(ErrorCode.TICKET_GUEST_EMAIL_INVALID);
        }
        return value;
    }

    private String buildPublicNumber(Long id) {
        return String.format(Locale.ROOT, "TCK-%d-%06d", Year.now().getValue(), id);
    }

    private String key(String type, String value) {
        return type + ":" + value.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ApiException findApiException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ApiException api) {
                return api;
            }
            current = current.getCause();
        }
        return null;
    }

    private static final class TicketCsvRow {
        int rowNumber;
        String externalId;
        String publicNumber;
        String subject;
        String description;
        String status;
        String priority;
        String channel;
        String category;
        String queue;
        String tags;
        String requesterEmail;
        String requesterName;
        String guestEmail;
        String guestName;
        String assigneeEmail;
        String createdAt;
        String updatedAt;
        String closedAt;
        String resolvedAt;
    }

    private static final class MessageCsvRow {
        int rowNumber;
        String externalId;
        String publicNumber;
        Integer seq;
        String authorEmail;
        String body;
        boolean internalNote;
        Instant createdAt;
    }

    private static final class ResolvedTicket {
        String subject;
        String description;
        String publicNumber;
        TicketCategory category;
        TicketQueue queue;
        TicketPriority priority;
        TicketStatus status;
        TicketChannel channel;
        User requester;
        String guestEmail;
        String guestName;
        User assignee;
        Set<TicketTag> tags;
        Instant createdAt;
        Instant updatedAt;
        Instant closedAt;
        Instant resolvedAt;
    }
}
