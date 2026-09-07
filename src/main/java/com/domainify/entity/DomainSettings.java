package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "domain_settings")
public class DomainSettings {

    public static final long SINGLETON_ID = 1L;
    public static final String DEFAULT_WINDOWS = "90,60,30";
    public static final int DEFAULT_SEND_HOUR = 9;
    public static final int DEFAULT_SEND_MINUTE = 0;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false)
    private boolean renewalRemindersEnabled = true;

    @Column(name = "renewal_in_app_enabled", nullable = false)
    private boolean renewalInAppEnabled = true;

    @Column(name = "renewal_email_enabled", nullable = false)
    private boolean renewalEmailEnabled = true;

    @Column(name = "renewal_sms_enabled", nullable = false)
    private boolean renewalSmsEnabled = true;

    /** Comma-separated day windows before expiry, e.g. {@code 90,60,30}. */
    @Column(name = "renewal_windows", nullable = false, length = 64)
    private String renewalWindows = DEFAULT_WINDOWS;

    /** Local server hour (0–23) when the daily renewal job should run. */
    @Column(name = "renewal_send_hour", nullable = false)
    private int renewalSendHour = DEFAULT_SEND_HOUR;

    /** Local server minute (0–59) when the daily renewal job should run. */
    @Column(name = "renewal_send_minute", nullable = false)
    private int renewalSendMinute = DEFAULT_SEND_MINUTE;

    /** Calendar date of the last successful daily run (prevents re-runs the same day). */
    @Column(name = "renewal_last_run_date")
    private LocalDate renewalLastRunDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public static DomainSettings defaults() {
        DomainSettings settings = new DomainSettings();
        settings.setId(SINGLETON_ID);
        return settings;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isRenewalRemindersEnabled() {
        return renewalRemindersEnabled;
    }

    public void setRenewalRemindersEnabled(boolean renewalRemindersEnabled) {
        this.renewalRemindersEnabled = renewalRemindersEnabled;
    }

    public boolean isRenewalInAppEnabled() {
        return renewalInAppEnabled;
    }

    public void setRenewalInAppEnabled(boolean renewalInAppEnabled) {
        this.renewalInAppEnabled = renewalInAppEnabled;
    }

    public boolean isRenewalEmailEnabled() {
        return renewalEmailEnabled;
    }

    public void setRenewalEmailEnabled(boolean renewalEmailEnabled) {
        this.renewalEmailEnabled = renewalEmailEnabled;
    }

    public boolean isRenewalSmsEnabled() {
        return renewalSmsEnabled;
    }

    public void setRenewalSmsEnabled(boolean renewalSmsEnabled) {
        this.renewalSmsEnabled = renewalSmsEnabled;
    }

    public String getRenewalWindows() {
        return renewalWindows;
    }

    public void setRenewalWindows(String renewalWindows) {
        this.renewalWindows = renewalWindows;
    }

    public int getRenewalSendHour() {
        return renewalSendHour;
    }

    public void setRenewalSendHour(int renewalSendHour) {
        this.renewalSendHour = renewalSendHour;
    }

    public int getRenewalSendMinute() {
        return renewalSendMinute;
    }

    public void setRenewalSendMinute(int renewalSendMinute) {
        this.renewalSendMinute = renewalSendMinute;
    }

    public LocalDate getRenewalLastRunDate() {
        return renewalLastRunDate;
    }

    public void setRenewalLastRunDate(LocalDate renewalLastRunDate) {
        this.renewalLastRunDate = renewalLastRunDate;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
