package com.domainify.dto;

import com.domainify.entity.BusinessRuleTrigger;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * DTO for business rule data transfer.
 */
public class BusinessRuleDto {

    private Long id;

    @NotBlank(message = "Rule name is required")
    @Size(max = 100, message = "Rule name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;

    @NotNull(message = "Trigger event is required")
    private BusinessRuleTrigger triggerEvent;

    @NotNull(message = "Priority is required")
    private Integer priority;

    @NotNull(message = "Conditions are required")
    @Size(min = 1, message = "At least one condition is required")
    private List<BusinessRuleCondition> conditions;

    @NotNull(message = "Actions are required")
    @Size(min = 1, message = "At least one action is required")
    private List<BusinessRuleAction> actions;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastExecutedAt;
    private Long executionCount;

    public BusinessRuleDto() {}

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

    public Boolean getEnabled() {
        return enabled;
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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public List<BusinessRuleCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<BusinessRuleCondition> conditions) {
        this.conditions = conditions;
    }

    public List<BusinessRuleAction> getActions() {
        return actions;
    }

    public void setActions(List<BusinessRuleAction> actions) {
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

    public Long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }
}