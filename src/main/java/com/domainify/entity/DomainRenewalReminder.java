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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "domain_renewal_reminders", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_domain_renewal_reminders_domain_window_expires",
                columnNames = {"domain_id", "window_days", "expires_at"})
}, indexes = {
        @Index(name = "idx_domain_renewal_reminders_domain", columnList = "domain_id"),
        @Index(name = "idx_domain_renewal_reminders_sent_at", columnList = "sent_at")
})
public class DomainRenewalReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @Column(name = "window_days", nullable = false)
    private int windowDays;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Column(name = "channels_sent", nullable = false, length = 64)
    private String channelsSent;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getChannelsSent() {
        return channelsSent;
    }

    public void setChannelsSent(String channelsSent) {
        this.channelsSent = channelsSent;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
