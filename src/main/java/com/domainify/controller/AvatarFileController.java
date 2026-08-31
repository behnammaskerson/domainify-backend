package com.domainify.controller;

import com.domainify.service.AvatarStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files/avatars")
public class AvatarFileController {

    private final AvatarStorageService avatarStorageService;

    public AvatarFileController(AvatarStorageService avatarStorageService) {
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getAvatar(@PathVariable Long userId) {
        return avatarStorageService.load(userId)
                .<ResponseEntity<?>>map(stored -> ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                        .contentType(stored.mediaType())
                        .body(stored.resource()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
