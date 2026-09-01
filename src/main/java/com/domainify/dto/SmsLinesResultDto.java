package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class SmsLinesResultDto {

    private boolean success;
    private List<String> lines = new ArrayList<>();
    private Integer httpStatus;
    private Integer providerStatus;

    public SmsLinesResultDto() {
    }

    public static SmsLinesResultDto success(List<String> lines) {
        SmsLinesResultDto dto = new SmsLinesResultDto();
        dto.setSuccess(true);
        dto.setLines(lines);
        return dto;
    }

    public static SmsLinesResultDto failure(Integer httpStatus, Integer providerStatus) {
        SmsLinesResultDto dto = new SmsLinesResultDto();
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

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
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
