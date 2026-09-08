package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class LinkTicketDomainsRequest {

    private List<Long> domainIds = new ArrayList<>();

    public List<Long> getDomainIds() {
        return domainIds;
    }

    public void setDomainIds(List<Long> domainIds) {
        this.domainIds = domainIds != null ? domainIds : new ArrayList<>();
    }
}
