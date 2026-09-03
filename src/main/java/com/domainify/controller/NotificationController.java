package com.domainify.controller;

import com.domainify.dto.NotificationDto;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.UnreadCountDto;
import com.domainify.entity.User;
import com.domainify.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationDto>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.listForUser(user, pageable, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDto> unreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.unreadCount(user));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(notificationService.markRead(user, id));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<UnreadCountDto> markAllRead(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.markAllRead(user));
    }
}
