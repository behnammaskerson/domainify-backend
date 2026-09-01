package com.domainify.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sms_config")
public class SmsConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false, length = 512)
    private String serverUrl = "https://api.sms.ir/";

    @Column(length = 512)
    private String apiKey = "";

    @Column(length = 32)
    private String defaultLine;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public static SmsConfig defaults() {
        SmsConfig config = new SmsConfig();
        config.setId(SINGLETON_ID);
        return config;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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
