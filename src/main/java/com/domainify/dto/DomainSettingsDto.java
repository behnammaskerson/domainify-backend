package com.domainify.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class DomainSettingsDto {

    private boolean renewalRemindersEnabled;
    private boolean renewalInAppEnabled;
    private boolean renewalEmailEnabled;
    private boolean renewalSmsEnabled;
    private String renewalWindows;
    private List<Integer> renewalWindowsParsed;
    private int renewalSendHour;
    private int renewalSendMinute;
    private LocalDate renewalLastRunDate;
    private Instant updatedAt;

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

    public List<Integer> getRenewalWindowsParsed() {
        return renewalWindowsParsed;
    }

    public void setRenewalWindowsParsed(List<Integer> renewalWindowsParsed) {
        this.renewalWindowsParsed = renewalWindowsParsed;
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
