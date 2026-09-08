package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_settings")
public class PaymentSettings {

    public static final long SINGLETON_ID = 1L;
    public static final BigDecimal DEFAULT_COMMISSION_PERCENT = new BigDecimal("5.00");
    public static final long DEFAULT_MIN_TOP_UP_IRT = 10_000L;
    public static final long DEFAULT_FEATURED_LISTING_PRICE_IRT = 50_000L;
    public static final int DEFAULT_ESCROW_HOLD_DAYS = 3;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "merchant_id", length = 128)
    private String merchantId = "";

    @Column(nullable = false)
    private boolean sandbox = true;

    @Column(name = "access_token", length = 1024)
    private String accessToken = "";

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "commission_percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal commissionPercent = DEFAULT_COMMISSION_PERCENT;

    @Column(name = "min_top_up_irt", nullable = false)
    private long minTopUpIrt = DEFAULT_MIN_TOP_UP_IRT;

    @Column(name = "featured_listing_price_irt", nullable = false)
    private long featuredListingPriceIrt = DEFAULT_FEATURED_LISTING_PRICE_IRT;

    @Column(name = "escrow_hold_days", nullable = false)
    private int escrowHoldDays = DEFAULT_ESCROW_HOLD_DAYS;

    @Column(name = "callback_public_base_url", length = 512)
    private String callbackPublicBaseUrl = "";

    /** Master switch for payment transaction notifications. */
    @Column(name = "payment_notifications_enabled", nullable = false)
    private boolean paymentNotificationsEnabled = true;

    @Column(name = "payment_in_app_notifications_enabled", nullable = false)
    private boolean paymentInAppNotificationsEnabled = true;

    @Column(name = "payment_email_notifications_enabled", nullable = false)
    private boolean paymentEmailNotificationsEnabled = true;

    @Column(name = "payment_sms_notifications_enabled", nullable = false)
    private boolean paymentSmsNotificationsEnabled = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public static PaymentSettings defaults() {
        PaymentSettings settings = new PaymentSettings();
        settings.setId(SINGLETON_ID);
        return settings;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(BigDecimal commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public long getMinTopUpIrt() {
        return minTopUpIrt;
    }

    public void setMinTopUpIrt(long minTopUpIrt) {
        this.minTopUpIrt = minTopUpIrt;
    }

    public long getFeaturedListingPriceIrt() {
        return featuredListingPriceIrt;
    }

    public void setFeaturedListingPriceIrt(long featuredListingPriceIrt) {
        this.featuredListingPriceIrt = featuredListingPriceIrt;
    }

    public int getEscrowHoldDays() {
        return escrowHoldDays;
    }

    public void setEscrowHoldDays(int escrowHoldDays) {
        this.escrowHoldDays = escrowHoldDays;
    }

    public String getCallbackPublicBaseUrl() {
        return callbackPublicBaseUrl;
    }

    public void setCallbackPublicBaseUrl(String callbackPublicBaseUrl) {
        this.callbackPublicBaseUrl = callbackPublicBaseUrl;
    }

    public boolean isPaymentNotificationsEnabled() {
        return paymentNotificationsEnabled;
    }

    public void setPaymentNotificationsEnabled(boolean paymentNotificationsEnabled) {
        this.paymentNotificationsEnabled = paymentNotificationsEnabled;
    }

    public boolean isPaymentInAppNotificationsEnabled() {
        return paymentInAppNotificationsEnabled;
    }

    public void setPaymentInAppNotificationsEnabled(boolean paymentInAppNotificationsEnabled) {
        this.paymentInAppNotificationsEnabled = paymentInAppNotificationsEnabled;
    }

    public boolean isPaymentEmailNotificationsEnabled() {
        return paymentEmailNotificationsEnabled;
    }

    public void setPaymentEmailNotificationsEnabled(boolean paymentEmailNotificationsEnabled) {
        this.paymentEmailNotificationsEnabled = paymentEmailNotificationsEnabled;
    }

    public boolean isPaymentSmsNotificationsEnabled() {
        return paymentSmsNotificationsEnabled;
    }

    public void setPaymentSmsNotificationsEnabled(boolean paymentSmsNotificationsEnabled) {
        this.paymentSmsNotificationsEnabled = paymentSmsNotificationsEnabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
