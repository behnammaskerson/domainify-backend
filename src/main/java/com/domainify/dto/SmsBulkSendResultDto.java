package com.domainify.dto;

public class SmsBulkSendResultDto {

    private boolean success;
    private SmsBulkSendDataDto data;
    private Integer httpStatus;
    private Integer providerStatus;

    public SmsBulkSendResultDto() {
    }

    public static SmsBulkSendResultDto success(SmsBulkSendDataDto data) {
        SmsBulkSendResultDto dto = new SmsBulkSendResultDto();
        dto.setSuccess(true);
        dto.setData(data);
        return dto;
    }

    public static SmsBulkSendResultDto failure(Integer httpStatus, Integer providerStatus) {
        SmsBulkSendResultDto dto = new SmsBulkSendResultDto();
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

    public SmsBulkSendDataDto getData() {
        return data;
    }

    public void setData(SmsBulkSendDataDto data) {
        this.data = data;
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
