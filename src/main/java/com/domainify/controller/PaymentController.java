package com.domainify.controller;

import com.domainify.dto.PaymentVerifyRequest;
import com.domainify.dto.PaymentVerifyResultDto;
import com.domainify.entity.User;
import com.domainify.service.PaymentIntentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentIntentService paymentIntentService;

    public PaymentController(PaymentIntentService paymentIntentService) {
        this.paymentIntentService = paymentIntentService;
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentVerifyResultDto> verify(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(paymentIntentService.verifyForUser(user, request.getAuthority()));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @AuthenticationPrincipal User user,
            @RequestParam("authority") String authority) {
        if (StringUtils.hasText(authority)) {
            paymentIntentService.markCancelledForUser(user, authority);
        }
        return ResponseEntity.ok(Map.of("cancelled", true));
    }
}
