package com.domainify.dto;

import com.domainify.entity.BulkTicketAction;
import com.domainify.entity.TicketStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class BulkTicketActionRequest {

    @NotEmpty
    @Size(max = 100)
    private List<Long> ticketIds = new ArrayList<>();

    @NotNull
    private BulkTicketAction action;

    /** ASSIGN: null unassigns. */
    private Long assigneeId;

    /** CHANGE_STATUS */
    private TicketStatus status;

    /** ADD_TAG (merged onto existing tags). */
    private List<Long> tagIds = new ArrayList<>();
    private List<String> names = new ArrayList<>();

    public List<Long> getTicketIds() {
        return ticketIds;
    }

    public void setTicketIds(List<Long> ticketIds) {
        this.ticketIds = ticketIds != null ? ticketIds : new ArrayList<>();
    }

    public BulkTicketAction getAction() {
        return action;
    }

    public void setAction(BulkTicketAction action) {
        this.action = action;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds != null ? tagIds : new ArrayList<>();
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names != null ? names : new ArrayList<>();
    }
}
