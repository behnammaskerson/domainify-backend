package com.domainify.dto;

public class TicketReplyTemplateDto {

    private Long id;
    private String title;
    private String body;
    private boolean active;
    private int sortOrder;

    public TicketReplyTemplateDto() {
    }

    public TicketReplyTemplateDto(Long id, String title, String body, boolean active, int sortOrder) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
