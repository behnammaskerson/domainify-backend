package com.domainify.dto;

public class TicketCategoryDto {

    private Long id;
    private String code;
    private String name;
    private boolean active;
    private int sortOrder;

    public TicketCategoryDto() {
    }

    public TicketCategoryDto(Long id, String code, String name, boolean active, int sortOrder) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
