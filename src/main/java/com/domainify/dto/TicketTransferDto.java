package com.domainify.dto;

import java.time.Instant;

public class TicketTransferDto {

    private Long id;
    private Long transferredById;
    private String transferredByName;
    private Long fromAssigneeId;
    private String fromAssigneeName;
    private Long toAssigneeId;
    private String toAssigneeName;
    private Long fromQueueId;
    private String fromQueueName;
    private Long toQueueId;
    private String toQueueName;
    private String note;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransferredById() {
        return transferredById;
    }

    public void setTransferredById(Long transferredById) {
        this.transferredById = transferredById;
    }

    public String getTransferredByName() {
        return transferredByName;
    }

    public void setTransferredByName(String transferredByName) {
        this.transferredByName = transferredByName;
    }

    public Long getFromAssigneeId() {
        return fromAssigneeId;
    }

    public void setFromAssigneeId(Long fromAssigneeId) {
        this.fromAssigneeId = fromAssigneeId;
    }

    public String getFromAssigneeName() {
        return fromAssigneeName;
    }

    public void setFromAssigneeName(String fromAssigneeName) {
        this.fromAssigneeName = fromAssigneeName;
    }

    public Long getToAssigneeId() {
        return toAssigneeId;
    }

    public void setToAssigneeId(Long toAssigneeId) {
        this.toAssigneeId = toAssigneeId;
    }

    public String getToAssigneeName() {
        return toAssigneeName;
    }

    public void setToAssigneeName(String toAssigneeName) {
        this.toAssigneeName = toAssigneeName;
    }

    public Long getFromQueueId() {
        return fromQueueId;
    }

    public void setFromQueueId(Long fromQueueId) {
        this.fromQueueId = fromQueueId;
    }

    public String getFromQueueName() {
        return fromQueueName;
    }

    public void setFromQueueName(String fromQueueName) {
        this.fromQueueName = fromQueueName;
    }

    public Long getToQueueId() {
        return toQueueId;
    }

    public void setToQueueId(Long toQueueId) {
        this.toQueueId = toQueueId;
    }

    public String getToQueueName() {
        return toQueueName;
    }

    public void setToQueueName(String toQueueName) {
        this.toQueueName = toQueueName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
