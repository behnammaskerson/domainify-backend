package com.domainify.dto;

import com.domainify.entity.DomainExpirySource;
import com.domainify.entity.DomainStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class UpsertDomainRequest {

    @NotBlank
    @Size(max = 253)
    private String name;

    @NotNull
    private DomainStatus status;

    @NotNull
    private Long categoryId;

    @NotNull
    @DecimalMin("0")
    private BigDecimal price;

    private LocalDate expiresAt;

    /** MANUAL (default) or REGISTRAR when filled from RDAP lookup. */
    private DomainExpirySource expiresSource;

    private String expiresRegistrar;

    /** Null/empty = use global admin renewal windows. Non-empty = custom day windows for this domain. */
    private List<Integer> renewalWindows;

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

    public String getExpiresRegistrar() {
        return expiresRegistrar;
    }

    public void setExpiresRegistrar(String expiresRegistrar) {
        this.expiresRegistrar = expiresRegistrar;
    }

    public List<Integer> getRenewalWindows() {
        return renewalWindows;
    }

    public void setRenewalWindows(List<Integer> renewalWindows) {
        this.renewalWindows = renewalWindows;
    }
}
