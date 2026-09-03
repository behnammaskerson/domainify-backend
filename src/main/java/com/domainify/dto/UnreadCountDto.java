package com.domainify.dto;

public class UnreadCountDto {
    private long count;

    public UnreadCountDto() {
    }

    public UnreadCountDto(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
