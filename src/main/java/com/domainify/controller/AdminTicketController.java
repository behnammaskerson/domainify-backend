package com.domainify.controller;

import com.domainify.dto.LinkTicketsRequest;
import com.domainify.dto.MergeTicketRequest;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.SplitTicketRequest;
import com.domainify.dto.SplitTicketResultDto;
import com.domainify.dto.TicketAssigneeOptionDto;
import com.domainify.dto.TicketDetailDto;
import com.domainify.dto.TicketDto;
import com.domainify.dto.TicketInboxFilter;
import com.domainify.dto.TicketTagDto;
import com.domainify.dto.UpdateTicketDueDateRequest;
import com.domainify.dto.UpdateTicketStatusRequest;
import com.domainify.dto.UpdateTicketTagsRequest;
import com.domainify.entity.TicketInboxView;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.User;
import com.domainify.service.AdminTicketService;
import com.domainify.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/admin/tickets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTicketController {

    private final AdminTicketService adminTicketService;
    private final TicketService ticketService;

    public AdminTicketController(AdminTicketService adminTicketService, TicketService ticketService) {
        this.adminTicketService = adminTicketService;
        this.ticketService = ticketService;
    }

    @GetMapping("/inbox")
    public ResponseEntity<PagedResponse<TicketDto>> inbox(
            @AuthenticationPrincipal User agent,
            @RequestParam(value = "view", defaultValue = "ALL") TicketInboxView view,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) TicketStatus status,
            @RequestParam(value = "priority", required = false) TicketPriority priority,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "assigneeId", required = false) Long assigneeId,
            @RequestParam(value = "unassigned", required = false) Boolean unassigned,
            @RequestParam(value = "createdFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(value = "createdTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "customer", required = false) String customer,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ticketService.autoArchiveClosedTickets();
        TicketInboxFilter filter = new TicketInboxFilter();
        filter.setStatus(status);
        filter.setPriority(priority);
        filter.setCategoryId(categoryId);
        filter.setAssigneeId(assigneeId);
        filter.setUnassignedOnly(Boolean.TRUE.equals(unassigned));
        filter.setCreatedFrom(createdFrom);
        filter.setCreatedTo(createdTo);
        filter.setTagId(tagId);
        filter.setCustomer(customer);
        return ResponseEntity.ok(adminTicketService.listInbox(agent, view, q, filter, pageable));
    }

    @GetMapping("/assignees")
    public ResponseEntity<List<TicketAssigneeOptionDto>> assignees() {
        return ResponseEntity.ok(adminTicketService.listAssignees());
    }

    @GetMapping("/tags")
    public ResponseEntity<List<TicketTagDto>> tags() {
        return ResponseEntity.ok(adminTicketService.listTags());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDetailDto> get(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.getForStaff(agent, id));
    }

    @PostMapping(value = "/{id}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketDetailDto> reply(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @RequestParam("body") String body,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(ticketService.replyAsStaff(agent, id, body, attachments));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketDetailDto> updateStatus(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ResponseEntity.ok(ticketService.updateStatusAsStaff(agent, id, request.getStatus()));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<TicketDetailDto> close(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.closeAsStaff(agent, id));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<TicketDetailDto> reopen(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.reopenAsStaff(agent, id));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<TicketDetailDto> archive(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.archiveAsStaff(agent, id));
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<TicketDetailDto> unarchive(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.unarchiveAsStaff(agent, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TicketDetailDto> softDelete(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.softDeleteAsStaff(agent, id));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<TicketDetailDto> restore(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.restoreAsStaff(agent, id));
    }

    @PostMapping("/{id}/merge")
    public ResponseEntity<TicketDetailDto> merge(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @RequestBody MergeTicketRequest request) {
        return ResponseEntity.ok(ticketService.mergeAsStaff(agent, id, request));
    }

    @PostMapping("/{id}/split")
    public ResponseEntity<SplitTicketResultDto> split(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @Valid @RequestBody SplitTicketRequest request) {
        return ResponseEntity.ok(ticketService.splitAsStaff(agent, id, request));
    }

    @PostMapping("/{id}/related")
    public ResponseEntity<TicketDetailDto> linkRelated(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @RequestBody LinkTicketsRequest request) {
        return ResponseEntity.ok(ticketService.linkRelatedAsStaff(agent, id, request));
    }

    @DeleteMapping("/{id}/related/{relatedId}")
    public ResponseEntity<TicketDetailDto> unlinkRelated(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @PathVariable("relatedId") Long relatedId) {
        return ResponseEntity.ok(ticketService.unlinkRelatedAsStaff(agent, id, relatedId));
    }

    @PatchMapping("/{id}/due-date")
    public ResponseEntity<TicketDetailDto> updateDueDate(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @RequestBody UpdateTicketDueDateRequest request) {
        return ResponseEntity.ok(ticketService.updateDueDateAsStaff(agent, id, request));
    }

    @PutMapping("/{id}/tags")
    public ResponseEntity<TicketDetailDto> updateTags(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @RequestBody UpdateTicketTagsRequest request) {
        return ResponseEntity.ok(ticketService.updateTagsAsStaff(agent, id, request));
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadTicketAttachment(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @PathVariable("attachmentId") Long attachmentId) {
        return ticketService.downloadTicketAttachmentForStaff(agent, id, attachmentId);
    }

    @GetMapping("/{id}/messages/{messageId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadMessageAttachment(
            @AuthenticationPrincipal User agent,
            @PathVariable("id") Long id,
            @PathVariable("messageId") Long messageId,
            @PathVariable("attachmentId") Long attachmentId) {
        return ticketService.downloadMessageAttachmentForStaff(agent, id, messageId, attachmentId);
    }
}
