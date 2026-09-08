package com.domainify.controller;

import com.domainify.dto.PaymentSettingsDto;
import com.domainify.dto.PaymentSettingsUpdateRequest;
import com.domainify.service.PaymentSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/payment-settings")
@PreAuthorize("hasRole('ADMIN')")
public class PaymentSettingsController {

    private final PaymentSettingsService paymentSettingsService;

    public PaymentSettingsController(PaymentSettingsService paymentSettingsService) {
        this.paymentSettingsService = paymentSettingsService;
    }

    @GetMapping
    public ResponseEntity<PaymentSettingsDto> get() {
        return ResponseEntity.ok(paymentSettingsService.getDto());
    }

    @PutMapping
    public ResponseEntity<PaymentSettingsDto> update(@Valid @RequestBody PaymentSettingsUpdateRequest request) {
        return ResponseEntity.ok(paymentSettingsService.update(request));
    }
}
