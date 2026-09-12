package com.domainify.service;

import com.domainify.dto.GuestTicketLinkingSummaryDto;
import com.domainify.entity.Ticket;
import com.domainify.entity.User;
import com.domainify.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

/**
 * Links guest tickets to user accounts when a user registers with an email that has existing guest tickets.
 */
@Service
public class GuestTicketLinkingService {

    private static final Logger log = LoggerFactory.getLogger(GuestTicketLinkingService.class);

    private final TicketRepository ticketRepository;

    public GuestTicketLinkingService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Links all guest tickets with the given email to the specified user.
     * Only links verified guest tickets (those where guestEmailVerified = true).
     * After linking, the tickets become regular user tickets.
     *
     * @param user the user account to link tickets to
     * @param email the email address to search for guest tickets
     * @return the number of tickets that were linked
     */
    @Transactional
    public int linkGuestTicketsToUser(User user, String email) {
        GuestTicketLinkingSummaryDto summary = linkGuestTicketsToUserDetailed(user, email);
        return summary.getLinkedTickets();
    }

    /**
     * Links all guest tickets with the given email to the specified user with detailed results.
     *
     * @param user the user account to link tickets to
     * @param email the email address to search for guest tickets
     * @return detailed summary of the linking operation
     */
    @Transactional
    public GuestTicketLinkingSummaryDto linkGuestTicketsToUserDetailed(User user, String email) {
        if (user == null || user.getId() == null || !StringUtils.hasText(email)) {
            return new GuestTicketLinkingSummaryDto(0, 0, 0, null);
        }

        String normalizedEmail = email.toLowerCase().trim();
        List<Ticket> guestTickets = ticketRepository.findByGuestEmailIgnoreCaseAndRequesterIsNull(normalizedEmail);
        
        if (guestTickets.isEmpty()) {
            return new GuestTicketLinkingSummaryDto(0, 0, 0, null);
        }

        int linkedCount = 0;
        int unverifiedCount = 0;
        Instant linkedAt = null;

        for (Ticket ticket : guestTickets) {
            if (ticket.isGuestEmailVerified()) {
                linkTicketToUser(ticket, user);
                linkedCount++;
                if (linkedAt == null) {
                    linkedAt = Instant.now();
                }
            } else {
                unverifiedCount++;
            }
        }

        if (linkedCount > 0) {
            log.info("Linked {} guest tickets to user {} (email: {}), {} unverified tickets remain", 
                    linkedCount, user.getId(), normalizedEmail, unverifiedCount);
        }

        return new GuestTicketLinkingSummaryDto(guestTickets.size(), linkedCount, unverifiedCount, linkedAt);
    }

    private void linkTicketToUser(Ticket ticket, User user) {
        // Set the requester to the user
        ticket.setRequester(user);
        
        // Clear guest-specific fields since this is now a regular user ticket
        ticket.setGuestName(null);
        ticket.setGuestEmail(null);
        ticket.setGuestEmailVerified(null);
        ticket.setGuestAccessTokenHash(null);
        ticket.setGuestAccessTokenExpiresAt(null);
        
        ticketRepository.save(ticket);
    }
}