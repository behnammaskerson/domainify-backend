package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class TicketQueueDto {

    private Long id;
    private String code;
    private String name;
    private boolean active;
    private int sortOrder;
    private List<Long> agentIds = new ArrayList<>();

    public TicketQueueDto() {
    }

    public TicketQueueDto(Long id, String code, String name, boolean active, int sortOrder) {
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

    public List<Long> getAgentIds() {
        return agentIds;
    }

    public void setAgentIds(List<Long> agentIds) {
        this.agentIds = agentIds != null ? agentIds : new ArrayList<>();
    }
}
