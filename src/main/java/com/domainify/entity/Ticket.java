package com.domainify.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_public_number", columnList = "public_number", unique = true),
        @Index(name = "idx_tickets_requester", columnList = "requester_id"),
        @Index(name = "idx_tickets_status", columnList = "status"),
        @Index(name = "idx_tickets_assignee", columnList = "assignee_id"),
        @Index(name = "idx_tickets_queue", columnList = "queue_id"),
        @Index(name = "idx_tickets_due_at", columnList = "due_at"),
        @Index(name = "idx_tickets_escalated_at", columnList = "escalated_at")
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_number", nullable = false, unique = true, length = 32)
    private String publicNumber;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private TicketCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id")
    private TicketQueue queue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketStatus status = TicketStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketChannel channel = TicketChannel.PORTAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "customer_last_read_at")
    private Instant customerLastReadAt;

    @Column(name = "staff_last_read_at")
    private Instant staffLastReadAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_ticket_id")
    private Ticket mergedInto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_from_ticket_id")
    private Ticket splitFrom;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketAttachment> attachments = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ticket_tag_links",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TicketTag> tags = new HashSet<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = TicketStatus.NEW;
        }
        if (channel == null) {
            channel = TicketChannel.PORTAL;
        }
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

    public String getPublicNumber() {
        return publicNumber;
    }

    public void setPublicNumber(String publicNumber) {
        this.publicNumber = publicNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public TicketQueue getQueue() {
        return queue;
    }

    public void setQueue(TicketQueue queue) {
        this.queue = queue;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketChannel getChannel() {
        return channel;
    }

    public void setChannel(TicketChannel channel) {
        this.channel = channel;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(Instant escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public boolean isEscalated() {
        return escalatedAt != null;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Instant getCustomerLastReadAt() {
        return customerLastReadAt;
    }

    public void setCustomerLastReadAt(Instant customerLastReadAt) {
        this.customerLastReadAt = customerLastReadAt;
    }

    public Instant getStaffLastReadAt() {
        return staffLastReadAt;
    }

    public void setStaffLastReadAt(Instant staffLastReadAt) {
        this.staffLastReadAt = staffLastReadAt;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Ticket getMergedInto() {
        return mergedInto;
    }

    public void setMergedInto(Ticket mergedInto) {
        this.mergedInto = mergedInto;
    }

    public boolean isMerged() {
        return mergedInto != null;
    }

    public Ticket getSplitFrom() {
        return splitFrom;
    }

    public void setSplitFrom(Ticket splitFrom) {
        this.splitFrom = splitFrom;
    }

    public List<TicketAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<TicketAttachment> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public void addAttachment(TicketAttachment attachment) {
        attachments.add(attachment);
        attachment.setTicket(this);
    }

    public Set<TicketTag> getTags() {
        return tags;
    }

    public void setTags(Set<TicketTag> tags) {
        this.tags = tags != null ? tags : new HashSet<>();
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
