package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class SmsReceiveLatestResultDto {

    private boolean success;
    private List<SmsReceivedMessageDto> data = new ArrayList<>();
    private int count;
    private Integer httpStatus;
    private Integer providerStatus;

    public static SmsReceiveLatestResultDto success(List<SmsReceivedMessageDto> data, int count) {
        SmsReceiveLatestResultDto dto = new SmsReceiveLatestResultDto();
        dto.setSuccess(true);
        dto.setData(data);
        dto.setCount(count);
        return dto;
    }

    public static SmsReceiveLatestResultDto failure(Integer httpStatus, Integer providerStatus) {
        SmsReceiveLatestResultDto dto = new SmsReceiveLatestResultDto();
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

    public List<SmsReceivedMessageDto> getData() {
        return data;
    }

    public void setData(List<SmsReceivedMessageDto> data) {
        this.data = data != null ? data : new ArrayList<>();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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
