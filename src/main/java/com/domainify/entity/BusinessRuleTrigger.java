package com.domainify.entity;

/**
 * Defines when a business rule should be triggered.
 */
public enum BusinessRuleTrigger {
    /**
     * When a new ticket is created.
     */
    ON_CREATE,

    /**
     * When a ticket is updated (status, priority, assignee, category, etc.).
     */
    ON_UPDATE,

    /**
     * When a reply (public or internal) is added to a ticket.
     */
    ON_REPLY,

    /**
     * Periodically executed by a scheduler (for batch processing).
     */
    ON_SCHEDULE
}