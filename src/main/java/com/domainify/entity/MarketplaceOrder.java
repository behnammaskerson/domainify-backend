package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "marketplace_orders", indexes = {
        @Index(name = "idx_mo_buyer", columnList = "buyer_id"),
        @Index(name = "idx_mo_seller", columnList = "seller_id"),
        @Index(name = "idx_mo_status", columnList = "status"),
        @Index(name = "idx_mo_listing", columnList = "listing_id")
})
public class MarketplaceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private DomainListing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id")
    private DomainListingOffer offer;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "commission_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "seller_net", nullable = false, precision = 14, scale = 2)
    private BigDecimal sellerNet = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MarketplaceOrderStatus status = MarketplaceOrderStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private MarketplaceOrderPaymentMethod paymentMethod;

    @Column(name = "payment_intent_id")
    private Long paymentIntentId;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "payment_deadline")
    private Instant paymentDeadline;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DomainListing getListing() {
        return listing;
    }

    public void setListing(DomainListing listing) {
        this.listing = listing;
    }

    public User getBuyer() {
        return buyer;
    }

    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    public DomainListingOffer getOffer() {
        return offer;
    }

    public void setOffer(DomainListingOffer offer) {
        this.offer = offer;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public BigDecimal getSellerNet() {
        return sellerNet;
    }

    public void setSellerNet(BigDecimal sellerNet) {
        this.sellerNet = sellerNet;
    }

    public MarketplaceOrderStatus getStatus() {
        return status;
    }

    public void setStatus(MarketplaceOrderStatus status) {
        this.status = status;
    }

    public MarketplaceOrderPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(MarketplaceOrderPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(Long paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getPaymentDeadline() {
        return paymentDeadline;
    }

    public void setPaymentDeadline(Instant paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
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
