package com.domainify.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentSettingsDto {

    private String merchantId;
    private boolean sandbox;
    private boolean accessTokenConfigured;
    private boolean enabled;
    private BigDecimal commissionPercent;
    private long minTopUpIrt;
    private long featuredListingPriceIrt;
    private int escrowHoldDays;
    private String callbackPublicBaseUrl;
    private boolean paymentNotificationsEnabled;
    private boolean paymentInAppNotificationsEnabled;
    private boolean paymentEmailNotificationsEnabled;
    private boolean paymentSmsNotificationsEnabled;
    private Instant updatedAt;

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

    public boolean isAccessTokenConfigured() {
        return accessTokenConfigured;
    }

    public void setAccessTokenConfigured(boolean accessTokenConfigured) {
        this.accessTokenConfigured = accessTokenConfigured;
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
