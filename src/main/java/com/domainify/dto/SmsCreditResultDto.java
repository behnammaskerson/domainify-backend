package com.domainify.dto;

import java.math.BigDecimal;

public class SmsCreditResultDto {

    private boolean success;
    private BigDecimal credit;
    private Integer httpStatus;
    private Integer providerStatus;

    public SmsCreditResultDto() {
    }

    public static SmsCreditResultDto success(BigDecimal credit) {
        SmsCreditResultDto dto = new SmsCreditResultDto();
        dto.setSuccess(true);
        dto.setCredit(credit);
        return dto;
    }

    public static SmsCreditResultDto failure(Integer httpStatus, Integer providerStatus) {
        SmsCreditResultDto dto = new SmsCreditResultDto();
        dto.setSuccess(false);
        dto.setHttpStatus(httpStatus);
        dto.setProviderStatus(providerStatus);
        return dto;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Integer getProviderStatus() {
        return providerStatus;
    }

    public void setProviderStatus(Integer providerStatus) {
        this.providerStatus = providerStatus;
    }
}
