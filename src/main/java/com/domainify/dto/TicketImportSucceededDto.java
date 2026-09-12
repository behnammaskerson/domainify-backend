package com.domainify.dto;

public class TicketImportSucceededDto {

    private int rowNumber;
    private String externalId;
    private Long ticketId;
    private String publicNumber;

    public TicketImportSucceededDto() {
    }

    public TicketImportSucceededDto(int rowNumber, String externalId, Long ticketId, String publicNumber) {
        this.rowNumber = rowNumber;
        this.externalId = externalId;
        this.ticketId = ticketId;
        this.publicNumber = publicNumber;
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

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getPublicNumber() {
        return publicNumber;
    }

    public void setPublicNumber(String publicNumber) {
        this.publicNumber = publicNumber;
    }
}
