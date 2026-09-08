package com.domainify.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminWalletAdjustRequest {

    public enum Direction {
        CREDIT,
        DEBIT
    }

    @NotNull
    private Direction direction;

    @NotNull
    @Min(1)
    private Long amountIrt;

    @NotBlank
    @Size(max = 500)
    private String note;

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Long getAmountIrt() {
        return amountIrt;
    }

    public void setAmountIrt(Long amountIrt) {
        this.amountIrt = amountIrt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
