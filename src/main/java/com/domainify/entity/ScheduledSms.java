package com.domainify.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "scheduled_sms", indexes = {
        @Index(name = "idx_scheduled_sms_pack_id", columnList = "packId", unique = true),
        @Index(name = "idx_scheduled_sms_status_scheduled_at", columnList = "status, scheduledAt")
})
public class ScheduledSms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String packId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScheduledSmsSourceType sourceType;

    @Column(nullable = false, length = 32)
    private String lineNumber;

    @Column(nullable = false, length = 2000)
    private String messageText;

    @Column(nullable = false)
    private int recipientCount;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ScheduledSmsStatus status = ScheduledSmsStatus.PENDING;

    @Column
    private Instant cancelledAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal returnedCreditCount;

    @Column
    private Integer smsCount;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
}
