package com.domainify.controller;

import com.domainify.dto.DomainRenewalTestRequest;
import com.domainify.dto.DomainRenewalTestResultDto;
import com.domainify.dto.DomainSettingsDto;
import com.domainify.dto.DomainSettingsUpdateRequest;
import com.domainify.entity.User;
import com.domainify.service.DomainRenewalReminderService;
import com.domainify.service.DomainSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/domain-settings")
@PreAuthorize("hasRole('ADMIN')")
public class DomainSettingsController {

    private final DomainSettingsService domainSettingsService;
    private final DomainRenewalReminderService domainRenewalReminderService;

    public DomainSettingsController(
            DomainSettingsService domainSettingsService,
            DomainRenewalReminderService domainRenewalReminderService) {
        this.domainSettingsService = domainSettingsService;
        this.domainRenewalReminderService = domainRenewalReminderService;
    }

    @GetMapping
    public ResponseEntity<DomainSettingsDto> get() {
        return ResponseEntity.ok(domainSettingsService.getDto());
    }

    @PutMapping
    public ResponseEntity<DomainSettingsDto> update(@Valid @RequestBody DomainSettingsUpdateRequest request) {
        return ResponseEntity.ok(domainSettingsService.update(request));
    }

    @PostMapping("/test-renewal")
    public ResponseEntity<DomainRenewalTestResultDto> testRenewal(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DomainRenewalTestRequest request) {
        return ResponseEntity.ok(domainRenewalReminderService.sendTest(user, request));
    }
}
