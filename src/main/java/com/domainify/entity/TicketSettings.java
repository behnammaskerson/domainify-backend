package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "ticket_settings")
public class TicketSettings {

    public static final long SINGLETON_ID = 1L;
    public static final int DEFAULT_REOPEN_WINDOW_DAYS = 14;
    public static final int DEFAULT_MAX_ATTACHMENTS = 5;
    public static final int DEFAULT_MAX_ATTACHMENT_SIZE_MB = 5;
    public static final String DEFAULT_ALLOWED_ATTACHMENT_KINDS = "IMAGE,PDF,LOG,DOCUMENT";

    @Id
    private Long id = SINGLETON_ID;

    /** Days after close during which a ticket may be reopened. */
    @Column(name = "reopen_window_days", nullable = false)
    private int reopenWindowDays = DEFAULT_REOPEN_WINDOW_DAYS;

    /** Max files allowed in a single create/reply upload. */
    @Column(name = "max_attachments", nullable = false, columnDefinition = "integer not null default 5")
    private int maxAttachments = DEFAULT_MAX_ATTACHMENTS;

    /** Max size per attachment file, in megabytes. */
    @Column(name = "max_attachment_size_mb", nullable = false, columnDefinition = "integer not null default 5")
    private int maxAttachmentSizeMb = DEFAULT_MAX_ATTACHMENT_SIZE_MB;

    /** Comma-separated {@link TicketAttachmentKind} names. */
    @Column(
            name = "allowed_attachment_kinds",
            nullable = false,
            length = 120,
            columnDefinition = "varchar(120) not null default 'IMAGE,PDF,LOG,DOCUMENT'"
    )
    private String allowedAttachmentKinds = DEFAULT_ALLOWED_ATTACHMENT_KINDS;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
        normalize();
    }

    public void normalize() {
        if (reopenWindowDays < 1) {
            reopenWindowDays = DEFAULT_REOPEN_WINDOW_DAYS;
        }
        if (maxAttachments < 1) {
            maxAttachments = DEFAULT_MAX_ATTACHMENTS;
        }
        if (maxAttachmentSizeMb < 1) {
            maxAttachmentSizeMb = DEFAULT_MAX_ATTACHMENT_SIZE_MB;
        }
        Set<TicketAttachmentKind> kinds = TicketAttachmentKind.parseCsv(allowedAttachmentKinds);
        if (kinds.isEmpty()) {
            kinds = EnumSet.allOf(TicketAttachmentKind.class);
        }
        allowedAttachmentKinds = TicketAttachmentKind.toCsv(kinds);
    }

    public static TicketSettings defaults() {
        TicketSettings settings = new TicketSettings();
        settings.setId(SINGLETON_ID);
        settings.setReopenWindowDays(DEFAULT_REOPEN_WINDOW_DAYS);
        settings.setMaxAttachments(DEFAULT_MAX_ATTACHMENTS);
        settings.setMaxAttachmentSizeMb(DEFAULT_MAX_ATTACHMENT_SIZE_MB);
        settings.setAllowedAttachmentKinds(DEFAULT_ALLOWED_ATTACHMENT_KINDS);
        settings.normalize();
        return settings;
    }

    public long maxAttachmentBytes() {
        return Math.max(1, maxAttachmentSizeMb) * 1024L * 1024L;
    }

    public Set<TicketAttachmentKind> resolvedAttachmentKinds() {
        Set<TicketAttachmentKind> kinds = TicketAttachmentKind.parseCsv(allowedAttachmentKinds);
        if (kinds.isEmpty()) {
            return EnumSet.allOf(TicketAttachmentKind.class);
        }
        return kinds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getReopenWindowDays() {
        return reopenWindowDays;
    }

    public void setReopenWindowDays(int reopenWindowDays) {
        this.reopenWindowDays = reopenWindowDays;
    }

    public int getMaxAttachments() {
        return maxAttachments;
    }

    public void setMaxAttachments(int maxAttachments) {
        this.maxAttachments = maxAttachments;
    }

    public int getMaxAttachmentSizeMb() {
        return maxAttachmentSizeMb;
    }

    public void setMaxAttachmentSizeMb(int maxAttachmentSizeMb) {
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
    }

    public String getAllowedAttachmentKinds() {
        return allowedAttachmentKinds;
    }

    public void setAllowedAttachmentKinds(String allowedAttachmentKinds) {
        this.allowedAttachmentKinds = allowedAttachmentKinds;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
