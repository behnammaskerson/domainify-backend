package com.domainify.dto;

import com.domainify.entity.TicketStatus;

public class TicketStatusDefinitionDto {

    private TicketStatus status;
    private String label;
    private boolean active;
    private int sortOrder;

    public TicketStatusDefinitionDto() {
    }

    public TicketStatusDefinitionDto(TicketStatus status, String label, boolean active, int sortOrder) {
        this.status = status;
        this.label = label;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
