package com.domainify.dto;

import com.domainify.entity.DomainOwnershipStatus;
import com.domainify.entity.DomainStatus;

public class RelatedDomainDto {

    private Long id;
    private String name;
    private DomainStatus status;
    private DomainOwnershipStatus ownershipStatus;

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

    public DomainStatus getStatus() {
        return status;
    }

    public void setStatus(DomainStatus status) {
        this.status = status;
    }

    public DomainOwnershipStatus getOwnershipStatus() {
        return ownershipStatus;
    }

    public void setOwnershipStatus(DomainOwnershipStatus ownershipStatus) {
        this.ownershipStatus = ownershipStatus;
    }
}
