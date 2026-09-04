package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ticket_transfers", indexes = {
        @Index(name = "idx_ticket_transfers_ticket", columnList = "ticket_id"),
        @Index(name = "idx_ticket_transfers_created", columnList = "created_at")
})
public class TicketTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transferred_by_id", nullable = false)
    private User transferredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_assignee_id")
    private User fromAssignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_assignee_id")
    private User toAssignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_queue_id")
    private TicketQueue fromQueue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_queue_id")
    private TicketQueue toQueue;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public User getTransferredBy() {
        return transferredBy;
    }

    public void setTransferredBy(User transferredBy) {
        this.transferredBy = transferredBy;
    }

    public User getFromAssignee() {
        return fromAssignee;
    }

    public void setFromAssignee(User fromAssignee) {
        this.fromAssignee = fromAssignee;
    }

    public User getToAssignee() {
        return toAssignee;
    }

    public void setToAssignee(User toAssignee) {
        this.toAssignee = toAssignee;
    }

    public TicketQueue getFromQueue() {
        return fromQueue;
    }

    public void setFromQueue(TicketQueue fromQueue) {
        this.fromQueue = fromQueue;
    }

    public TicketQueue getToQueue() {
        return toQueue;
    }

    public void setToQueue(TicketQueue toQueue) {
        this.toQueue = toQueue;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
