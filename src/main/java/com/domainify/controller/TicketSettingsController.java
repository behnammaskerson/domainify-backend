package com.domainify.controller;

import com.domainify.dto.AgentDigestRunResultDto;
import com.domainify.dto.TicketSettingsDto;
import com.domainify.service.TicketAgentDigestService;
import com.domainify.service.TicketSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ticket-settings")
@PreAuthorize("hasRole('ADMIN')")
public class TicketSettingsController {

    private final TicketSettingsService ticketSettingsService;
    private final TicketAgentDigestService ticketAgentDigestService;

    public TicketSettingsController(
            TicketSettingsService ticketSettingsService,
            TicketAgentDigestService ticketAgentDigestService) {
        this.ticketSettingsService = ticketSettingsService;
        this.ticketAgentDigestService = ticketAgentDigestService;
    }

    @GetMapping
    public ResponseEntity<TicketSettingsDto> get() {
        return ResponseEntity.ok(ticketSettingsService.getDto());
    }

    @PutMapping
    public ResponseEntity<TicketSettingsDto> update(@Valid @RequestBody TicketSettingsDto request) {
        return ResponseEntity.ok(ticketSettingsService.update(request));
    }

    /** Manually send today's agent digest emails (does not change last-run date). */
    @PostMapping("/digest/run-now")
    public ResponseEntity<AgentDigestRunResultDto> runAgentDigestNow() {
        int sent = ticketAgentDigestService.processDailyDigest();
        return ResponseEntity.ok(new AgentDigestRunResultDto(sent));
    }
}
