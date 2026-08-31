package com.domainify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class PasswordPolicyDto {

    @NotNull
    @Min(4)
    @Max(128)
    private Integer minLength;

    @NotNull
    @Min(4)
    @Max(256)
    private Integer maxLength;

    @NotNull
    private Boolean requireUppercase;

    @NotNull
    private Boolean requireLowercase;

    @NotNull
    private Boolean requireDigit;

    @NotNull
    private Boolean requireSpecial;

    @NotNull
    @Min(0)
    @Max(3650)
    private Integer expiryDays;

    @NotNull
    @Min(0)
    @Max(24)
    private Integer historyCount;

    private Instant updatedAt;

    public PasswordPolicyDto() {
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Boolean getRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(Boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public Boolean getRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(Boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }

    public Boolean getRequireDigit() {
        return requireDigit;
    }

    public void setRequireDigit(Boolean requireDigit) {
        this.requireDigit = requireDigit;
    }

    public Boolean getRequireSpecial() {
        return requireSpecial;
    }

    public void setRequireSpecial(Boolean requireSpecial) {
        this.requireSpecial = requireSpecial;
    }

    public Integer getExpiryDays() {
        return expiryDays;
    }

    public void setExpiryDays(Integer expiryDays) {
        this.expiryDays = expiryDays;
    }

    public Integer getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(Integer historyCount) {
        this.historyCount = historyCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
