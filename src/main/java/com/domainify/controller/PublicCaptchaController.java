package com.domainify.controller;

import com.domainify.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public CAPTCHA endpoints for guest forms.
 */
@RestController
@RequestMapping("/public/captcha")
public class PublicCaptchaController {

    private final CaptchaService captchaService;

    public PublicCaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * Generate a new CAPTCHA challenge.
     */
    @PostMapping("/generate")
    public ResponseEntity<CaptchaService.CaptchaResponse> generateCaptcha(HttpServletRequest request) {
        if (!captchaService.isCaptchaEnabled()) {
            return ResponseEntity.ok(new CaptchaService.CaptchaResponse(null, null));
        }
        
        CaptchaService.CaptchaResponse captcha = captchaService.generateCaptcha(request);
        return ResponseEntity.ok(captcha);
    }
}