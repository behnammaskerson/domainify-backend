package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class SmsPackReportResultDto {

    private boolean success;
    private String packId;
    private List<SmsDeliveryStatusDataDto> data = new ArrayList<>();
    private Integer httpStatus;
    private Integer providerStatus;

    public static SmsPackReportResultDto success(String packId, List<SmsDeliveryStatusDataDto> data) {
        SmsPackReportResultDto dto = new SmsPackReportResultDto();
        dto.setSuccess(true);
        dto.setPackId(packId);
        dto.setData(data);
        return dto;
    }

    public static SmsPackReportResultDto failure(Integer httpStatus, Integer providerStatus) {
        SmsPackReportResultDto dto = new SmsPackReportResultDto();
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

    public String getPackId() {
        return packId;
    }

    public void setPackId(String packId) {
        this.packId = packId;
    }

    public List<SmsDeliveryStatusDataDto> getData() {
        return data;
    }

    public void setData(List<SmsDeliveryStatusDataDto> data) {
        this.data = data != null ? data : new ArrayList<>();
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
