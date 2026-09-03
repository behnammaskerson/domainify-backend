package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ticket_settings")
public class TicketSettings {

    public static final long SINGLETON_ID = 1L;
    public static final int DEFAULT_REOPEN_WINDOW_DAYS = 14;

    @Id
    private Long id = SINGLETON_ID;

    /** Days after close during which a ticket may be reopened. */
    @Column(name = "reopen_window_days", nullable = false)
    private int reopenWindowDays = DEFAULT_REOPEN_WINDOW_DAYS;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public static TicketSettings defaults() {
        TicketSettings settings = new TicketSettings();
        settings.setId(SINGLETON_ID);
        settings.setReopenWindowDays(DEFAULT_REOPEN_WINDOW_DAYS);
        return settings;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
