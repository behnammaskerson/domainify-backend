package com.domainify.dto;

import com.domainify.entity.NotificationType;
import com.domainify.entity.TicketStatus;

import java.time.Instant;

public class NotificationDto {
    private Long id;
    private NotificationType type;
    private boolean read;
    private Instant createdAt;
    private Instant readAt;
    private Long actorId;
    private String actorName;
    private Long ticketId;
    private String ticketPublicNumber;
    private String ticketSubject;
    private TicketStatus statusFrom;
    private TicketStatus statusTo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketPublicNumber() {
        return ticketPublicNumber;
    }

    public void setTicketPublicNumber(String ticketPublicNumber) {
        this.ticketPublicNumber = ticketPublicNumber;
    }

    public String getTicketSubject() {
        return ticketSubject;
    }

    public void setTicketSubject(String ticketSubject) {
        this.ticketSubject = ticketSubject;
    }

    public TicketStatus getStatusFrom() {
        return statusFrom;
    }

    public void setStatusFrom(TicketStatus statusFrom) {
        this.statusFrom = statusFrom;
    }

    public TicketStatus getStatusTo() {
        return statusTo;
    }

    public void setStatusTo(TicketStatus statusTo) {
        this.statusTo = statusTo;
    }
}
