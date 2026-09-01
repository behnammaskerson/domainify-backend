package com.domainify.dto;

import java.math.BigDecimal;

public class SmsScheduledCancelResultDto {

    private boolean success;
    private BigDecimal returnedCreditCount;
    private Integer smsCount;
    private Integer httpStatus;
    private Integer providerStatus;

    public static SmsScheduledCancelResultDto success(BigDecimal returnedCreditCount, Integer smsCount) {
        SmsScheduledCancelResultDto dto = new SmsScheduledCancelResultDto();
        dto.setSuccess(true);
        dto.setReturnedCreditCount(returnedCreditCount);
        dto.setSmsCount(smsCount);
        return dto;
    }

    public static SmsScheduledCancelResultDto failure(Integer httpStatus, Integer providerStatus) {
        SmsScheduledCancelResultDto dto = new SmsScheduledCancelResultDto();
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

    public BigDecimal getReturnedCreditCount() {
        return returnedCreditCount;
    }

    public void setReturnedCreditCount(BigDecimal returnedCreditCount) {
        this.returnedCreditCount = returnedCreditCount;
    }

    public Integer getSmsCount() {
        return smsCount;
    }

    public void setSmsCount(Integer smsCount) {
        this.smsCount = smsCount;
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
