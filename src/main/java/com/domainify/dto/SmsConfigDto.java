package com.domainify.dto;

import java.time.Instant;

public class SmsConfigDto {

    private String serverUrl;
    private boolean apiKeyConfigured;
    private String defaultLine;
    private Instant updatedAt;

    public SmsConfigDto() {
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public boolean isApiKeyConfigured() {
        return apiKeyConfigured;
    }

    public void setApiKeyConfigured(boolean apiKeyConfigured) {
        this.apiKeyConfigured = apiKeyConfigured;
    }

    public String getDefaultLine() {
        return defaultLine;
    }

    public void setDefaultLine(String defaultLine) {
        this.defaultLine = defaultLine;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
