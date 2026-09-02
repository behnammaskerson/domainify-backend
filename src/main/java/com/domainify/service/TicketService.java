package com.domainify.service;

import com.domainify.dto.TicketAttachmentDto;
import com.domainify.dto.TicketDto;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketAttachment;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TicketService {

    private static final int SUBJECT_MAX = 200;
    private static final int DESCRIPTION_MAX = 10000;
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

    private final TicketRepository ticketRepository;
    private final TicketCategoryService ticketCategoryService;

    public TicketService(TicketRepository ticketRepository, TicketCategoryService ticketCategoryService) {
        this.ticketRepository = ticketRepository;
        this.ticketCategoryService = ticketCategoryService;
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
        ticket.setRequester(requester);
        ticket.setPublicNumber("TMP-" + System.nanoTime());

        for (MultipartFile file : files) {
            ticket.addAttachment(toAttachment(file));
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

    private TicketAttachment toAttachment(MultipartFile file) {
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_UPLOAD_FAILED);
        }
        if (bytes.length == 0 || bytes.length > MAX_ATTACHMENT_BYTES) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }

        String originalName = file.getOriginalFilename();
        String fileName = StringUtils.hasText(originalName) ? originalName.trim() : "attachment";
        if (fileName.length() > 255) {
            fileName = fileName.substring(fileName.length() - 255);
        }

        TicketAttachment attachment = new TicketAttachment();
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setSizeBytes(bytes.length);
        attachment.setData(bytes);
        return attachment;
    }

    private String buildPublicNumber(Long id) {
        return String.format(Locale.ROOT, "TCK-%d-%06d", Year.now().getValue(), id);
    }

    private TicketDto toDto(Ticket ticket) {
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
        if (ticket.getRequester() != null) {
            dto.setRequesterId(ticket.getRequester().getId());
            dto.setRequesterEmail(ticket.getRequester().getEmail());
        }
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());

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
}
