package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payment_intents")
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentIntentPurpose purpose = PaymentIntentPurpose.WALLET_TOP_UP;

    @Column(name = "amount_irt", nullable = false)
    private long amountIrt;

    @Column(length = 64, unique = true)
    private String authority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentIntentStatus status = PaymentIntentStatus.CREATED;

    @Column(name = "ref_id")
    private Long refId;

    @Column
    private Long fee;

    @Column(name = "card_pan", length = 32)
    private String cardPan;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "offer_id")
    private Long offerId;

    @Column(length = 500)
    private String description = "";

    /** Provider status code when request/verify fails (e.g. ZarinPal code). */
    @Column(name = "gateway_code")
    private Integer gatewayCode;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public PaymentIntentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentIntentStatus status) {
        this.status = status;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public String getCardPan() {
        return cardPan;
    }

    public void setCardPan(String cardPan) {
        this.cardPan = cardPan;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
