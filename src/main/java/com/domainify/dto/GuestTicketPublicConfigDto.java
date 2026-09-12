package com.domainify.dto;

/** Public guest-support flags for unauthenticated UI. */
public class GuestTicketPublicConfigDto {

    private boolean guestTicketCreateEnabled;
    private boolean guestTicketAttachmentsEnabled;

    public GuestTicketPublicConfigDto() {
    }

    public GuestTicketPublicConfigDto(boolean guestTicketCreateEnabled, boolean guestTicketAttachmentsEnabled) {
        this.guestTicketCreateEnabled = guestTicketCreateEnabled;
        this.guestTicketAttachmentsEnabled = guestTicketAttachmentsEnabled;
    }

    public boolean isGuestTicketCreateEnabled() {
        return guestTicketCreateEnabled;
    }

    public void setGuestTicketCreateEnabled(boolean guestTicketCreateEnabled) {
        this.guestTicketCreateEnabled = guestTicketCreateEnabled;
    }

    public boolean isGuestTicketAttachmentsEnabled() {
        return guestTicketAttachmentsEnabled;
    }

    public void setGuestTicketAttachmentsEnabled(boolean guestTicketAttachmentsEnabled) {
        this.guestTicketAttachmentsEnabled = guestTicketAttachmentsEnabled;
    }
}
