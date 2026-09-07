package com.domainify.dto;

import java.time.Instant;
import java.time.LocalDate;

public class DomainRegistrarExpiryDto {

    private String domainName;
    private LocalDate expiresAt;
    private String registrar;
    private String rdapUrl;
    private Instant checkedAt;

    public DomainRegistrarExpiryDto() {
    }

    public DomainRegistrarExpiryDto(
            String domainName,
            LocalDate expiresAt,
            String registrar,
            String rdapUrl,
            Instant checkedAt) {
        this.domainName = domainName;
        this.expiresAt = expiresAt;
        this.registrar = registrar;
        this.rdapUrl = rdapUrl;
        this.checkedAt = checkedAt;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRegistrar() {
        return registrar;
    }

    public void setRegistrar(String registrar) {
        this.registrar = registrar;
    }

    public String getRdapUrl() {
        return rdapUrl;
    }

    public void setRdapUrl(String rdapUrl) {
        this.rdapUrl = rdapUrl;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}
