package com.domainify.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LinkTicketRequesterRequest {

    @NotNull
    private Long requesterId;

    @Size(max = 2000)
    private String note;

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
