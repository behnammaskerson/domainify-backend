package com.domainify.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rule for automated ticket processing with if/then conditions and actions.
 */
@Entity
@Table(name = "business_rules")
public class BusinessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "enabled", nullable = false)
    @ColumnDefault("true")
    private Boolean enabled = true;

    /**
     * Rule trigger: when this rule should be evaluated.
     * - ON_CREATE: When a new ticket is created
     * - ON_UPDATE: When a ticket is updated (status, priority, assignee, etc.)
     * - ON_REPLY: When a reply is added to the ticket
     * - ON_SCHEDULE: Periodically (for batch processing)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false)
    private BusinessRuleTrigger triggerEvent;

    /**
     * Rule execution priority (lower number = higher priority).
     */
    @Column(name = "priority", nullable = false)
    @ColumnDefault("100")
    private Integer priority = 100;

    /**
     * Conditions that must ALL be met for the rule to execute (AND logic).
     * JSON array of condition objects.
     */
    @Column(name = "conditions", nullable = false, columnDefinition = "TEXT")
    private String conditions;

    /**
     * Actions to execute when all conditions are met.
     * JSON array of action objects.
     */
    @Column(name = "actions", nullable = false, columnDefinition = "TEXT")
    private String actions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /**
     * Last time this rule was executed successfully.
     */
    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    /**
     * Number of times this rule has been executed.
     */
    @Column(name = "execution_count", nullable = false)
    @ColumnDefault("0")
    private Long executionCount = 0L;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public BusinessRule() {}

    public BusinessRule(String name, String description, BusinessRuleTrigger triggerEvent,
                       String conditions, String actions) {
        this.name = name;
        this.description = description;
        this.triggerEvent = triggerEvent;
        this.conditions = conditions;
        this.actions = actions;
    }

    // Getters and setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BusinessRuleTrigger getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(BusinessRuleTrigger triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public int getPriority() {
        return priority != null ? priority : 100;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
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

    public Instant getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(Instant lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public long getExecutionCount() {
        return executionCount != null ? executionCount : 0L;
    }

    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }

    public void incrementExecutionCount() {
        this.executionCount = (this.executionCount != null ? this.executionCount : 0L) + 1;
        this.lastExecutedAt = Instant.now();
    }
}