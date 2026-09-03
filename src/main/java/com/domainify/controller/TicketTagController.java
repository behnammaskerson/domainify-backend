package com.domainify.controller;

import com.domainify.dto.TicketTagDto;
import com.domainify.dto.TicketTagRequest;
import com.domainify.service.TicketTagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/ticket-tags")
@PreAuthorize("hasRole('ADMIN')")
public class TicketTagController {

    private final TicketTagService ticketTagService;

    public TicketTagController(TicketTagService ticketTagService) {
        this.ticketTagService = ticketTagService;
    }

    @GetMapping
    public ResponseEntity<List<TicketTagDto>> list() {
        return ResponseEntity.ok(ticketTagService.listAll());
    }

    @PostMapping
    public ResponseEntity<TicketTagDto> create(@Valid @RequestBody TicketTagRequest request) {
        return ResponseEntity.ok(ticketTagService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketTagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
