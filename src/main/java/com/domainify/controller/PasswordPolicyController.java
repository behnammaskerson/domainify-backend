package com.domainify.controller;

import com.domainify.dto.PasswordPolicyDto;
import com.domainify.service.PasswordPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/password-policy")
@PreAuthorize("hasRole('ADMIN')")
public class PasswordPolicyController {

    private final PasswordPolicyService passwordPolicyService;

    public PasswordPolicyController(PasswordPolicyService passwordPolicyService) {
        this.passwordPolicyService = passwordPolicyService;
    }

    @GetMapping
    public ResponseEntity<PasswordPolicyDto> get() {
        return ResponseEntity.ok(passwordPolicyService.getDto());
    }

    @PutMapping
    public ResponseEntity<PasswordPolicyDto> update(@Valid @RequestBody PasswordPolicyDto request) {
        return ResponseEntity.ok(passwordPolicyService.update(request));
    }
}
