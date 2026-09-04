package com.domainify.dto;

import com.domainify.entity.TicketPriority;

public class EscalateTicketRequest {

    /** When true, bump priority one level (ignored if {@link #priorityChanged} is true). */
    private boolean bumpPriority;

    private TicketPriority priority;
    private boolean priorityChanged;

    private Long assigneeId;
    private boolean assigneeChanged;

    private Long queueId;
    private boolean queueChanged;

    private String note;

    public boolean isBumpPriority() {
        return bumpPriority;
    }

    public void setBumpPriority(boolean bumpPriority) {
        this.bumpPriority = bumpPriority;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public boolean isPriorityChanged() {
        return priorityChanged;
    }

    public void setPriorityChanged(boolean priorityChanged) {
        this.priorityChanged = priorityChanged;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public boolean isAssigneeChanged() {
        return assigneeChanged;
    }

    public void setAssigneeChanged(boolean assigneeChanged) {
        this.assigneeChanged = assigneeChanged;
    }

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }

    public boolean isQueueChanged() {
        return queueChanged;
    }

    public void setQueueChanged(boolean queueChanged) {
        this.queueChanged = queueChanged;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
