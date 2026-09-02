package com.domainify.dto;

public class TicketAttachmentDto {

    private Long id;
    private String fileName;
    private String contentType;
    private long sizeBytes;

    public TicketAttachmentDto() {
    }

    public TicketAttachmentDto(Long id, String fileName, String contentType, long sizeBytes) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
