package com.domainify.controller;

import com.domainify.dto.PagedResponse;
import com.domainify.dto.TicketAttachmentPolicyDto;
import com.domainify.dto.TicketCategoryDto;
import com.domainify.dto.SaveTicketReplyDraftRequest;
import com.domainify.dto.TicketDetailDto;
import com.domainify.dto.TicketReplyDraftDto;
import com.domainify.dto.TicketDto;
import com.domainify.dto.UpdateTicketMessageRequest;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.User;
import com.domainify.service.TicketCategoryService;
import com.domainify.service.TicketService;
import com.domainify.service.TicketSettingsService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketCategoryService ticketCategoryService;
    private final TicketSettingsService ticketSettingsService;

    public TicketController(
            TicketService ticketService,
            TicketCategoryService ticketCategoryService,
            TicketSettingsService ticketSettingsService) {
        this.ticketService = ticketService;
        this.ticketCategoryService = ticketCategoryService;
        this.ticketSettingsService = ticketSettingsService;
    }

    @GetMapping("/attachment-policy")
    public ResponseEntity<TicketAttachmentPolicyDto> attachmentPolicy() {
        return ResponseEntity.ok(ticketSettingsService.getAttachmentPolicy());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<TicketCategoryDto>> listActiveCategories() {
        return ResponseEntity.ok(ticketCategoryService.listActive());
    }

    @GetMapping("/mine")
    public ResponseEntity<PagedResponse<TicketDto>> listMine(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "status", required = false) TicketStatus status,
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ticketService.listMine(user, status, q, pageable));
    }

    @GetMapping("/mine/{id}")
    public ResponseEntity<TicketDetailDto> getMine(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.getMine(user, id));
    }

    @PutMapping("/mine/{id}/reply-draft")
    public ResponseEntity<TicketReplyDraftDto> saveReplyDraft(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @RequestBody SaveTicketReplyDraftRequest request) {
        TicketReplyDraftDto draft = ticketService.saveReplyDraft(user, id, request);
        return draft == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(draft);
    }

    @PostMapping(value = "/mine/{id}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketDetailDto> reply(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @RequestParam("body") String body,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(ticketService.reply(user, id, body, attachments));
    }

    @PatchMapping("/mine/{id}/messages/{messageId}")
    public ResponseEntity<TicketDetailDto> editMessage(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @PathVariable("messageId") Long messageId,
            @Valid @RequestBody UpdateTicketMessageRequest request) {
        return ResponseEntity.ok(ticketService.editMessage(user, id, messageId, request, false));
    }

    @DeleteMapping("/mine/{id}/messages/{messageId}")
    public ResponseEntity<TicketDetailDto> deleteMessage(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @PathVariable("messageId") Long messageId) {
        return ResponseEntity.ok(ticketService.deleteMessage(user, id, messageId, false));
    }

    @PatchMapping("/mine/{id}/description")
    public ResponseEntity<TicketDetailDto> editDescription(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTicketMessageRequest request) {
        return ResponseEntity.ok(ticketService.editDescription(user, id, request.getBody(), false));
    }

    @PostMapping("/mine/{id}/close")
    public ResponseEntity<TicketDetailDto> closeMine(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.closeMine(user, id));
    }

    @PostMapping("/mine/{id}/reopen")
    public ResponseEntity<TicketDetailDto> reopenMine(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.reopenMine(user, id));
    }

    @GetMapping("/mine/{id}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadTicketAttachment(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @PathVariable("attachmentId") Long attachmentId) {
        return ticketService.downloadTicketAttachment(user, id, attachmentId);
    }

    @GetMapping("/mine/{id}/messages/{messageId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadMessageAttachment(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @PathVariable("messageId") Long messageId,
            @PathVariable("attachmentId") Long attachmentId) {
        return ticketService.downloadMessageAttachment(user, id, messageId, attachmentId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketDto> create(
            @AuthenticationPrincipal User user,
            @RequestParam("subject") String subject,
            @RequestParam("description") String description,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("priority") TicketPriority priority,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(ticketService.create(user, subject, description, categoryId, priority, attachments));
    }
}
