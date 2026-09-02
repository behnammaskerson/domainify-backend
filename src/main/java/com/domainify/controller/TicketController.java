package com.domainify.controller;

import com.domainify.dto.TicketCategoryDto;
import com.domainify.dto.TicketDto;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.User;
import com.domainify.service.TicketCategoryService;
import com.domainify.service.TicketService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketCategoryService ticketCategoryService;

    public TicketController(TicketService ticketService, TicketCategoryService ticketCategoryService) {
        this.ticketService = ticketService;
        this.ticketCategoryService = ticketCategoryService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<TicketCategoryDto>> listActiveCategories() {
        return ResponseEntity.ok(ticketCategoryService.listActive());
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
