package com.domainify.dto;

public class TicketImportRowFailureDto {

    private int rowNumber;
    private String externalId;
    private String code;
    private String message;

    public TicketImportRowFailureDto() {
    }

    public TicketImportRowFailureDto(int rowNumber, String externalId, String code, String message) {
        this.rowNumber = rowNumber;
        this.externalId = externalId;
        this.code = code;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
