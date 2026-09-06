package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "domains", uniqueConstraints = {
        @UniqueConstraint(name = "uk_domains_owner_name", columnNames = {"owner_id", "name"})
}, indexes = {
        @Index(name = "idx_domains_owner", columnList = "owner_id"),
        @Index(name = "idx_domains_status", columnList = "status"),
        @Index(name = "idx_domains_category", columnList = "category_id"),
        @Index(name = "idx_domains_ownership_status", columnList = "ownership_status"),
        @Index(name = "idx_domains_expires_at", columnList = "expires_at"),
        @Index(name = "idx_domains_name", columnList = "name")
})
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 253)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DomainStatus status = DomainStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private DomainCategory category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_status", nullable = false, length = 20)
    private DomainOwnershipStatus ownershipStatus = DomainOwnershipStatus.UNVERIFIED;

    @Column(name = "ownership_verified_at")
    private Instant ownershipVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_method", length = 20)
    private DomainOwnershipMethod ownershipMethod;

    @Column(name = "ownership_token", length = 64)
    private String ownershipToken;

    @Column(name = "ownership_token_expires_at")
    private Instant ownershipTokenExpiresAt;

    @Column(name = "ownership_last_checked_at")
    private Instant ownershipLastCheckedAt;

    @Column(name = "ownership_check_attempts", nullable = false)
    private int ownershipCheckAttempts = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (name != null) {
            name = name.trim().toLowerCase();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (name != null) {
            name = name.trim().toLowerCase();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DomainStatus getStatus() {
        return status;
    }

    public void setStatus(DomainStatus status) {
        this.status = status;
    }

    public DomainCategory getCategory() {
        return category;
    }

    public void setCategory(DomainCategory category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public DomainOwnershipStatus getOwnershipStatus() {
        return ownershipStatus;
    }

    public void setOwnershipStatus(DomainOwnershipStatus ownershipStatus) {
        this.ownershipStatus = ownershipStatus;
    }

    public Instant getOwnershipVerifiedAt() {
        return ownershipVerifiedAt;
    }

    public void setOwnershipVerifiedAt(Instant ownershipVerifiedAt) {
        this.ownershipVerifiedAt = ownershipVerifiedAt;
    }

    public DomainOwnershipMethod getOwnershipMethod() {
        return ownershipMethod;
    }

    public void setOwnershipMethod(DomainOwnershipMethod ownershipMethod) {
        this.ownershipMethod = ownershipMethod;
    }

    public String getOwnershipToken() {
        return ownershipToken;
    }

    public void setOwnershipToken(String ownershipToken) {
        this.ownershipToken = ownershipToken;
    }

    public Instant getOwnershipTokenExpiresAt() {
        return ownershipTokenExpiresAt;
    }

    public void setOwnershipTokenExpiresAt(Instant ownershipTokenExpiresAt) {
        this.ownershipTokenExpiresAt = ownershipTokenExpiresAt;
    }

    public Instant getOwnershipLastCheckedAt() {
        return ownershipLastCheckedAt;
    }

    public void setOwnershipLastCheckedAt(Instant ownershipLastCheckedAt) {
        this.ownershipLastCheckedAt = ownershipLastCheckedAt;
    }

    public int getOwnershipCheckAttempts() {
        return ownershipCheckAttempts;
    }

    public void setOwnershipCheckAttempts(int ownershipCheckAttempts) {
        this.ownershipCheckAttempts = ownershipCheckAttempts;
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
