package com.domainify.dto;

import com.domainify.entity.ScheduledSmsSourceType;
import com.domainify.entity.ScheduledSmsStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class SmsScheduledItemDto {

    private String packId;
    private ScheduledSmsSourceType sourceType;
    private String lineNumber;
    private String messageText;
    private int recipientCount;
    private BigDecimal cost;
    private Instant scheduledAt;
    private ScheduledSmsStatus status;
    private Instant cancelledAt;
    private BigDecimal returnedCreditCount;
    private Integer smsCount;
    private Instant createdAt;
    private boolean cancellable;

    public String getPackId() {
        return packId;
    }

    public void setPackId(String packId) {
        this.packId = packId;
    }

    public ScheduledSmsSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(ScheduledSmsSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public ScheduledSmsStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduledSmsStatus status) {
        this.status = status;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public BigDecimal getReturnedCreditCount() {
        return returnedCreditCount;
    }

    public void setReturnedCreditCount(BigDecimal returnedCreditCount) {
        this.returnedCreditCount = returnedCreditCount;
    }

    public Integer getSmsCount() {
        return smsCount;
    }

    public void setSmsCount(Integer smsCount) {
        this.smsCount = smsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;
    }
}
