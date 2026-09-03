package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class UpdateTicketTagsRequest {

    private List<Long> tagIds = new ArrayList<>();
    private List<String> names = new ArrayList<>();

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds != null ? tagIds : new ArrayList<>();
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names != null ? names : new ArrayList<>();
    }
}
