package com.domainify.dto;

public class AssignTicketQueueRequest {

    /** Null clears the queue. */
    private Long queueId;

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }
}
