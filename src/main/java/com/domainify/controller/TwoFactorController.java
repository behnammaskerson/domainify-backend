package com.domainify.controller;

import com.domainify.dto.*;
import com.domainify.entity.User;
import com.domainify.service.TwoFactorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me/2fa")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    public TwoFactorController(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    @PostMapping("/setup")
    public ResponseEntity<TotpSetupResponse> setup(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(twoFactorService.beginSetup(user));
    }

    @PostMapping("/enable")
    public ResponseEntity<TotpEnableResponse> enable(@AuthenticationPrincipal User user,
                                                     @Valid @RequestBody TotpCodeRequest request) {
        return ResponseEntity.ok(twoFactorService.enable(user, request));
    }

    @PostMapping("/disable")
    public ResponseEntity<UserDto> disable(@AuthenticationPrincipal User user,
                                           @Valid @RequestBody TotpDisableRequest request) {
        return ResponseEntity.ok(twoFactorService.disable(user, request));
    }

    @PostMapping("/backup-codes")
    public ResponseEntity<TotpEnableResponse> regenerateBackupCodes(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TotpDisableRequest request) {
        return ResponseEntity.ok(twoFactorService.regenerateBackupCodes(user, request));
    }
}
