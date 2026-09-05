package com.domainify.controller;

import com.domainify.dto.DomainDto;
import com.domainify.dto.DomainStatusCountsDto;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.UpsertDomainRequest;
import com.domainify.entity.DomainStatus;
import com.domainify.entity.User;
import com.domainify.service.DomainService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DomainDto>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) DomainStatus status,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "priceMin", required = false) BigDecimal priceMin,
            @RequestParam(value = "priceMax", required = false) BigDecimal priceMax,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(domainService.list(user, q, status, categoryId, priceMin, priceMax, pageable));
    }

    @GetMapping("/status-counts")
    public ResponseEntity<DomainStatusCountsDto> statusCounts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(domainService.statusCounts(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DomainDto> get(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(domainService.get(user, id));
    }

    @PostMapping
    public ResponseEntity<DomainDto> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpsertDomainRequest request) {
        return ResponseEntity.ok(domainService.create(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DomainDto> update(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpsertDomainRequest request) {
        return ResponseEntity.ok(domainService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id) {
        domainService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
