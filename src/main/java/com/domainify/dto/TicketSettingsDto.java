package com.domainify.dto;

import com.domainify.entity.TicketAutoAssignMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class TicketSettingsDto {

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer reopenWindowDays;

    @NotNull
    @Min(1)
    @Max(20)
    private Integer maxAttachments;

    @NotNull
    @Min(1)
    @Max(50)
    private Integer maxAttachmentSizeMb;

    @NotEmpty
    @Size(max = 4)
    private List<String> allowedAttachmentKinds = new ArrayList<>();

    /** 0 disables auto-archive. */
    @NotNull
    @Min(0)
    @Max(3650)
    private Integer autoArchiveClosedAfterDays;

    @NotNull
    @Min(1)
    @Max(8760)
    private Integer slaUrgentHours;

    @NotNull
    @Min(1)
    @Max(8760)
    private Integer slaHighHours;

    @NotNull
    @Min(1)
    @Max(8760)
    private Integer slaMediumHours;

    @NotNull
    @Min(1)
    @Max(8760)
    private Integer slaLowHours;

    @NotNull
    private TicketAutoAssignMode autoAssignMode = TicketAutoAssignMode.OFF;

    @NotNull
    private Boolean autoAssignFallbackRoundRobin = true;

    @NotNull
    private Boolean ticketEmailNotificationsEnabled = true;

    @NotNull
    private Boolean ticketSmsNotificationsEnabled = true;

    @NotEmpty
    @Size(max = 4)
    private List<String> emailNotificationPriorities = new ArrayList<>();

    @NotEmpty
    @Size(max = 4)
    private List<String> smsNotificationPriorities = new ArrayList<>();

    public TicketSettingsDto() {
    }

    public TicketSettingsDto(
            Integer reopenWindowDays,
            Integer maxAttachments,
            Integer maxAttachmentSizeMb,
            List<String> allowedAttachmentKinds,
            Integer autoArchiveClosedAfterDays,
            Integer slaUrgentHours,
            Integer slaHighHours,
            Integer slaMediumHours,
            Integer slaLowHours,
            TicketAutoAssignMode autoAssignMode,
            Boolean autoAssignFallbackRoundRobin,
            Boolean ticketEmailNotificationsEnabled,
            Boolean ticketSmsNotificationsEnabled,
            List<String> emailNotificationPriorities,
            List<String> smsNotificationPriorities) {
        this.reopenWindowDays = reopenWindowDays;
        this.maxAttachments = maxAttachments;
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
        this.allowedAttachmentKinds = allowedAttachmentKinds != null ? allowedAttachmentKinds : new ArrayList<>();
        this.autoArchiveClosedAfterDays = autoArchiveClosedAfterDays;
        this.slaUrgentHours = slaUrgentHours;
        this.slaHighHours = slaHighHours;
        this.slaMediumHours = slaMediumHours;
        this.slaLowHours = slaLowHours;
        this.autoAssignMode = autoAssignMode != null ? autoAssignMode : TicketAutoAssignMode.OFF;
        this.autoAssignFallbackRoundRobin = autoAssignFallbackRoundRobin == null || autoAssignFallbackRoundRobin;
        this.ticketEmailNotificationsEnabled = ticketEmailNotificationsEnabled == null || ticketEmailNotificationsEnabled;
        this.ticketSmsNotificationsEnabled = ticketSmsNotificationsEnabled == null || ticketSmsNotificationsEnabled;
        this.emailNotificationPriorities = emailNotificationPriorities != null ? emailNotificationPriorities : new ArrayList<>();
        this.smsNotificationPriorities = smsNotificationPriorities != null ? smsNotificationPriorities : new ArrayList<>();
    }

    public Integer getReopenWindowDays() {
        return reopenWindowDays;
    }

    public void setReopenWindowDays(Integer reopenWindowDays) {
        this.reopenWindowDays = reopenWindowDays;
    }

    public Integer getMaxAttachments() {
        return maxAttachments;
    }

    public void setMaxAttachments(Integer maxAttachments) {
        this.maxAttachments = maxAttachments;
    }

    public Integer getMaxAttachmentSizeMb() {
        return maxAttachmentSizeMb;
    }

    public void setMaxAttachmentSizeMb(Integer maxAttachmentSizeMb) {
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
    }

    public List<String> getAllowedAttachmentKinds() {
        return allowedAttachmentKinds;
    }

    public void setAllowedAttachmentKinds(List<String> allowedAttachmentKinds) {
        this.allowedAttachmentKinds = allowedAttachmentKinds != null ? allowedAttachmentKinds : new ArrayList<>();
    }

    public Integer getAutoArchiveClosedAfterDays() {
        return autoArchiveClosedAfterDays;
    }

    public void setAutoArchiveClosedAfterDays(Integer autoArchiveClosedAfterDays) {
        this.autoArchiveClosedAfterDays = autoArchiveClosedAfterDays;
    }

    public Integer getSlaUrgentHours() {
        return slaUrgentHours;
    }

    public void setSlaUrgentHours(Integer slaUrgentHours) {
        this.slaUrgentHours = slaUrgentHours;
    }

    public Integer getSlaHighHours() {
        return slaHighHours;
    }

    public void setSlaHighHours(Integer slaHighHours) {
        this.slaHighHours = slaHighHours;
    }

    public Integer getSlaMediumHours() {
        return slaMediumHours;
    }

    public void setSlaMediumHours(Integer slaMediumHours) {
        this.slaMediumHours = slaMediumHours;
    }

    public Integer getSlaLowHours() {
        return slaLowHours;
    }

    public void setSlaLowHours(Integer slaLowHours) {
        this.slaLowHours = slaLowHours;
    }

    public TicketAutoAssignMode getAutoAssignMode() {
        return autoAssignMode;
    }

    public void setAutoAssignMode(TicketAutoAssignMode autoAssignMode) {
        this.autoAssignMode = autoAssignMode;
    }

    public Boolean getAutoAssignFallbackRoundRobin() {
        return autoAssignFallbackRoundRobin;
    }

    public void setAutoAssignFallbackRoundRobin(Boolean autoAssignFallbackRoundRobin) {
        this.autoAssignFallbackRoundRobin = autoAssignFallbackRoundRobin;
    }

    public Boolean getTicketEmailNotificationsEnabled() {
        return ticketEmailNotificationsEnabled;
    }

    public void setTicketEmailNotificationsEnabled(Boolean ticketEmailNotificationsEnabled) {
        this.ticketEmailNotificationsEnabled = ticketEmailNotificationsEnabled;
    }

    public Boolean getTicketSmsNotificationsEnabled() {
        return ticketSmsNotificationsEnabled;
    }

    public void setTicketSmsNotificationsEnabled(Boolean ticketSmsNotificationsEnabled) {
        this.ticketSmsNotificationsEnabled = ticketSmsNotificationsEnabled;
    }

    public List<String> getEmailNotificationPriorities() {
        return emailNotificationPriorities;
    }

    public void setEmailNotificationPriorities(List<String> emailNotificationPriorities) {
        this.emailNotificationPriorities = emailNotificationPriorities != null
                ? emailNotificationPriorities
                : new ArrayList<>();
    }

    public List<String> getSmsNotificationPriorities() {
        return smsNotificationPriorities;
    }

    public void setSmsNotificationPriorities(List<String> smsNotificationPriorities) {
        this.smsNotificationPriorities = smsNotificationPriorities != null
                ? smsNotificationPriorities
                : new ArrayList<>();
    }
}
