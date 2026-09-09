package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "ticket_categories", indexes = {
        @Index(name = "idx_ticket_categories_code", columnList = "code", unique = true),
        @Index(name = "idx_ticket_categories_active", columnList = "active")
})
public class TicketCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    /** When false, ticket emails are not sent for tickets in this category. */
    @ColumnDefault("true")
    @Column(name = "email_notifications_enabled", nullable = true)
    private Boolean emailNotificationsEnabled = true;

    /** When false, ticket SMS alerts are not sent for tickets in this category. */
    @ColumnDefault("true")
    @Column(name = "sms_notifications_enabled", nullable = true)
    private Boolean smsNotificationsEnabled = true;

    /** Optional first-response SLA hour overrides by priority (null = inherit org). */
    @Column(name = "first_response_sla_urgent_hours")
    private Integer firstResponseSlaUrgentHours;

    @Column(name = "first_response_sla_high_hours")
    private Integer firstResponseSlaHighHours;

    @Column(name = "first_response_sla_medium_hours")
    private Integer firstResponseSlaMediumHours;

    @Column(name = "first_response_sla_low_hours")
    private Integer firstResponseSlaLowHours;

    /** Optional resolve SLA hour overrides by priority (null = inherit org). */
    @Column(name = "resolve_sla_urgent_hours")
    private Integer resolveSlaUrgentHours;

    @Column(name = "resolve_sla_high_hours")
    private Integer resolveSlaHighHours;

    @Column(name = "resolve_sla_medium_hours")
    private Integer resolveSlaMediumHours;

    @Column(name = "resolve_sla_low_hours")
    private Integer resolveSlaLowHours;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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
        return emailNotificationsEnabled == null || Boolean.TRUE.equals(emailNotificationsEnabled);
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public boolean isSmsNotificationsEnabled() {
        return smsNotificationsEnabled == null || Boolean.TRUE.equals(smsNotificationsEnabled);
    }

    public void setSmsNotificationsEnabled(boolean smsNotificationsEnabled) {
        this.smsNotificationsEnabled = smsNotificationsEnabled;
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

    public Integer firstResponseSlaHoursFor(TicketPriority priority) {
        if (priority == null) {
            return null;
        }
        return switch (priority) {
            case URGENT -> firstResponseSlaUrgentHours;
            case HIGH -> firstResponseSlaHighHours;
            case MEDIUM -> firstResponseSlaMediumHours;
            case LOW -> firstResponseSlaLowHours;
        };
    }

    public Integer resolveSlaHoursFor(TicketPriority priority) {
        if (priority == null) {
            return null;
        }
        return switch (priority) {
            case URGENT -> resolveSlaUrgentHours;
            case HIGH -> resolveSlaHighHours;
            case MEDIUM -> resolveSlaMediumHours;
            case LOW -> resolveSlaLowHours;
        };
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
