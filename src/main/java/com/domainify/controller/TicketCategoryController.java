package com.domainify.controller;

import com.domainify.dto.TicketCategoryDto;
import com.domainify.dto.TicketCategoryRequest;
import com.domainify.service.TicketCategoryService;
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
@RequestMapping("/admin/ticket-categories")
@PreAuthorize("hasRole('ADMIN')")
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    public TicketCategoryController(TicketCategoryService ticketCategoryService) {
        this.ticketCategoryService = ticketCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<TicketCategoryDto>> listAll() {
        return ResponseEntity.ok(ticketCategoryService.listAll());
    }

    @PostMapping
    public ResponseEntity<TicketCategoryDto> create(@Valid @RequestBody TicketCategoryRequest request) {
        return ResponseEntity.ok(ticketCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketCategoryDto> update(
            @PathVariable Long id,
            @Valid @RequestBody TicketCategoryRequest request) {
        return ResponseEntity.ok(ticketCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
