package com.domainify.dto;

import com.domainify.entity.PaymentIntentPurpose;
import com.domainify.entity.PaymentIntentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentVerifyResultDto {

    private Long paymentIntentId;
    private PaymentIntentPurpose purpose;
    private PaymentIntentStatus status;
    private long amountIrt;
    private Long refId;
    private BigDecimal availableBalance;
    private BigDecimal heldBalance;
    private boolean alreadyVerified;
    private Instant verifiedAt;

    public Long getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(Long paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public PaymentIntentPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(PaymentIntentPurpose purpose) {
        this.purpose = purpose;
    }

    public PaymentIntentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentIntentStatus status) {
        this.status = status;
    }

    public long getAmountIrt() {
        return amountIrt;
    }

    public void setAmountIrt(long amountIrt) {
        this.amountIrt = amountIrt;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getHeldBalance() {
        return heldBalance;
    }

    public void setHeldBalance(BigDecimal heldBalance) {
        this.heldBalance = heldBalance;
    }

    public boolean isAlreadyVerified() {
        return alreadyVerified;
    }

    public void setAlreadyVerified(boolean alreadyVerified) {
        this.alreadyVerified = alreadyVerified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
