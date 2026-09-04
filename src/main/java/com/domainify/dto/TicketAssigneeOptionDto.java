package com.domainify.dto;

public class TicketAssigneeOptionDto {

    private Long id;
    private String name;
    private String email;
    private boolean available = true;

    public TicketAssigneeOptionDto() {
    }

    public TicketAssigneeOptionDto(Long id, String name, String email) {
        this(id, name, email, true);
    }

    public TicketAssigneeOptionDto(Long id, String name, String email, boolean available) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
