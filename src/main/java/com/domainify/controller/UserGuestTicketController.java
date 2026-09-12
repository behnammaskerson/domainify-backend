package com.domainify.controller;

import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.UserRepository;
import com.domainify.service.GuestTicketLinkingService;
import com.domainify.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User-facing endpoints for guest ticket linking operations.
 */
@RestController
@RequestMapping("/user/guest-tickets")
public class UserGuestTicketController {

    private final GuestTicketLinkingService guestTicketLinkingService;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public UserGuestTicketController(
            GuestTicketLinkingService guestTicketLinkingService,
            UserRepository userRepository,
            MessageService messageService) {
        this.guestTicketLinkingService = guestTicketLinkingService;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    /**
     * Manually link guest tickets with the current user's email address.
     * This is useful if automatic linking during registration didn't work
     * or if the user wants to check for guest tickets created before registration.
     */
    @PostMapping("/link-mine")
    public ResponseEntity<Map<String, Object>> linkMyGuestTickets(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        int linkedCount = guestTicketLinkingService.linkGuestTicketsToUser(user, user.getEmail());
        
        String message;
        if (linkedCount == 0) {
            message = messageService.get("ticket.guest.linking.none");
        } else {
            message = messageService.get("ticket.guest.linking.success", new Object[]{linkedCount});
        }
        
        return ResponseEntity.ok(Map.of(
                "message", message,
                "linkedCount", linkedCount
        ));
    }
}