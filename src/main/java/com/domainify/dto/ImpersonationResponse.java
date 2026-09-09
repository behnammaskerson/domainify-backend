package com.domainify.dto;

import java.time.Instant;

public class ImpersonationResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long auditId;
    private Instant expiresAt;
    private UserDto user;
    private UserDto impersonator;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public UserDto getImpersonator() {
        return impersonator;
    }

    public void setImpersonator(UserDto impersonator) {
        this.impersonator = impersonator;
    }
}
