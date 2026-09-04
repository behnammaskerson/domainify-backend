package com.domainify.dto;

public class TransferTicketRequest {

    /** Target assignee; null clears assignee. Only applied when {@link #assigneeChanged} is true. */
    private Long assigneeId;
    private boolean assigneeChanged;

    /** Target queue; null clears queue. Only applied when {@link #queueChanged} is true. */
    private Long queueId;
    private boolean queueChanged;

    private String note;

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
