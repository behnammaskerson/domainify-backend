package com.domainify.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class WalletTopUpRequest {

    @NotNull
    @Min(1)
    private Long amountIrt;

    public Long getAmountIrt() {
        return amountIrt;
    }

    public void setAmountIrt(Long amountIrt) {
        this.amountIrt = amountIrt;
    }
}
