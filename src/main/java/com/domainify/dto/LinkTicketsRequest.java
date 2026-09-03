package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class LinkTicketsRequest {

    private List<Long> relatedTicketIds = new ArrayList<>();

    public List<Long> getRelatedTicketIds() {
        return relatedTicketIds;
    }

    public void setRelatedTicketIds(List<Long> relatedTicketIds) {
        this.relatedTicketIds = relatedTicketIds != null ? relatedTicketIds : new ArrayList<>();
    }
}
