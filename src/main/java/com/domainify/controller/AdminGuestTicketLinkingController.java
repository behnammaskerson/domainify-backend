package com.domainify.controller;

import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.UserRepository;
import com.domainify.service.GuestTicketLinkingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only endpoints for manual guest ticket linking operations.
 */
@RestController
@RequestMapping("/admin/guest-tickets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGuestTicketLinkingController {

    private final GuestTicketLinkingService guestTicketLinkingService;
    private final UserRepository userRepository;

    public AdminGuestTicketLinkingController(
            GuestTicketLinkingService guestTicketLinkingService,
            UserRepository userRepository) {
        this.guestTicketLinkingService = guestTicketLinkingService;
        this.userRepository = userRepository;
    }

    /**
     * Manually link guest tickets with a specific email to a user account.
     * This is useful for cases where automatic linking didn't work or for bulk operations.
     */
    @PostMapping("/link")
    public ResponseEntity<Map<String, Object>> linkGuestTicketsToUser(
            @RequestParam Long userId,
            @RequestParam String email) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        
        int linkedCount = guestTicketLinkingService.linkGuestTicketsToUser(user, email);
        
        return ResponseEntity.ok(Map.of(
                "message", "Guest tickets linked successfully",
                "linkedCount", linkedCount,
                "userId", userId,
                "email", email
        ));
    }
}