package com.domainify.controller;

import com.domainify.dto.EmailConfigDto;
import com.domainify.dto.EmailConfigUpdateRequest;
import com.domainify.dto.EmailTestRequest;
import com.domainify.dto.EmailTestResultDto;
import com.domainify.service.EmailConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/email-config")
@PreAuthorize("hasRole('ADMIN')")
public class EmailConfigController {

    private final EmailConfigService emailConfigService;

    public EmailConfigController(EmailConfigService emailConfigService) {
        this.emailConfigService = emailConfigService;
    }

    @GetMapping
    public ResponseEntity<EmailConfigDto> get() {
        return ResponseEntity.ok(emailConfigService.getDto());
    }

    @PutMapping
    public ResponseEntity<EmailConfigDto> update(@Valid @RequestBody EmailConfigUpdateRequest request) {
        return ResponseEntity.ok(emailConfigService.update(request));
    }

    @PostMapping("/test")
    public ResponseEntity<EmailTestResultDto> test(@Valid @RequestBody EmailTestRequest request) {
        return ResponseEntity.ok(emailConfigService.sendTestEmail(request.getTo()));
    }
}
