package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class SmsScheduledPagedResponse {

    private List<SmsScheduledItemDto> content = new ArrayList<>();
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
    private long allCount;
    private long pendingCount;
    private long cancelledCount;
    private long sentCount;

    public static SmsScheduledPagedResponse from(
            org.springframework.data.domain.Page<SmsScheduledItemDto> page,
            long allCount,
            long pendingCount,
            long cancelledCount,
            long sentCount) {
        SmsScheduledPagedResponse response = new SmsScheduledPagedResponse();
        response.setContent(page.getContent());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setNumber(page.getNumber());
        response.setSize(page.getSize());
        response.setAllCount(allCount);
        response.setPendingCount(pendingCount);
        response.setCancelledCount(cancelledCount);
        response.setSentCount(sentCount);
        return response;
    }

    public List<SmsScheduledItemDto> getContent() {
        return content;
    }

    public void setContent(List<SmsScheduledItemDto> content) {
        this.content = content != null ? content : new ArrayList<>();
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getAllCount() {
        return allCount;
    }

    public void setAllCount(long allCount) {
        this.allCount = allCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(long cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public long getSentCount() {
        return sentCount;
    }

    public void setSentCount(long sentCount) {
        this.sentCount = sentCount;
    }
}
