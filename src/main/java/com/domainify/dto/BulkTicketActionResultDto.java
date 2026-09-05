package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkTicketActionResultDto {

    private List<Long> succeeded = new ArrayList<>();
    private List<BulkTicketFailureDto> failed = new ArrayList<>();

    public BulkTicketActionResultDto() {
    }

    public BulkTicketActionResultDto(List<Long> succeeded, List<BulkTicketFailureDto> failed) {
        this.succeeded = succeeded != null ? succeeded : new ArrayList<>();
        this.failed = failed != null ? failed : new ArrayList<>();
    }

    public List<Long> getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(List<Long> succeeded) {
        this.succeeded = succeeded != null ? succeeded : new ArrayList<>();
    }

    public List<BulkTicketFailureDto> getFailed() {
        return failed;
    }

    public void setFailed(List<BulkTicketFailureDto> failed) {
        this.failed = failed != null ? failed : new ArrayList<>();
    }
}
