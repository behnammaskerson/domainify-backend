package com.domainify.dto;

import java.util.Map;

/**
 * Represents a single action to execute when business rule conditions are met.
 * Actions use the format: actionType with parameters
 * Example: {"actionType": "SET_PRIORITY", "parameters": {"priority": "HIGH"}}
 */
public class BusinessRuleAction {
    
    /**
     * The type of action to execute.
     */
    private BusinessRuleActionType actionType;

    /**
     * Parameters for the action (varies by action type).
     */
    private Map<String, Object> parameters;

    public BusinessRuleAction() {}

    public BusinessRuleAction(BusinessRuleActionType actionType, Map<String, Object> parameters) {
        this.actionType = actionType;
        this.parameters = parameters;
    }

    public BusinessRuleActionType getActionType() {
        return actionType;
    }

    public void setActionType(BusinessRuleActionType actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public enum BusinessRuleActionType {
        /**
         * Set ticket status.
         * Parameters: status (string)
         */
        SET_STATUS,

        /**
         * Set ticket priority.
         * Parameters: priority (string: LOW, MEDIUM, HIGH, URGENT)
         */
        SET_PRIORITY,

        /**
         * Assign ticket to user.
         * Parameters: assigneeId (number) or assigneeEmail (string)
         */
        ASSIGN_TO_USER,

        /**
         * Assign ticket to queue.
         * Parameters: queueId (number) or queueName (string)
         */
        ASSIGN_TO_QUEUE,

        /**
         * Add tags to ticket.
         * Parameters: tags (array of strings)
         */
        ADD_TAGS,

        /**
         * Remove tags from ticket.
         * Parameters: tags (array of strings)
         */
        REMOVE_TAGS,

        /**
         * Set ticket category.
         * Parameters: categoryId (number) or categoryName (string)
         */
        SET_CATEGORY,

        /**
         * Add internal note.
         * Parameters: note (string)
         */
        ADD_INTERNAL_NOTE,

        /**
         * Send email notification.
         * Parameters: recipientType (CUSTOMER, ASSIGNEE, ADMIN), subject (string), body (string)
         */
        SEND_EMAIL_NOTIFICATION,

        /**
         * Send SMS notification.
         * Parameters: recipientType (CUSTOMER, ASSIGNEE, ADMIN), message (string)
         */
        SEND_SMS_NOTIFICATION,

        /**
         * Set due date.
         * Parameters: dueDate (ISO string) or daysFromNow (number)
         */
        SET_DUE_DATE,

        /**
         * Escalate ticket.
         * Parameters: escalateTo (user ID or queue ID), reason (string)
         */
        ESCALATE_TICKET
    }
}