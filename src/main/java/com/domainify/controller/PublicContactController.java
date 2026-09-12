package com.domainify.controller;

import com.domainify.dto.ContactRequest;
import com.domainify.service.MessageService;
import com.domainify.service.PublicContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicContactController {

    private final PublicContactService publicContactService;
    private final MessageService messageService;

    public PublicContactController(PublicContactService publicContactService, MessageService messageService) {
        this.publicContactService = publicContactService;
        this.messageService = messageService;
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, String>> contact(
            @Valid @RequestBody ContactRequest request,
            @RequestParam(required = false) String captchaToken,
            @RequestParam(required = false) String captchaAnswer,
            HttpServletRequest httpRequest) {
        publicContactService.submit(request, captchaToken, captchaAnswer, httpRequest);
        return ResponseEntity.ok(Map.of("message", messageService.get("contact.submitted")));
    }
}
