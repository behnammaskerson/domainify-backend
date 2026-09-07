package com.domainify.controller;

import com.domainify.dto.DomainListingDto;
import com.domainify.dto.PagedResponse;
import com.domainify.entity.User;
import com.domainify.service.DomainListingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/marketplace/listings")
public class MarketplaceListingController {

    private final DomainListingService domainListingService;

    public MarketplaceListingController(DomainListingService domainListingService) {
        this.domainListingService = domainListingService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DomainListingDto>> browse(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "priceMin", required = false) BigDecimal priceMin,
            @RequestParam(value = "priceMax", required = false) BigDecimal priceMax,
            @RequestParam(value = "featured", required = false) Boolean featured,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(domainListingService.browseMarketplace(
                q, categoryId, priceMin, priceMax, featured, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DomainListingDto> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(domainListingService.getPublic(id));
    }
}
