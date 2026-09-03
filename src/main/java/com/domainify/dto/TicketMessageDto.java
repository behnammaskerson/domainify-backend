package com.domainify.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TicketMessageDto {

    private Long id;
    private String body;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private boolean mine;
    /** True when the author is support staff (admin/agent). */
    private boolean staff;
    /** True for the opening ticket description (not a reply row). */
    private boolean initial;
    private Instant createdAt;
    private List<TicketAttachmentDto> attachments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public boolean isStaff() {
        return staff;
    }

    public void setStaff(boolean staff) {
        this.staff = staff;
    }

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<TicketAttachmentDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<TicketAttachmentDto> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }
}
