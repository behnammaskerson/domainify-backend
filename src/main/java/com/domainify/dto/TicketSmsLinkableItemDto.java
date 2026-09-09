package com.domainify.dto;

import com.domainify.entity.TicketSmsLinkType;

import java.time.Instant;

/** Candidate SMS item that can be linked to a ticket. */
public class TicketSmsLinkableItemDto {

    private TicketSmsLinkType type;
    private String externalId;
    private String mobile;
    private String messagePreview;
    private String lineNumber;
    private String statusLabel;
    private Integer recipientCount;
    private Instant occurredAt;

    public TicketSmsLinkType getType() {
        return type;
    }

    public void setType(TicketSmsLinkType type) {
        this.type = type;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getMessagePreview() {
        return messagePreview;
    }

    public void setMessagePreview(String messagePreview) {
        this.messagePreview = messagePreview;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public Integer getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
