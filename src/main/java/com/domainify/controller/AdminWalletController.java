package com.domainify.controller;

import com.domainify.dto.AdminWalletAdjustRequest;
import com.domainify.dto.WalletDto;
import com.domainify.entity.User;
import com.domainify.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/wallets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {

    private final WalletService walletService;

    public AdminWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletDto> getUserWallet(
            @PathVariable Long userId,
            @RequestParam(value = "ledgerLimit", defaultValue = "20") int ledgerLimit) {
        return ResponseEntity.ok(walletService.getWalletDtoForUserId(userId, Math.min(Math.max(ledgerLimit, 1), 100)));
    }

    @PostMapping("/{userId}/adjust")
    public ResponseEntity<WalletDto> adjust(
            @PathVariable Long userId,
            @AuthenticationPrincipal User actor,
            @Valid @RequestBody AdminWalletAdjustRequest request) {
        return ResponseEntity.ok(walletService.adminAdjust(userId, request, actor));
    }
}
