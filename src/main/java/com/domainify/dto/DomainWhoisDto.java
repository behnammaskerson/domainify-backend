package com.domainify.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Full domain registration (RDAP) snapshot. */
public class DomainWhoisDto {

    private String domainName;
    private String ldhName;
    private String unicodeName;
    private List<String> statuses = new ArrayList<>();
    private LocalDate registeredAt;
    private LocalDate updatedAt;
    private LocalDate expiresAt;
    private String registrar;
    private String registrarIanaId;
    private String registrarUrl;
    private String registrarEmail;
    private String registrantName;
    private String registrantOrganization;
    private String registrantCountry;
    private String registrantEmail;
    private List<String> nameServers = new ArrayList<>();
    private boolean dnssecSigned;
    private String rdapUrl;
    private Instant checkedAt;
    private boolean available;

    public DomainWhoisDto() {
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getLdhName() {
        return ldhName;
    }

    public void setLdhName(String ldhName) {
        this.ldhName = ldhName;
    }

    public String getUnicodeName() {
        return unicodeName;
    }

    public void setUnicodeName(String unicodeName) {
        this.unicodeName = unicodeName;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<String> statuses) {
        this.statuses = statuses != null ? statuses : new ArrayList<>();
    }

    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDate registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getRegistrarIanaId() {
        return registrarIanaId;
    }

    public void setRegistrarIanaId(String registrarIanaId) {
        this.registrarIanaId = registrarIanaId;
    }

    public String getRegistrarUrl() {
        return registrarUrl;
    }

    public void setRegistrarUrl(String registrarUrl) {
        this.registrarUrl = registrarUrl;
    }

    public String getRegistrarEmail() {
        return registrarEmail;
    }

    public void setRegistrarEmail(String registrarEmail) {
        this.registrarEmail = registrarEmail;
    }

    public String getRegistrantName() {
        return registrantName;
    }

    public void setRegistrantName(String registrantName) {
        this.registrantName = registrantName;
    }

    public String getRegistrantOrganization() {
        return registrantOrganization;
    }

    public void setRegistrantOrganization(String registrantOrganization) {
        this.registrantOrganization = registrantOrganization;
    }

    public String getRegistrantCountry() {
        return registrantCountry;
    }

    public void setRegistrantCountry(String registrantCountry) {
        this.registrantCountry = registrantCountry;
    }

    public String getRegistrantEmail() {
        return registrantEmail;
    }

    public void setRegistrantEmail(String registrantEmail) {
        this.registrantEmail = registrantEmail;
    }

    public List<String> getNameServers() {
        return nameServers;
    }

    public void setNameServers(List<String> nameServers) {
        this.nameServers = nameServers != null ? nameServers : new ArrayList<>();
    }

    public boolean isDnssecSigned() {
        return dnssecSigned;
    }

    public void setDnssecSigned(boolean dnssecSigned) {
        this.dnssecSigned = dnssecSigned;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
