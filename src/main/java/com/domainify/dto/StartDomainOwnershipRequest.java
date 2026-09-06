package com.domainify.dto;

import com.domainify.entity.DomainOwnershipMethod;
import jakarta.validation.constraints.NotNull;

public class StartDomainOwnershipRequest {

    @NotNull
    private DomainOwnershipMethod method;

    public DomainOwnershipMethod getMethod() {
        return method;
    }

    public void setMethod(DomainOwnershipMethod method) {
        this.method = method;
    }
}
