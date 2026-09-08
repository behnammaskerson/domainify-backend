package com.domainify.dto;

import java.time.Instant;

public class TicketRequesterChangeDto {

    private Long id;
    private Long changedById;
    private String changedByName;
    private Long fromRequesterId;
    private String fromRequesterName;
    private String fromRequesterEmail;
    private Long toRequesterId;
    private String toRequesterName;
    private String toRequesterEmail;
    private String note;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChangedById() {
        return changedById;
    }

    public void setChangedById(Long changedById) {
        this.changedById = changedById;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public void setChangedByName(String changedByName) {
        this.changedByName = changedByName;
    }

    public Long getFromRequesterId() {
        return fromRequesterId;
    }

    public void setFromRequesterId(Long fromRequesterId) {
        this.fromRequesterId = fromRequesterId;
    }

    public String getFromRequesterName() {
        return fromRequesterName;
    }

    public void setFromRequesterName(String fromRequesterName) {
        this.fromRequesterName = fromRequesterName;
    }

    public String getFromRequesterEmail() {
        return fromRequesterEmail;
    }

    public void setFromRequesterEmail(String fromRequesterEmail) {
        this.fromRequesterEmail = fromRequesterEmail;
    }

    public Long getToRequesterId() {
        return toRequesterId;
    }

    public void setToRequesterId(Long toRequesterId) {
        this.toRequesterId = toRequesterId;
    }

    public String getToRequesterName() {
        return toRequesterName;
    }

    public void setToRequesterName(String toRequesterName) {
        this.toRequesterName = toRequesterName;
    }

    public String getToRequesterEmail() {
        return toRequesterEmail;
    }

    public void setToRequesterEmail(String toRequesterEmail) {
        this.toRequesterEmail = toRequesterEmail;
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
