package com.domainify.controller;

import com.domainify.dto.SmsBulkSendRequest;
import com.domainify.dto.SmsBulkSendResultDto;
import com.domainify.dto.SmsScheduledCancelResultDto;
import com.domainify.dto.SmsScheduledPagedResponse;
import com.domainify.entity.ScheduledSmsSourceType;
import com.domainify.entity.ScheduledSmsStatus;
import com.domainify.service.ScheduledSmsService;
import com.domainify.service.SmsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/admin/sms")
@PreAuthorize("hasRole('ADMIN')")
public class SmsController {

    private final SmsService smsService;
    private final ScheduledSmsService scheduledSmsService;

    public SmsController(SmsService smsService, ScheduledSmsService scheduledSmsService) {
        this.smsService = smsService;
        this.scheduledSmsService = scheduledSmsService;
    }

    @PostMapping("/send/bulk")
    public ResponseEntity<SmsBulkSendResultDto> sendBulk(@Valid @RequestBody SmsBulkSendRequest request) {
        return ResponseEntity.ok(smsService.sendBulk(request));
    }

    @GetMapping("/scheduled")
    public ResponseEntity<SmsScheduledPagedResponse> listScheduled(
            @RequestParam(required = false) ScheduledSmsStatus status,
            @RequestParam(required = false) ScheduledSmsSourceType sourceType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledTo,
            @PageableDefault(size = 10, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(scheduledSmsService.listScheduled(
                status, sourceType, search, scheduledFrom, scheduledTo, pageable));
    }

    @DeleteMapping("/scheduled/{packId}")
    public ResponseEntity<SmsScheduledCancelResultDto> cancelScheduled(@PathVariable String packId) {
        return ResponseEntity.ok(scheduledSmsService.cancelScheduled(packId));
    }

    @DeleteMapping("/scheduled/{packId}/record")
    public ResponseEntity<Void> removeScheduledRecord(@PathVariable String packId) {
        scheduledSmsService.removeScheduledRecord(packId);
        return ResponseEntity.noContent().build();
    }
}
