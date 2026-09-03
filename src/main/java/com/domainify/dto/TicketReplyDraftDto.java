package com.domainify.dto;

import java.time.Instant;

public class TicketReplyDraftDto {

    private String body;
    private boolean internalNote;
    private Instant updatedAt;

    public TicketReplyDraftDto() {
    }

    public TicketReplyDraftDto(String body, boolean internalNote, Instant updatedAt) {
        this.body = body;
        this.internalNote = internalNote;
        this.updatedAt = updatedAt;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isInternalNote() {
        return internalNote;
    }

    public void setInternalNote(boolean internalNote) {
        this.internalNote = internalNote;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
