package com.domainify.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentSettingsUpdateRequest {

    @Size(max = 128)
    private String merchantId;

    @NotNull
    private Boolean sandbox;

    /** Leave blank to keep existing token. */
    @Size(max = 1024)
    private String accessToken;

    @NotNull
    private Boolean enabled;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal commissionPercent;

    @NotNull
    @Min(1000)
    private Long minTopUpIrt;

    @NotNull
    @Min(0)
    private Long featuredListingPriceIrt;

    @NotNull
    @Min(0)
    @Max(365)
    private Integer escrowHoldDays;

    @Size(max = 512)
    private String callbackPublicBaseUrl;

    @NotNull
    private Boolean paymentNotificationsEnabled;

    @NotNull
    private Boolean paymentInAppNotificationsEnabled;

    @NotNull
    private Boolean paymentEmailNotificationsEnabled;

    @NotNull
    private Boolean paymentSmsNotificationsEnabled;

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public Boolean getSandbox() {
        return sandbox;
    }

    public void setSandbox(Boolean sandbox) {
        this.sandbox = sandbox;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(BigDecimal commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public Long getMinTopUpIrt() {
        return minTopUpIrt;
    }

    public void setMinTopUpIrt(Long minTopUpIrt) {
        this.minTopUpIrt = minTopUpIrt;
    }

    public Long getFeaturedListingPriceIrt() {
        return featuredListingPriceIrt;
    }

    public void setFeaturedListingPriceIrt(Long featuredListingPriceIrt) {
        this.featuredListingPriceIrt = featuredListingPriceIrt;
    }

    public Integer getEscrowHoldDays() {
        return escrowHoldDays;
    }

    public void setEscrowHoldDays(Integer escrowHoldDays) {
        this.escrowHoldDays = escrowHoldDays;
    }

    public String getCallbackPublicBaseUrl() {
        return callbackPublicBaseUrl;
    }

    public void setCallbackPublicBaseUrl(String callbackPublicBaseUrl) {
        this.callbackPublicBaseUrl = callbackPublicBaseUrl;
    }

    public Boolean getPaymentNotificationsEnabled() {
        return paymentNotificationsEnabled;
    }

    public void setPaymentNotificationsEnabled(Boolean paymentNotificationsEnabled) {
        this.paymentNotificationsEnabled = paymentNotificationsEnabled;
    }

    public Boolean getPaymentInAppNotificationsEnabled() {
        return paymentInAppNotificationsEnabled;
    }

    public void setPaymentInAppNotificationsEnabled(Boolean paymentInAppNotificationsEnabled) {
        this.paymentInAppNotificationsEnabled = paymentInAppNotificationsEnabled;
    }

    public Boolean getPaymentEmailNotificationsEnabled() {
        return paymentEmailNotificationsEnabled;
    }

    public void setPaymentEmailNotificationsEnabled(Boolean paymentEmailNotificationsEnabled) {
        this.paymentEmailNotificationsEnabled = paymentEmailNotificationsEnabled;
    }

    public Boolean getPaymentSmsNotificationsEnabled() {
        return paymentSmsNotificationsEnabled;
    }

    public void setPaymentSmsNotificationsEnabled(Boolean paymentSmsNotificationsEnabled) {
        this.paymentSmsNotificationsEnabled = paymentSmsNotificationsEnabled;
    }
}
