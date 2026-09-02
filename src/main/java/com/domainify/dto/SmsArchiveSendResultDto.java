package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class SmsArchiveSendResultDto {

    private boolean success;
    private List<SmsDeliveryStatusDataDto> data = new ArrayList<>();
    private int pageNumber;
    private int pageSize;
    private boolean hasMore;
    private Integer httpStatus;
    private Integer providerStatus;

    public static SmsArchiveSendResultDto success(
            List<SmsDeliveryStatusDataDto> data,
            int pageNumber,
            int pageSize,
            boolean hasMore) {
        SmsArchiveSendResultDto dto = new SmsArchiveSendResultDto();
        dto.setSuccess(true);
        dto.setData(data);
        dto.setPageNumber(pageNumber);
        dto.setPageSize(pageSize);
        dto.setHasMore(hasMore);
        return dto;
    }

    public static SmsArchiveSendResultDto failure(Integer httpStatus, Integer providerStatus) {
        SmsArchiveSendResultDto dto = new SmsArchiveSendResultDto();
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

    public List<SmsDeliveryStatusDataDto> getData() {
        return data;
    }

    public void setData(List<SmsDeliveryStatusDataDto> data) {
        this.data = data != null ? data : new ArrayList<>();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
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
