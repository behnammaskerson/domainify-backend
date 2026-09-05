package com.domainify.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class DomainStatusCountsDto {

    private long all;
    private Map<String, Long> byStatus = new LinkedHashMap<>();

    public DomainStatusCountsDto() {
    }

    public DomainStatusCountsDto(long all, Map<String, Long> byStatus) {
        this.all = all;
        this.byStatus = byStatus != null ? byStatus : new LinkedHashMap<>();
    }

    public long getAll() {
        return all;
    }

    public void setAll(long all) {
        this.all = all;
    }

    public Map<String, Long> getByStatus() {
        return byStatus;
    }

    public void setByStatus(Map<String, Long> byStatus) {
        this.byStatus = byStatus != null ? byStatus : new LinkedHashMap<>();
    }
}
