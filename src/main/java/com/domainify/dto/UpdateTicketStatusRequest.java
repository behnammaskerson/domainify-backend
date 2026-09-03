package com.domainify.dto;

import com.domainify.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateTicketStatusRequest {

    @NotNull
    private TicketStatus status;

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}
