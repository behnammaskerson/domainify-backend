package com.domainify.dto;

public class TicketWorkloadRowDto {

    private Long agentId;
    private String name;
    private String email;
    private boolean available = true;
    private long openCount;

    public TicketWorkloadRowDto() {
    }

    public TicketWorkloadRowDto(Long agentId, String name, String email, boolean available, long openCount) {
        this.agentId = agentId;
        this.name = name;
        this.email = email;
        this.available = available;
        this.openCount = openCount;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
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

    public long getOpenCount() {
        return openCount;
    }

    public void setOpenCount(long openCount) {
        this.openCount = openCount;
    }
}
