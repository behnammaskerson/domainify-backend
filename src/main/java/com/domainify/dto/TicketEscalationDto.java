package com.domainify.dto;

import com.domainify.entity.TicketEscalationTrigger;
import com.domainify.entity.TicketPriority;

import java.time.Instant;

public class TicketEscalationDto {

    private Long id;
    private TicketEscalationTrigger triggerType;
    private Long escalatedById;
    private String escalatedByName;
    private TicketPriority fromPriority;
    private TicketPriority toPriority;
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

    public TicketEscalationTrigger getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TicketEscalationTrigger triggerType) {
        this.triggerType = triggerType;
    }

    public Long getEscalatedById() {
        return escalatedById;
    }

    public void setEscalatedById(Long escalatedById) {
        this.escalatedById = escalatedById;
    }

    public String getEscalatedByName() {
        return escalatedByName;
    }

    public void setEscalatedByName(String escalatedByName) {
        this.escalatedByName = escalatedByName;
    }

    public TicketPriority getFromPriority() {
        return fromPriority;
    }

    public void setFromPriority(TicketPriority fromPriority) {
        this.fromPriority = fromPriority;
    }

    public TicketPriority getToPriority() {
        return toPriority;
    }

    public void setToPriority(TicketPriority toPriority) {
        this.toPriority = toPriority;
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
