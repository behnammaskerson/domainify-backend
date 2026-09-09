package com.domainify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TicketCategoryRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 64)
    private String code;

    private Boolean active;

    private Boolean emailNotificationsEnabled;

    private Boolean smsNotificationsEnabled;

    private Integer sortOrder;

    @Min(1)
    @Max(8760)
    private Integer firstResponseSlaUrgentHours;

    @Min(1)
    @Max(8760)
    private Integer firstResponseSlaHighHours;

    @Min(1)
    @Max(8760)
    private Integer firstResponseSlaMediumHours;

    @Min(1)
    @Max(8760)
    private Integer firstResponseSlaLowHours;

    @Min(1)
    @Max(8760)
    private Integer resolveSlaUrgentHours;

    @Min(1)
    @Max(8760)
    private Integer resolveSlaHighHours;

    @Min(1)
    @Max(8760)
    private Integer resolveSlaMediumHours;

    @Min(1)
    @Max(8760)
    private Integer resolveSlaLowHours;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public Boolean getSmsNotificationsEnabled() {
        return smsNotificationsEnabled;
    }

    public void setSmsNotificationsEnabled(Boolean smsNotificationsEnabled) {
        this.smsNotificationsEnabled = smsNotificationsEnabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
