package com.domainify.dto;

import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;

import java.time.Instant;

public class TicketInboxFilter {

    private TicketStatus status;
    private TicketPriority priority;
    private Long categoryId;
    private Long assigneeId;
    private boolean unassignedOnly;
    private Instant createdFrom;
    private Instant createdTo;
    private Long tagId;
    private String customer;

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public boolean isUnassignedOnly() {
        return unassignedOnly;
    }

    public void setUnassignedOnly(boolean unassignedOnly) {
        this.unassignedOnly = unassignedOnly;
    }

    public Instant getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(Instant createdFrom) {
        this.createdFrom = createdFrom;
    }

    public Instant getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(Instant createdTo) {
        this.createdTo = createdTo;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }
}
