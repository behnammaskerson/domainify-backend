package com.domainify.dto;

import com.domainify.entity.PaymentIntentPurpose;
import com.domainify.entity.PaymentIntentStatus;
import com.domainify.entity.WalletLedgerDirection;
import com.domainify.entity.WalletLedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class WalletDto {

    private Long walletId;
    private BigDecimal availableBalance;
    private BigDecimal heldBalance;
    private long minTopUpIrt;
    private boolean paymentsEnabled;
    private List<WalletLedgerEntryDto> recentLedger;
    private List<PaymentIntentSummaryDto> recentPayments;

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
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

    public long getMinTopUpIrt() {
        return minTopUpIrt;
    }

    public void setMinTopUpIrt(long minTopUpIrt) {
        this.minTopUpIrt = minTopUpIrt;
    }

    public boolean isPaymentsEnabled() {
        return paymentsEnabled;
    }

    public void setPaymentsEnabled(boolean paymentsEnabled) {
        this.paymentsEnabled = paymentsEnabled;
    }

    public List<WalletLedgerEntryDto> getRecentLedger() {
        return recentLedger;
    }

    public void setRecentLedger(List<WalletLedgerEntryDto> recentLedger) {
        this.recentLedger = recentLedger;
    }

    public List<PaymentIntentSummaryDto> getRecentPayments() {
        return recentPayments;
    }

    public void setRecentPayments(List<PaymentIntentSummaryDto> recentPayments) {
        this.recentPayments = recentPayments;
    }

    public static class WalletLedgerEntryDto {
        private Long id;
        private WalletLedgerDirection direction;
        private BigDecimal amount;
        private BigDecimal availableAfter;
        private BigDecimal heldAfter;
        private WalletLedgerEntryType entryType;
        private String note;
        private Long paymentIntentId;
        private Instant createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public WalletLedgerDirection getDirection() {
            return direction;
        }

        public void setDirection(WalletLedgerDirection direction) {
            this.direction = direction;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAvailableAfter() {
            return availableAfter;
        }

        public void setAvailableAfter(BigDecimal availableAfter) {
            this.availableAfter = availableAfter;
        }

        public BigDecimal getHeldAfter() {
            return heldAfter;
        }

        public void setHeldAfter(BigDecimal heldAfter) {
            this.heldAfter = heldAfter;
        }

        public WalletLedgerEntryType getEntryType() {
            return entryType;
        }

        public void setEntryType(WalletLedgerEntryType entryType) {
            this.entryType = entryType;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public Long getPaymentIntentId() {
            return paymentIntentId;
        }

        public void setPaymentIntentId(Long paymentIntentId) {
            this.paymentIntentId = paymentIntentId;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class PaymentIntentSummaryDto {
        private Long id;
        private PaymentIntentPurpose purpose;
        private long amountIrt;
        private PaymentIntentStatus status;
        private String authority;
        private Long refId;
        private Integer gatewayCode;
        private String failureReason;
        private Instant failedAt;
        private Instant verifiedAt;
        private Instant createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public PaymentIntentPurpose getPurpose() {
            return purpose;
        }

        public void setPurpose(PaymentIntentPurpose purpose) {
            this.purpose = purpose;
        }

        public long getAmountIrt() {
            return amountIrt;
        }

        public void setAmountIrt(long amountIrt) {
            this.amountIrt = amountIrt;
        }

        public PaymentIntentStatus getStatus() {
            return status;
        }

        public void setStatus(PaymentIntentStatus status) {
            this.status = status;
        }

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public Long getRefId() {
            return refId;
        }

        public void setRefId(Long refId) {
            this.refId = refId;
        }

        public Integer getGatewayCode() {
            return gatewayCode;
        }

        public void setGatewayCode(Integer gatewayCode) {
            this.gatewayCode = gatewayCode;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public Instant getFailedAt() {
            return failedAt;
        }

        public void setFailedAt(Instant failedAt) {
            this.failedAt = failedAt;
        }

        public Instant getVerifiedAt() {
            return verifiedAt;
        }

        public void setVerifiedAt(Instant verifiedAt) {
            this.verifiedAt = verifiedAt;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
}
