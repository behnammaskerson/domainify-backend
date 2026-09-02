package com.domainify.controller;

import com.domainify.dto.SmsArchiveSendResultDto;
import com.domainify.dto.SmsBulkSendRequest;
import com.domainify.dto.SmsBulkSendResultDto;
import com.domainify.dto.SmsDailyPackResultDto;
import com.domainify.dto.SmsLiveSendResultDto;
import com.domainify.dto.SmsPackReportResultDto;
import com.domainify.dto.SmsReceiveLatestResultDto;
import com.domainify.dto.SmsReceivePagedResultDto;
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

    @GetMapping("/send/live")
    public ResponseEntity<SmsLiveSendResultDto> listLiveSends(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer pageNumber) {
        return ResponseEntity.ok(smsService.fetchLiveSends(pageSize, pageNumber));
    }

    @GetMapping("/send/archive")
    public ResponseEntity<SmsArchiveSendResultDto> listArchiveSends(
            @RequestParam(required = false) Long fromDate,
            @RequestParam(required = false) Long toDate,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer pageNumber) {
        return ResponseEntity.ok(smsService.fetchArchiveSends(fromDate, toDate, pageSize, pageNumber));
    }

    @GetMapping("/send/pack")
    public ResponseEntity<SmsDailyPackResultDto> listDailyPacks(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer pageNumber) {
        return ResponseEntity.ok(smsService.fetchDailyPacks(pageSize, pageNumber));
    }

    @GetMapping("/send/pack/{packId}")
    public ResponseEntity<SmsPackReportResultDto> getPackReport(@PathVariable String packId) {
        return ResponseEntity.ok(smsService.fetchPackReport(packId));
    }

    @GetMapping("/receive/latest")
    public ResponseEntity<SmsReceiveLatestResultDto> listLatestReceived(
            @RequestParam(required = false) Integer count) {
        return ResponseEntity.ok(smsService.fetchLatestReceived(count));
    }

    @GetMapping("/receive/live")
    public ResponseEntity<SmsReceivePagedResultDto> listLiveReceived(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Boolean sortByNewest,
            @RequestParam(required = false) String mobile) {
        return ResponseEntity.ok(smsService.fetchLiveReceived(pageSize, pageNumber, sortByNewest, mobile));
    }

    @GetMapping("/receive/archive")
    public ResponseEntity<SmsReceivePagedResultDto> listArchiveReceived(
            @RequestParam(required = false) Long fromDate,
            @RequestParam(required = false) Long toDate,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) String mobile) {
        return ResponseEntity.ok(smsService.fetchArchiveReceived(fromDate, toDate, pageSize, pageNumber, mobile));
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
