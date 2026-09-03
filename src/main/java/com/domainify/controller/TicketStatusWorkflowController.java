package com.domainify.controller;

import com.domainify.dto.TicketStatusWorkflowDto;
import com.domainify.service.TicketStatusWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ticket-status-workflow")
@PreAuthorize("hasRole('ADMIN')")
public class TicketStatusWorkflowController {

    private final TicketStatusWorkflowService workflowService;

    public TicketStatusWorkflowController(TicketStatusWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public ResponseEntity<TicketStatusWorkflowDto> get() {
        return ResponseEntity.ok(workflowService.getWorkflow());
    }

    @PutMapping
    public ResponseEntity<TicketStatusWorkflowDto> save(@Valid @RequestBody TicketStatusWorkflowDto request) {
        return ResponseEntity.ok(workflowService.saveWorkflow(request));
    }
}
