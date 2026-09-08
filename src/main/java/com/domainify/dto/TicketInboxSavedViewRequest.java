package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketInboxSavedViewRequest {

    @NotBlank
    @Size(min = 1, max = 80)
    private String name;

    private TicketInboxSavedViewFilterDto filter;

    private Boolean isDefault;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TicketInboxSavedViewFilterDto getFilter() {
        return filter;
    }

    public void setFilter(TicketInboxSavedViewFilterDto filter) {
        this.filter = filter;
    }

    @JsonProperty("isDefault")
    public Boolean getIsDefault() {
        return isDefault;
    }

    @JsonProperty("isDefault")
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
