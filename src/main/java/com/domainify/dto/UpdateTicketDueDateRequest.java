package com.domainify.dto;

import java.time.Instant;

public class UpdateTicketDueDateRequest {

    private Instant dueAt;
    private Boolean recalculateFromPriority;

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Boolean getRecalculateFromPriority() {
        return recalculateFromPriority;
    }

    public void setRecalculateFromPriority(Boolean recalculateFromPriority) {
        this.recalculateFromPriority = recalculateFromPriority;
    }
}
