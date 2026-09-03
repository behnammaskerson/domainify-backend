package com.domainify.dto;

import com.domainify.entity.TicketMessageRevision;

import java.time.Instant;

public class TicketMessageRevisionDto {

    private Long id;
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private TicketMessageRevision.Action action;
    private String previousBody;
    private String newBody;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public TicketMessageRevision.Action getAction() {
        return action;
    }

    public void setAction(TicketMessageRevision.Action action) {
        this.action = action;
    }

    public String getPreviousBody() {
        return previousBody;
    }

    public void setPreviousBody(String previousBody) {
        this.previousBody = previousBody;
    }

    public String getNewBody() {
        return newBody;
    }

    public void setNewBody(String newBody) {
        this.newBody = newBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
