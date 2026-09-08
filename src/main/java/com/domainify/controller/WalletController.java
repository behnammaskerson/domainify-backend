package com.domainify.controller;

import com.domainify.dto.WalletDto;
import com.domainify.dto.WalletTopUpRequest;
import com.domainify.dto.WalletTopUpResponse;
import com.domainify.entity.User;
import com.domainify.service.PaymentIntentService;
import com.domainify.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final WalletService walletService;
    private final PaymentIntentService paymentIntentService;

    public WalletController(WalletService walletService, PaymentIntentService paymentIntentService) {
        this.walletService = walletService;
        this.paymentIntentService = paymentIntentService;
    }

    @GetMapping
    public ResponseEntity<WalletDto> get(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "ledgerLimit", defaultValue = "20") int ledgerLimit) {
        return ResponseEntity.ok(walletService.getWalletDto(user, Math.min(Math.max(ledgerLimit, 1), 100)));
    }

    @PostMapping("/top-up")
    public ResponseEntity<WalletTopUpResponse> topUp(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WalletTopUpRequest request) {
        return ResponseEntity.ok(paymentIntentService.startWalletTopUp(user, request.getAmountIrt()));
    }
}
