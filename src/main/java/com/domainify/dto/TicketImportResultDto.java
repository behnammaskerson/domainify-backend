package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class TicketImportResultDto {

    private boolean dryRun;
    private int totalRows;
    private int validRows;
    private int importedCount;
    private int failedCount;
    private List<TicketImportSucceededDto> succeeded = new ArrayList<>();
    private List<TicketImportRowFailureDto> failed = new ArrayList<>();

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<TicketImportSucceededDto> getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(List<TicketImportSucceededDto> succeeded) {
        this.succeeded = succeeded != null ? succeeded : new ArrayList<>();
    }

    public List<TicketImportRowFailureDto> getFailed() {
        return failed;
    }

    public void setFailed(List<TicketImportRowFailureDto> failed) {
        this.failed = failed != null ? failed : new ArrayList<>();
    }
}
