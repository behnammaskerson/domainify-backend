package com.domainify.controller;

import com.domainify.dto.GuestTicketLookupRequest;
import com.domainify.dto.GuestTicketPublicConfigDto;
import com.domainify.dto.GuestTicketSummaryDto;
import com.domainify.dto.TicketAttachmentPolicyDto;
import com.domainify.dto.TicketCategoryDto;
import com.domainify.dto.TicketDetailDto;
import com.domainify.entity.TicketPriority;
import com.domainify.service.GuestTicketService;
import com.domainify.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/tickets")
public class PublicTicketController {

    private final GuestTicketService guestTicketService;
    private final MessageService messageService;

    public PublicTicketController(GuestTicketService guestTicketService, MessageService messageService) {
        this.guestTicketService = guestTicketService;
        this.messageService = messageService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<TicketCategoryDto>> categories() {
        return ResponseEntity.ok(guestTicketService.listCategories());
    }

    @GetMapping("/attachment-policy")
    public ResponseEntity<TicketAttachmentPolicyDto> attachmentPolicy() {
        return ResponseEntity.ok(guestTicketService.attachmentPolicy());
    }

    @GetMapping("/config")
    public ResponseEntity<GuestTicketPublicConfigDto> config() {
        return ResponseEntity.ok(guestTicketService.publicConfig());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> create(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String description,
            @RequestParam Long categoryId,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) MultipartFile[] attachments,
            @RequestParam(required = false) String captchaToken,
            @RequestParam(required = false) String captchaAnswer,
            HttpServletRequest request) {
        guestTicketService.createFromWeb(
                name, email, subject, description, categoryId, priority, attachments, 
                captchaToken, captchaAnswer, request);
        return ResponseEntity.ok(Map.of("message", messageService.get("ticket.guest.submitted")));
    }

    @PostMapping("/lookup")
    public ResponseEntity<Map<String, String>> lookup(
            @Valid @RequestBody GuestTicketLookupRequest request,
            HttpServletRequest httpRequest) {
        guestTicketService.requestListByEmail(request, httpRequest);
        return ResponseEntity.ok(Map.of("message", messageService.get("ticket.guest.list_emailed")));
    }

    @GetMapping("/access/{token}/mine")
    public ResponseEntity<List<GuestTicketSummaryDto>> mine(@PathVariable String token) {
        return ResponseEntity.ok(guestTicketService.listByAccessToken(token));
    }

    @PostMapping("/access/{token}/mine/{ticketId}/open")
    public ResponseEntity<GuestTicketSummaryDto> openRelated(
            @PathVariable String token,
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(guestTicketService.openRelatedTicket(token, ticketId));
    }

    @GetMapping("/verify")
    public ResponseEntity<TicketDetailDto> verifyGet(@RequestParam String token) {
        return ResponseEntity.ok(guestTicketService.verifyAndOpen(token));
    }

    @PostMapping("/verify")
    public ResponseEntity<TicketDetailDto> verifyPost(@RequestParam String token) {
        return ResponseEntity.ok(guestTicketService.verifyAndOpen(token));
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<TicketDetailDto> access(@PathVariable String token) {
        return ResponseEntity.ok(guestTicketService.getByToken(token));
    }

    @PostMapping(value = "/access/{token}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketDetailDto> reply(
            @PathVariable String token,
            @RequestParam String body,
            @RequestParam(required = false) MultipartFile[] attachments) {
        return ResponseEntity.ok(guestTicketService.reply(token, body, attachments));
    }
}
