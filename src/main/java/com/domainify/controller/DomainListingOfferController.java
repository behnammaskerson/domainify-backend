package com.domainify.controller;

import com.domainify.dto.AcceptOfferResponse;
import com.domainify.dto.CreateOrCounterOfferRequest;
import com.domainify.dto.ListingOfferDto;
import com.domainify.dto.PagedResponse;
import com.domainify.entity.DomainListingOfferStatus;
import com.domainify.entity.User;
import com.domainify.service.DomainListingOfferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class DomainListingOfferController {

    private final DomainListingOfferService offerService;

    public DomainListingOfferController(DomainListingOfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/marketplace/listings/{listingId}/offers")
    public ResponseEntity<ListingOfferDto> create(
            @AuthenticationPrincipal User user,
            @PathVariable Long listingId,
            @Valid @RequestBody CreateOrCounterOfferRequest request) {
        return ResponseEntity.ok(offerService.create(user, listingId, request));
    }

    @GetMapping("/marketplace/offers/mine")
    public ResponseEntity<PagedResponse<ListingOfferDto>> listMine(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "status", required = false) DomainListingOfferStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(offerService.listMine(user, status, pageable));
    }

    @GetMapping("/listings/{listingId}/offers")
    public ResponseEntity<PagedResponse<ListingOfferDto>> listForListing(
            @AuthenticationPrincipal User user,
            @PathVariable Long listingId,
            @RequestParam(value = "status", required = false) DomainListingOfferStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(offerService.listForListing(user, listingId, status, pageable));
    }

    @GetMapping("/offers/{id}")
    public ResponseEntity<ListingOfferDto> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(offerService.get(user, id));
    }

    @PostMapping("/offers/{id}/counter")
    public ResponseEntity<ListingOfferDto> counter(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody CreateOrCounterOfferRequest request) {
        return ResponseEntity.ok(offerService.counter(user, id, request));
    }

    @PostMapping("/offers/{id}/accept")
    public ResponseEntity<AcceptOfferResponse> accept(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(offerService.accept(user, id));
    }

    @PostMapping("/offers/{id}/reject")
    public ResponseEntity<ListingOfferDto> reject(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(offerService.reject(user, id));
    }

    @PostMapping("/offers/{id}/withdraw")
    public ResponseEntity<ListingOfferDto> withdraw(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(offerService.withdraw(user, id));
    }
}
