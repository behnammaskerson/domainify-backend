package com.domainify.dto;

import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TicketDto {

    private Long id;
    private String publicNumber;
    private String subject;
    private String description;
    private TicketCategoryDto category;
    private TicketPriority priority;
    private TicketStatus status;
    private Long requesterId;
    private String requesterEmail;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TicketAttachmentDto> attachments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicNumber() {
        return publicNumber;
    }

    public void setPublicNumber(String publicNumber) {
        this.publicNumber = publicNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketCategoryDto getCategory() {
        return category;
    }

    public void setCategory(TicketCategoryDto category) {
        this.category = category;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TicketAttachmentDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<TicketAttachmentDto> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }
}
