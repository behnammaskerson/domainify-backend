package com.domainify.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cached FX snapshot for the frontend. Domain prices are stored in IRT (تومان);
 * {@code sellByCode} is IRT per 1 unit of each foreign currency (upstream sell rate).
 */
public class CurrencyRatesDto {

    private String base = "IRT";
    private String updatedAt;
    private long fetchedAt;
    private Map<String, Double> sellByCode = new LinkedHashMap<>();

    public CurrencyRatesDto() {
    }

    public CurrencyRatesDto(String base, String updatedAt, long fetchedAt, Map<String, Double> sellByCode) {
        this.base = base;
        this.updatedAt = updatedAt;
        this.fetchedAt = fetchedAt;
        this.sellByCode = sellByCode != null ? sellByCode : new LinkedHashMap<>();
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(long fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Map<String, Double> getSellByCode() {
        return sellByCode == null ? Collections.emptyMap() : sellByCode;
    }

    public void setSellByCode(Map<String, Double> sellByCode) {
        this.sellByCode = sellByCode != null ? sellByCode : new LinkedHashMap<>();
    }
}
