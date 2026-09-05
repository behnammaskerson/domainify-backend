package com.domainify.dto;

public class BulkTicketFailureDto {

    private Long ticketId;
    private String code;
    private String message;

    public BulkTicketFailureDto() {
    }

    public BulkTicketFailureDto(Long ticketId, String code, String message) {
        this.ticketId = ticketId;
        this.code = code;
        this.message = message;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
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
