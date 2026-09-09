package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class TicketCategoryDto {

    private Long id;
    private String code;
    private String name;
    private boolean active;
    private boolean emailNotificationsEnabled = true;
    private boolean smsNotificationsEnabled = true;
    private int sortOrder;
    private List<Long> agentIds = new ArrayList<>();

    private Integer firstResponseSlaUrgentHours;
    private Integer firstResponseSlaHighHours;
    private Integer firstResponseSlaMediumHours;
    private Integer firstResponseSlaLowHours;
    private Integer resolveSlaUrgentHours;
    private Integer resolveSlaHighHours;
    private Integer resolveSlaMediumHours;
    private Integer resolveSlaLowHours;

    public TicketCategoryDto() {
    }

    public TicketCategoryDto(
            Long id,
            String code,
            String name,
            boolean active,
            boolean emailNotificationsEnabled,
            boolean smsNotificationsEnabled,
            int sortOrder) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
        this.emailNotificationsEnabled = emailNotificationsEnabled;
        this.smsNotificationsEnabled = smsNotificationsEnabled;
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

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public boolean isSmsNotificationsEnabled() {
        return smsNotificationsEnabled;
    }

    public void setSmsNotificationsEnabled(boolean smsNotificationsEnabled) {
        this.smsNotificationsEnabled = smsNotificationsEnabled;
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

    public Integer getFirstResponseSlaUrgentHours() {
        return firstResponseSlaUrgentHours;
    }

    public void setFirstResponseSlaUrgentHours(Integer firstResponseSlaUrgentHours) {
        this.firstResponseSlaUrgentHours = firstResponseSlaUrgentHours;
    }

    public Integer getFirstResponseSlaHighHours() {
        return firstResponseSlaHighHours;
    }

    public void setFirstResponseSlaHighHours(Integer firstResponseSlaHighHours) {
        this.firstResponseSlaHighHours = firstResponseSlaHighHours;
    }

    public Integer getFirstResponseSlaMediumHours() {
        return firstResponseSlaMediumHours;
    }

    public void setFirstResponseSlaMediumHours(Integer firstResponseSlaMediumHours) {
        this.firstResponseSlaMediumHours = firstResponseSlaMediumHours;
    }

    public Integer getFirstResponseSlaLowHours() {
        return firstResponseSlaLowHours;
    }

    public void setFirstResponseSlaLowHours(Integer firstResponseSlaLowHours) {
        this.firstResponseSlaLowHours = firstResponseSlaLowHours;
    }

    public Integer getResolveSlaUrgentHours() {
        return resolveSlaUrgentHours;
    }

    public void setResolveSlaUrgentHours(Integer resolveSlaUrgentHours) {
        this.resolveSlaUrgentHours = resolveSlaUrgentHours;
    }

    public Integer getResolveSlaHighHours() {
        return resolveSlaHighHours;
    }

    public void setResolveSlaHighHours(Integer resolveSlaHighHours) {
        this.resolveSlaHighHours = resolveSlaHighHours;
    }

    public Integer getResolveSlaMediumHours() {
        return resolveSlaMediumHours;
    }

    public void setResolveSlaMediumHours(Integer resolveSlaMediumHours) {
        this.resolveSlaMediumHours = resolveSlaMediumHours;
    }

    public Integer getResolveSlaLowHours() {
        return resolveSlaLowHours;
    }

    public void setResolveSlaLowHours(Integer resolveSlaLowHours) {
        this.resolveSlaLowHours = resolveSlaLowHours;
    }
}
