package com.domainify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class DomainSettingsUpdateRequest {

    @NotNull
    private Boolean renewalRemindersEnabled;

    @NotNull
    private Boolean renewalInAppEnabled;

    @NotNull
    private Boolean renewalEmailEnabled;

    @NotNull
    private Boolean renewalSmsEnabled;

    @NotEmpty
    private List<@NotNull @Min(1) @Max(3650) Integer> renewalWindows;

    @NotNull
    @Min(0)
    @Max(23)
    private Integer renewalSendHour;

    @NotNull
    @Min(0)
    @Max(59)
    private Integer renewalSendMinute;

    public Boolean getRenewalRemindersEnabled() {
        return renewalRemindersEnabled;
    }

    public void setRenewalRemindersEnabled(Boolean renewalRemindersEnabled) {
        this.renewalRemindersEnabled = renewalRemindersEnabled;
    }

    public Boolean getRenewalInAppEnabled() {
        return renewalInAppEnabled;
    }

    public void setRenewalInAppEnabled(Boolean renewalInAppEnabled) {
        this.renewalInAppEnabled = renewalInAppEnabled;
    }

    public Boolean getRenewalEmailEnabled() {
        return renewalEmailEnabled;
    }

    public void setRenewalEmailEnabled(Boolean renewalEmailEnabled) {
        this.renewalEmailEnabled = renewalEmailEnabled;
    }

    public Boolean getRenewalSmsEnabled() {
        return renewalSmsEnabled;
    }

    public void setRenewalSmsEnabled(Boolean renewalSmsEnabled) {
        this.renewalSmsEnabled = renewalSmsEnabled;
    }

    public List<Integer> getRenewalWindows() {
        return renewalWindows;
    }

    public void setRenewalWindows(List<Integer> renewalWindows) {
        this.renewalWindows = renewalWindows;
    }

    public Integer getRenewalSendHour() {
        return renewalSendHour;
    }

    public void setRenewalSendHour(Integer renewalSendHour) {
        this.renewalSendHour = renewalSendHour;
    }

    public Integer getRenewalSendMinute() {
        return renewalSendMinute;
    }

    public void setRenewalSendMinute(Integer renewalSendMinute) {
        this.renewalSendMinute = renewalSendMinute;
    }
}
