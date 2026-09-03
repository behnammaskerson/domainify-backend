package com.domainify.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "email_config")
public class EmailConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false, length = 255)
    private String host = "";

    @Column(nullable = false)
    private int port = 587;

    @Column(length = 255)
    private String username = "";

    @Column(length = 512)
    private String password = "";

    @Column(nullable = false, length = 255)
    private String fromEmail = "";

    @Column(length = 255)
    private String fromName = "";

    @Column(nullable = false)
    private boolean useTls = true;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public static EmailConfig defaults() {
        EmailConfig config = new EmailConfig();
        config.setId(SINGLETON_ID);
        return config;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
