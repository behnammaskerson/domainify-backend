package com.domainify.dto;

import com.domainify.entity.DomainOwnershipMethod;
import com.domainify.entity.DomainOwnershipStatus;

import java.time.Instant;

public class DomainOwnershipChallengeDto {

    private Long domainId;
    private String domainName;
    private DomainOwnershipStatus ownershipStatus;
    private DomainOwnershipMethod method;
    private String token;
    private Instant tokenExpiresAt;
    private String dnsHost;
    private String dnsType;
    private String dnsValue;
    private String httpUrl;
    private String httpBody;

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public DomainOwnershipStatus getOwnershipStatus() {
        return ownershipStatus;
    }

    public void setOwnershipStatus(DomainOwnershipStatus ownershipStatus) {
        this.ownershipStatus = ownershipStatus;
    }

    public DomainOwnershipMethod getMethod() {
        return method;
    }

    public void setMethod(DomainOwnershipMethod method) {
        this.method = method;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public String getDnsHost() {
        return dnsHost;
    }

    public void setDnsHost(String dnsHost) {
        this.dnsHost = dnsHost;
    }

    public String getDnsType() {
        return dnsType;
    }

    public void setDnsType(String dnsType) {
        this.dnsType = dnsType;
    }

    public String getDnsValue() {
        return dnsValue;
    }

    public void setDnsValue(String dnsValue) {
        this.dnsValue = dnsValue;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public String getHttpBody() {
        return httpBody;
    }

    public void setHttpBody(String httpBody) {
        this.httpBody = httpBody;
    }
}
