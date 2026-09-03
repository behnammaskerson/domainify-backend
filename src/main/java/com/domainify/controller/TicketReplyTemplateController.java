package com.domainify.controller;

import com.domainify.dto.TicketReplyTemplateDto;
import com.domainify.dto.TicketReplyTemplateRequest;
import com.domainify.service.TicketReplyTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/ticket-reply-templates")
@PreAuthorize("hasRole('ADMIN')")
public class TicketReplyTemplateController {

    private final TicketReplyTemplateService ticketReplyTemplateService;

    public TicketReplyTemplateController(TicketReplyTemplateService ticketReplyTemplateService) {
        this.ticketReplyTemplateService = ticketReplyTemplateService;
    }

    @GetMapping
    public ResponseEntity<List<TicketReplyTemplateDto>> list() {
        return ResponseEntity.ok(ticketReplyTemplateService.listAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TicketReplyTemplateDto>> listActive() {
        return ResponseEntity.ok(ticketReplyTemplateService.listActive());
    }

    @PostMapping
    public ResponseEntity<TicketReplyTemplateDto> create(@Valid @RequestBody TicketReplyTemplateRequest request) {
        return ResponseEntity.ok(ticketReplyTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketReplyTemplateDto> update(
            @PathVariable Long id,
            @Valid @RequestBody TicketReplyTemplateRequest request) {
        return ResponseEntity.ok(ticketReplyTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketReplyTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
