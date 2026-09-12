package com.domainify.entity;

public enum TicketChannel {
    PORTAL,
    EMAIL,
    /** Unauthenticated app support form. */
    WEB,
    /** Landing contact form intake. */
    CONTACT,
    /** Agent-opened ticket on behalf of a customer. */
    OUTBOUND
}
