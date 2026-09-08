package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class TicketInboxSavedViewDto {

    private Long id;
    private String name;
    private boolean isDefault;
    private int sortOrder;
    private TicketInboxSavedViewFilterDto filter;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("isDefault")
    public boolean isDefault() {
        return isDefault;
    }

    @JsonProperty("isDefault")
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public TicketInboxSavedViewFilterDto getFilter() {
        return filter;
    }

    public void setFilter(TicketInboxSavedViewFilterDto filter) {
        this.filter = filter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
