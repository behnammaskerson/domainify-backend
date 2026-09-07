package com.domainify.controller;

import com.domainify.dto.DomainListingDto;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.UpsertDomainListingRequest;
import com.domainify.entity.DomainListingStatus;
import com.domainify.entity.User;
import com.domainify.service.DomainListingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listings")
public class DomainListingController {

    private final DomainListingService domainListingService;

    public DomainListingController(DomainListingService domainListingService) {
        this.domainListingService = domainListingService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DomainListingDto>> listMine(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) DomainListingStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(domainListingService.listMine(user, q, status, pageable));
    }

    @GetMapping("/by-domain/{domainId}")
    public ResponseEntity<DomainListingDto> getByDomain(
            @AuthenticationPrincipal User user,
            @PathVariable Long domainId) {
        return domainListingService.findMineByDomainId(user, domainId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DomainListingDto> getMine(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(domainListingService.getMine(user, id));
    }

    @PostMapping
    public ResponseEntity<DomainListingDto> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpsertDomainListingRequest request) {
        return ResponseEntity.ok(domainListingService.create(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DomainListingDto> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody UpsertDomainListingRequest request) {
        return ResponseEntity.ok(domainListingService.update(user, id, request));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<DomainListingDto> deactivate(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(domainListingService.deactivate(user, id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<DomainListingDto> activate(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(domainListingService.activate(user, id));
    }
}
