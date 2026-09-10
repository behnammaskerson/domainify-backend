package com.domainify.dto;

import com.domainify.entity.TicketAutoAssignMode;
import com.domainify.entity.TicketNoReplyAction;
import com.domainify.entity.TicketPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
    @Min(1)
    @Max(8760)
    private Integer firstResponseSlaUrgentHours;

    @NotNull
    @Min(1)
    @Max(8760)
    private Integer firstResponseSlaHighHours;

    @NotNull
    @Min(1)
    @Max(8760)
    private Integer firstResponseSlaMediumHours;

    @NotNull
    @Min(1)
    @Max(8760)
    private Integer firstResponseSlaLowHours;

    @NotNull
    private TicketAutoAssignMode autoAssignMode = TicketAutoAssignMode.OFF;

    @NotNull
    private Boolean autoAssignFallbackRoundRobin = true;

    private Long defaultQueueId;

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

    @NotNull
    private Boolean agentDigestEnabled = false;

    @NotNull
    @Min(0)
    @Max(23)
    private Integer agentDigestSendHour = 8;

    @NotNull
    @Min(0)
    @Max(59)
    private Integer agentDigestSendMinute = 0;

    @NotNull
    private Boolean slaUseBusinessHours = false;

    @NotBlank
    @Size(max = 64)
    private String slaTimezone = "UTC";

    private BusinessHoursWeekDto businessHours;

    private List<BusinessHolidayDto> businessHolidays = new ArrayList<>();

    @NotNull
    private Boolean slaWarnEnabled = false;

    @Min(1)
    @Max(8760)
    private Integer slaWarnHoursBefore = 2;

    @NotNull
    private Boolean slaBreachEscalationEnabled = true;

    @NotNull
    private Boolean slaBreachBumpPriority = true;

    private Long slaBreachAssigneeId;

    private Long slaBreachQueueId;

    private TicketPriority automationDefaultPriority;

    @NotNull
    private Boolean automationCustomerAckEnabled = true;

    @NotNull
    private Boolean automationNoReplyEnabled = false;

    @NotNull
    @Min(1)
    @Max(8760)
    private Integer automationNoReplyHours = 48;

    @NotNull
    private TicketNoReplyAction automationNoReplyAction = TicketNoReplyAction.REMIND;

    @NotNull
    private Boolean automationCsatInviteEnabled = true;

    @NotNull
    private Boolean automationAutoCloseEnabled = false;

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer automationAutoCloseDays = 7;

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
            Integer firstResponseSlaUrgentHours,
            Integer firstResponseSlaHighHours,
            Integer firstResponseSlaMediumHours,
            Integer firstResponseSlaLowHours,
            TicketAutoAssignMode autoAssignMode,
            Boolean autoAssignFallbackRoundRobin,
            Boolean ticketEmailNotificationsEnabled,
            Boolean ticketSmsNotificationsEnabled,
            List<String> emailNotificationPriorities,
            List<String> smsNotificationPriorities,
            Boolean agentDigestEnabled,
            Integer agentDigestSendHour,
            Integer agentDigestSendMinute) {
        this.reopenWindowDays = reopenWindowDays;
        this.maxAttachments = maxAttachments;
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
        this.allowedAttachmentKinds = allowedAttachmentKinds != null ? allowedAttachmentKinds : new ArrayList<>();
        this.autoArchiveClosedAfterDays = autoArchiveClosedAfterDays;
        this.slaUrgentHours = slaUrgentHours;
        this.slaHighHours = slaHighHours;
        this.slaMediumHours = slaMediumHours;
        this.slaLowHours = slaLowHours;
        this.firstResponseSlaUrgentHours = firstResponseSlaUrgentHours;
        this.firstResponseSlaHighHours = firstResponseSlaHighHours;
        this.firstResponseSlaMediumHours = firstResponseSlaMediumHours;
        this.firstResponseSlaLowHours = firstResponseSlaLowHours;
        this.autoAssignMode = autoAssignMode != null ? autoAssignMode : TicketAutoAssignMode.OFF;
        this.autoAssignFallbackRoundRobin = autoAssignFallbackRoundRobin == null || autoAssignFallbackRoundRobin;
        this.ticketEmailNotificationsEnabled = ticketEmailNotificationsEnabled == null || ticketEmailNotificationsEnabled;
        this.ticketSmsNotificationsEnabled = ticketSmsNotificationsEnabled == null || ticketSmsNotificationsEnabled;
        this.emailNotificationPriorities = emailNotificationPriorities != null ? emailNotificationPriorities : new ArrayList<>();
        this.smsNotificationPriorities = smsNotificationPriorities != null ? smsNotificationPriorities : new ArrayList<>();
        this.agentDigestEnabled = Boolean.TRUE.equals(agentDigestEnabled);
        this.agentDigestSendHour = agentDigestSendHour != null ? agentDigestSendHour : 8;
        this.agentDigestSendMinute = agentDigestSendMinute != null ? agentDigestSendMinute : 0;
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

    public Integer getFirstResponseSlaUrgentHours() {
        return firstResponseSlaUrgentHours;
    }

    public void setFirstResponseSlaUrgentHours(Integer firstResponseSlaUrgentHours) {
        this.firstResponseSlaUrgentHours = firstResponseSlaUrgentHours;
    }

    public Integer getFirstResponseSlaHighHours() {
        return firstResponseSlaHighHours;
    }

    public void setFirstResponseSlaHighHours(Integer firstResponseSlaHighHours) {
        this.firstResponseSlaHighHours = firstResponseSlaHighHours;
    }

    public Integer getFirstResponseSlaMediumHours() {
        return firstResponseSlaMediumHours;
    }

    public void setFirstResponseSlaMediumHours(Integer firstResponseSlaMediumHours) {
        this.firstResponseSlaMediumHours = firstResponseSlaMediumHours;
    }

    public Integer getFirstResponseSlaLowHours() {
        return firstResponseSlaLowHours;
    }

    public void setFirstResponseSlaLowHours(Integer firstResponseSlaLowHours) {
        this.firstResponseSlaLowHours = firstResponseSlaLowHours;
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

    public Long getDefaultQueueId() {
        return defaultQueueId;
    }

    public void setDefaultQueueId(Long defaultQueueId) {
        this.defaultQueueId = defaultQueueId;
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

    public Boolean getAgentDigestEnabled() {
        return agentDigestEnabled;
    }

    public void setAgentDigestEnabled(Boolean agentDigestEnabled) {
        this.agentDigestEnabled = agentDigestEnabled;
    }

    public Integer getAgentDigestSendHour() {
        return agentDigestSendHour;
    }

    public void setAgentDigestSendHour(Integer agentDigestSendHour) {
        this.agentDigestSendHour = agentDigestSendHour;
    }

    public Integer getAgentDigestSendMinute() {
        return agentDigestSendMinute;
    }

    public void setAgentDigestSendMinute(Integer agentDigestSendMinute) {
        this.agentDigestSendMinute = agentDigestSendMinute;
    }

    public Boolean getSlaUseBusinessHours() {
        return slaUseBusinessHours;
    }

    public void setSlaUseBusinessHours(Boolean slaUseBusinessHours) {
        this.slaUseBusinessHours = slaUseBusinessHours;
    }

    public String getSlaTimezone() {
        return slaTimezone;
    }

    public void setSlaTimezone(String slaTimezone) {
        this.slaTimezone = slaTimezone;
    }

    public BusinessHoursWeekDto getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(BusinessHoursWeekDto businessHours) {
        this.businessHours = businessHours;
    }

    public List<BusinessHolidayDto> getBusinessHolidays() {
        return businessHolidays;
    }

    public void setBusinessHolidays(List<BusinessHolidayDto> businessHolidays) {
        this.businessHolidays = businessHolidays != null ? businessHolidays : new ArrayList<>();
    }

    public Boolean getSlaWarnEnabled() {
        return slaWarnEnabled;
    }

    public void setSlaWarnEnabled(Boolean slaWarnEnabled) {
        this.slaWarnEnabled = slaWarnEnabled;
    }

    public Integer getSlaWarnHoursBefore() {
        return slaWarnHoursBefore;
    }

    public void setSlaWarnHoursBefore(Integer slaWarnHoursBefore) {
        this.slaWarnHoursBefore = slaWarnHoursBefore;
    }

    public Boolean getSlaBreachEscalationEnabled() {
        return slaBreachEscalationEnabled;
    }

    public void setSlaBreachEscalationEnabled(Boolean slaBreachEscalationEnabled) {
        this.slaBreachEscalationEnabled = slaBreachEscalationEnabled;
    }

    public Boolean getSlaBreachBumpPriority() {
        return slaBreachBumpPriority;
    }

    public void setSlaBreachBumpPriority(Boolean slaBreachBumpPriority) {
        this.slaBreachBumpPriority = slaBreachBumpPriority;
    }

    public Long getSlaBreachAssigneeId() {
        return slaBreachAssigneeId;
    }

    public void setSlaBreachAssigneeId(Long slaBreachAssigneeId) {
        this.slaBreachAssigneeId = slaBreachAssigneeId;
    }

    public Long getSlaBreachQueueId() {
        return slaBreachQueueId;
    }

    public void setSlaBreachQueueId(Long slaBreachQueueId) {
        this.slaBreachQueueId = slaBreachQueueId;
    }

    public TicketPriority getAutomationDefaultPriority() {
        return automationDefaultPriority;
    }

    public void setAutomationDefaultPriority(TicketPriority automationDefaultPriority) {
        this.automationDefaultPriority = automationDefaultPriority;
    }

    public Boolean getAutomationCustomerAckEnabled() {
        return automationCustomerAckEnabled;
    }

    public void setAutomationCustomerAckEnabled(Boolean automationCustomerAckEnabled) {
        this.automationCustomerAckEnabled = automationCustomerAckEnabled;
    }

    public Boolean getAutomationNoReplyEnabled() {
        return automationNoReplyEnabled;
    }

    public void setAutomationNoReplyEnabled(Boolean automationNoReplyEnabled) {
        this.automationNoReplyEnabled = automationNoReplyEnabled;
    }

    public Integer getAutomationNoReplyHours() {
        return automationNoReplyHours;
    }

    public void setAutomationNoReplyHours(Integer automationNoReplyHours) {
        this.automationNoReplyHours = automationNoReplyHours;
    }

    public TicketNoReplyAction getAutomationNoReplyAction() {
        return automationNoReplyAction;
    }

    public void setAutomationNoReplyAction(TicketNoReplyAction automationNoReplyAction) {
        this.automationNoReplyAction = automationNoReplyAction;
    }

    public Boolean getAutomationCsatInviteEnabled() {
        return automationCsatInviteEnabled;
    }

    public void setAutomationCsatInviteEnabled(Boolean automationCsatInviteEnabled) {
        this.automationCsatInviteEnabled = automationCsatInviteEnabled;
    }

    public Boolean getAutomationAutoCloseEnabled() {
        return automationAutoCloseEnabled;
    }

    public void setAutomationAutoCloseEnabled(Boolean automationAutoCloseEnabled) {
        this.automationAutoCloseEnabled = automationAutoCloseEnabled;
    }

    public Integer getAutomationAutoCloseDays() {
        return automationAutoCloseDays;
    }

    public void setAutomationAutoCloseDays(Integer automationAutoCloseDays) {
        this.automationAutoCloseDays = automationAutoCloseDays;
    }
}
