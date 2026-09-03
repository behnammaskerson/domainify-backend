package com.domainify.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ticket_messages", indexes = {
        @Index(name = "idx_ticket_messages_ticket", columnList = "ticket_id"),
        @Index(name = "idx_ticket_messages_created", columnList = "created_at")
})
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** Internal notes are hidden from customers; portal replies are always public. */
    @Column(nullable = false)
    private boolean internalNote = false;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<TicketMessageAttachment> attachments = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    private Instant editedAt;

    private Instant deletedAt;

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

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isInternalNote() {
        return internalNote;
    }

    public void setInternalNote(boolean internalNote) {
        this.internalNote = internalNote;
    }

    public List<TicketMessageAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<TicketMessageAttachment> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public void addAttachment(TicketMessageAttachment attachment) {
        attachments.add(attachment);
        attachment.setMessage(this);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Instant editedAt) {
        this.editedAt = editedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
