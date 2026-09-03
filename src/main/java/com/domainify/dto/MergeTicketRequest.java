package com.domainify.dto;

public class MergeTicketRequest {

    private Long sourceTicketId;
    private String sourcePublicNumber;

    public Long getSourceTicketId() {
        return sourceTicketId;
    }

    public void setSourceTicketId(Long sourceTicketId) {
        this.sourceTicketId = sourceTicketId;
    }

    public String getSourcePublicNumber() {
        return sourcePublicNumber;
    }

    public void setSourcePublicNumber(String sourcePublicNumber) {
        this.sourcePublicNumber = sourcePublicNumber;
    }

    public boolean isSourceProvided() {
        return sourceTicketId != null
                || (sourcePublicNumber != null && !sourcePublicNumber.isBlank());
    }
}
