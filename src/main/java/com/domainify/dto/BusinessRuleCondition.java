package com.domainify.dto;

/**
 * Represents a single condition in a business rule.
 * Conditions use the format: field operator value
 * Example: {"field": "priority", "operator": "EQUALS", "value": "URGENT"}
 */
public class BusinessRuleCondition {
    
    /**
     * The field to evaluate (status, priority, category, assigneeId, etc.).
     */
    private String field;

    /**
     * The comparison operator.
     */
    private BusinessRuleOperator operator;

    /**
     * The value to compare against (can be string, number, or boolean).
     */
    private Object value;

    public BusinessRuleCondition() {}

    public BusinessRuleCondition(String field, BusinessRuleOperator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public BusinessRuleOperator getOperator() {
        return operator;
    }

    public void setOperator(BusinessRuleOperator operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public enum BusinessRuleOperator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN_OR_EQUAL,
        CONTAINS,
        NOT_CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        IS_EMPTY,
        IS_NOT_EMPTY,
        IN,
        NOT_IN
    }
}