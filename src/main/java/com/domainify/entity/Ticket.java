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
        @Index(name = "idx_tickets_guest_email", columnList = "guest_email"),
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    /** Display name for unauthenticated submitters (when {@link #requester} is null). */
    @Column(name = "guest_name", length = 120)
    private String guestName;

    /** Normalized email for unauthenticated submitters. */
    @Column(name = "guest_email", length = 255)
    private String guestEmail;

    /** False until guest confirms email via magic link; unverified guests are hidden from inbox. */
    @Column(name = "guest_email_verified")
    private Boolean guestEmailVerified;

    /** SHA-256 hex of opaque access token (magic link). */
    @Column(name = "guest_access_token_hash", length = 64, unique = true)
    private String guestAccessTokenHash;

    @Column(name = "guest_access_token_expires_at")
    private Instant guestAccessTokenExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "due_at")
    private Instant dueAt;

    /** Deadline for first public staff reply. */
    @Column(name = "first_response_due_at")
    private Instant firstResponseDueAt;

    /** When the first public staff reply was posted. */
    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    /** When SLA clocks were paused (e.g. status PENDING waiting on customer). */
    @Column(name = "sla_paused_at")
    private Instant slaPausedAt;

    /** When an approaching-SLA warning notification was sent (cleared when due shifts). */
    @Column(name = "sla_warned_at")
    private Instant slaWarnedAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** When the ticket most recently entered RESOLVED (auto-close silence clock). */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "customer_last_read_at")
    private Instant customerLastReadAt;

    @Column(name = "staff_last_read_at")
    private Instant staffLastReadAt;

    /** Last public staff reply (not internal note). */
    @Column(name = "last_staff_public_reply_at")
    private Instant lastStaffPublicReplyAt;

    /** Last public customer reply. */
    @Column(name = "last_customer_public_reply_at")
    private Instant lastCustomerPublicReplyAt;

    /** When a no-reply reminder was sent to the customer. */
    @Column(name = "no_reply_reminded_at")
    private Instant noReplyRemindedAt;

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
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
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

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public boolean isGuestEmailVerified() {
        if (guestEmail == null || guestEmail.isBlank()) {
            return true;
        }
        return Boolean.TRUE.equals(guestEmailVerified);
    }

    public Boolean getGuestEmailVerified() {
        return guestEmailVerified;
    }

    public void setGuestEmailVerified(Boolean guestEmailVerified) {
        this.guestEmailVerified = guestEmailVerified;
    }

    public String getGuestAccessTokenHash() {
        return guestAccessTokenHash;
    }

    public void setGuestAccessTokenHash(String guestAccessTokenHash) {
        this.guestAccessTokenHash = guestAccessTokenHash;
    }

    public Instant getGuestAccessTokenExpiresAt() {
        return guestAccessTokenExpiresAt;
    }

    public void setGuestAccessTokenExpiresAt(Instant guestAccessTokenExpiresAt) {
        this.guestAccessTokenExpiresAt = guestAccessTokenExpiresAt;
    }

    public boolean isGuestTicket() {
        return guestEmail != null && !guestEmail.isBlank();
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

    public Instant getFirstResponseDueAt() {
        return firstResponseDueAt;
    }

    public void setFirstResponseDueAt(Instant firstResponseDueAt) {
        this.firstResponseDueAt = firstResponseDueAt;
    }

    public Instant getFirstRespondedAt() {
        return firstRespondedAt;
    }

    public void setFirstRespondedAt(Instant firstRespondedAt) {
        this.firstRespondedAt = firstRespondedAt;
    }

    public Instant getSlaPausedAt() {
        return slaPausedAt;
    }

    public void setSlaPausedAt(Instant slaPausedAt) {
        this.slaPausedAt = slaPausedAt;
    }

    public Instant getSlaWarnedAt() {
        return slaWarnedAt;
    }

    public void setSlaWarnedAt(Instant slaWarnedAt) {
        this.slaWarnedAt = slaWarnedAt;
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

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
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

    public Instant getLastStaffPublicReplyAt() {
        return lastStaffPublicReplyAt;
    }

    public void setLastStaffPublicReplyAt(Instant lastStaffPublicReplyAt) {
        this.lastStaffPublicReplyAt = lastStaffPublicReplyAt;
    }

    public Instant getLastCustomerPublicReplyAt() {
        return lastCustomerPublicReplyAt;
    }

    public void setLastCustomerPublicReplyAt(Instant lastCustomerPublicReplyAt) {
        this.lastCustomerPublicReplyAt = lastCustomerPublicReplyAt;
    }

    public Instant getNoReplyRemindedAt() {
        return noReplyRemindedAt;
    }

    public void setNoReplyRemindedAt(Instant noReplyRemindedAt) {
        this.noReplyRemindedAt = noReplyRemindedAt;
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
