package com.domainify.dto;

import com.domainify.entity.Domain;
import com.domainify.entity.DomainExpirySource;
import com.domainify.entity.DomainOwnershipMethod;
import com.domainify.entity.DomainOwnershipStatus;
import com.domainify.entity.DomainStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class DomainDto {

    private Long id;
    private String name;
    private DomainStatus status;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private BigDecimal price;
    private LocalDate expiresAt;
    private DomainExpirySource expiresSource;
    private Instant expiresCheckedAt;
    private String expiresRegistrar;
    private boolean customRenewalWindows;
    private String renewalWindows;
    private List<Integer> renewalWindowsParsed;
    private DomainOwnershipStatus ownershipStatus;
    private DomainOwnershipMethod ownershipMethod;
    private Instant ownershipVerifiedAt;
    private Instant ownershipTokenExpiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public DomainDto() {
    }

    public static DomainDto from(Domain domain) {
        DomainDto dto = new DomainDto();
        dto.id = domain.getId();
        dto.name = domain.getName();
        dto.status = domain.getStatus();
        if (domain.getCategory() != null) {
            dto.categoryId = domain.getCategory().getId();
            dto.categoryCode = domain.getCategory().getCode();
            dto.categoryName = domain.getCategory().getName();
        }
        dto.price = domain.getPrice();
        dto.expiresAt = domain.getExpiresAt();
        dto.expiresSource = domain.getExpiresSource() != null
                ? domain.getExpiresSource()
                : DomainExpirySource.MANUAL;
        dto.expiresCheckedAt = domain.getExpiresCheckedAt();
        dto.expiresRegistrar = domain.getExpiresRegistrar();
        String windowsCsv = domain.getRenewalWindows();
        dto.renewalWindows = windowsCsv;
        dto.customRenewalWindows = windowsCsv != null && !windowsCsv.isBlank();
        dto.renewalWindowsParsed = dto.customRenewalWindows
                ? parseWindowsCsv(windowsCsv)
                : Collections.emptyList();
        dto.ownershipStatus = domain.getOwnershipStatus() != null
                ? domain.getOwnershipStatus()
                : DomainOwnershipStatus.UNVERIFIED;
        dto.ownershipMethod = domain.getOwnershipMethod();
        dto.ownershipVerifiedAt = domain.getOwnershipVerifiedAt();
        if (domain.getOwnershipStatus() == DomainOwnershipStatus.PENDING) {
            dto.ownershipTokenExpiresAt = domain.getOwnershipTokenExpiresAt();
        }
        dto.createdAt = domain.getCreatedAt();
        dto.updatedAt = domain.getUpdatedAt();
        return dto;
    }

    private static List<Integer> parseWindowsCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(v -> v != null && v > 0 && v <= 3650)
                .distinct()
                .sorted(java.util.Comparator.reverseOrder())
                .toList();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public DomainExpirySource getExpiresSource() {
        return expiresSource;
    }

    public void setExpiresSource(DomainExpirySource expiresSource) {
        this.expiresSource = expiresSource;
    }

    public Instant getExpiresCheckedAt() {
        return expiresCheckedAt;
    }

    public void setExpiresCheckedAt(Instant expiresCheckedAt) {
        this.expiresCheckedAt = expiresCheckedAt;
    }

    public String getExpiresRegistrar() {
        return expiresRegistrar;
    }

    public void setExpiresRegistrar(String expiresRegistrar) {
        this.expiresRegistrar = expiresRegistrar;
    }

    public boolean isCustomRenewalWindows() {
        return customRenewalWindows;
    }

    public void setCustomRenewalWindows(boolean customRenewalWindows) {
        this.customRenewalWindows = customRenewalWindows;
    }

    public String getRenewalWindows() {
        return renewalWindows;
    }

    public void setRenewalWindows(String renewalWindows) {
        this.renewalWindows = renewalWindows;
    }

    public List<Integer> getRenewalWindowsParsed() {
        return renewalWindowsParsed;
    }

    public void setRenewalWindowsParsed(List<Integer> renewalWindowsParsed) {
        this.renewalWindowsParsed = renewalWindowsParsed;
    }

    public DomainOwnershipStatus getOwnershipStatus() {
        return ownershipStatus;
    }

    public void setOwnershipStatus(DomainOwnershipStatus ownershipStatus) {
        this.ownershipStatus = ownershipStatus;
    }

    public DomainOwnershipMethod getOwnershipMethod() {
        return ownershipMethod;
    }

    public void setOwnershipMethod(DomainOwnershipMethod ownershipMethod) {
        this.ownershipMethod = ownershipMethod;
    }

    public Instant getOwnershipVerifiedAt() {
        return ownershipVerifiedAt;
    }

    public void setOwnershipVerifiedAt(Instant ownershipVerifiedAt) {
        this.ownershipVerifiedAt = ownershipVerifiedAt;
    }

    public Instant getOwnershipTokenExpiresAt() {
        return ownershipTokenExpiresAt;
    }

    public void setOwnershipTokenExpiresAt(Instant ownershipTokenExpiresAt) {
        this.ownershipTokenExpiresAt = ownershipTokenExpiresAt;
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
