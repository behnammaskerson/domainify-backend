package com.domainify.controller;

import com.domainify.dto.TicketQueueDto;
import com.domainify.dto.TicketQueueRequest;
import com.domainify.dto.UpdateCategoryAgentsRequest;
import com.domainify.service.TicketQueueService;
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
@RequestMapping("/admin/ticket-queues")
@PreAuthorize("hasRole('ADMIN')")
public class TicketQueueController {

    private final TicketQueueService ticketQueueService;

    public TicketQueueController(TicketQueueService ticketQueueService) {
        this.ticketQueueService = ticketQueueService;
    }

    @GetMapping
    public ResponseEntity<List<TicketQueueDto>> listAll() {
        return ResponseEntity.ok(ticketQueueService.listAll());
    }

    @PostMapping
    public ResponseEntity<TicketQueueDto> create(@Valid @RequestBody TicketQueueRequest request) {
        return ResponseEntity.ok(ticketQueueService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketQueueDto> update(
            @PathVariable Long id,
            @Valid @RequestBody TicketQueueRequest request) {
        return ResponseEntity.ok(ticketQueueService.update(id, request));
    }

    @PutMapping("/{id}/agents")
    public ResponseEntity<TicketQueueDto> updateAgents(
            @PathVariable Long id,
            @RequestBody UpdateCategoryAgentsRequest request) {
        return ResponseEntity.ok(ticketQueueService.updateQueueAgents(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketQueueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
