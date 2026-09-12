package com.domainify.dto;

import java.time.Instant;

/**
 * Summary information about guest ticket linking results.
 */
public class GuestTicketLinkingSummaryDto {

    private int totalGuestTickets;
    private int linkedTickets;
    private int unverifiedTickets;
    private Instant linkedAt;

    public GuestTicketLinkingSummaryDto() {
    }

    public GuestTicketLinkingSummaryDto(int totalGuestTickets, int linkedTickets, int unverifiedTickets, Instant linkedAt) {
        this.totalGuestTickets = totalGuestTickets;
        this.linkedTickets = linkedTickets;
        this.unverifiedTickets = unverifiedTickets;
        this.linkedAt = linkedAt;
    }

    public int getTotalGuestTickets() {
        return totalGuestTickets;
    }

    public void setTotalGuestTickets(int totalGuestTickets) {
        this.totalGuestTickets = totalGuestTickets;
    }

    public int getLinkedTickets() {
        return linkedTickets;
    }

    public void setLinkedTickets(int linkedTickets) {
        this.linkedTickets = linkedTickets;
    }

    public int getUnverifiedTickets() {
        return unverifiedTickets;
    }

    public void setUnverifiedTickets(int unverifiedTickets) {
        this.unverifiedTickets = unverifiedTickets;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }
}