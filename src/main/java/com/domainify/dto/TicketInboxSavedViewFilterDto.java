package com.domainify.dto;

import com.domainify.entity.TicketInboxView;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;

import java.time.Instant;

/** Filter snapshot stored inside a saved inbox view. */
public class TicketInboxSavedViewFilterDto {

    private TicketInboxView view;
    private String q;
    private TicketStatus status;
    private TicketPriority priority;
    private Long categoryId;
    private Long queueId;
    private Long tagId;
    private Long assigneeId;
    private Boolean unassigned;
    private String customer;
    private Instant createdFrom;
    private Instant createdTo;

    public TicketInboxView getView() {
        return view;
    }

    public void setView(TicketInboxView view) {
        this.view = view;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

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

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Boolean getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(Boolean unassigned) {
        this.unassigned = unassigned;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
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
}
