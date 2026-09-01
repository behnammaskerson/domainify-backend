package com.domainify.controller;

import com.domainify.dto.SmsConfigDto;
import com.domainify.dto.SmsConfigUpdateRequest;
import com.domainify.dto.SmsCreditResultDto;
import com.domainify.dto.SmsDefaultLineRequest;
import com.domainify.dto.SmsLinesResultDto;
import com.domainify.service.SmsConfigService;
import com.domainify.service.SmsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/sms-config")
@PreAuthorize("hasRole('ADMIN')")
public class SmsConfigController {

    private final SmsConfigService smsConfigService;
    private final SmsService smsService;

    public SmsConfigController(SmsConfigService smsConfigService, SmsService smsService) {
        this.smsConfigService = smsConfigService;
        this.smsService = smsService;
    }

    @GetMapping
    public ResponseEntity<SmsConfigDto> get() {
        return ResponseEntity.ok(smsConfigService.getDto());
    }

    @PutMapping
    public ResponseEntity<SmsConfigDto> update(@Valid @RequestBody SmsConfigUpdateRequest request) {
        return ResponseEntity.ok(smsConfigService.update(request));
    }

    @GetMapping("/credit")
    public ResponseEntity<SmsCreditResultDto> credit() {
        return ResponseEntity.ok(smsService.fetchCredit());
    }

    @GetMapping("/lines")
    public ResponseEntity<SmsLinesResultDto> lines() {
        return ResponseEntity.ok(smsService.fetchLines());
    }

    @PutMapping("/default-line")
    public ResponseEntity<SmsConfigDto> setDefaultLine(@Valid @RequestBody SmsDefaultLineRequest request) {
        return ResponseEntity.ok(smsConfigService.updateDefaultLine(request.getDefaultLine()));
    }
}
