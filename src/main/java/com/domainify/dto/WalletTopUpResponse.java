package com.domainify.dto;

public class WalletTopUpResponse {

    private Long paymentIntentId;
    private long amountIrt;
    private String authority;
    private String startPayUrl;

    public Long getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(Long paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
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

    public String getStartPayUrl() {
        return startPayUrl;
    }

    public void setStartPayUrl(String startPayUrl) {
        this.startPayUrl = startPayUrl;
    }
}
