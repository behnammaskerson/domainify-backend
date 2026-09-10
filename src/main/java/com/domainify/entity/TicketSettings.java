package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "ticket_settings")
public class TicketSettings {

    public static final long SINGLETON_ID = 1L;
    public static final int DEFAULT_REOPEN_WINDOW_DAYS = 14;
    public static final int DEFAULT_MAX_ATTACHMENTS = 5;
    public static final int DEFAULT_MAX_ATTACHMENT_SIZE_MB = 5;
    public static final int DEFAULT_AUTO_ARCHIVE_CLOSED_AFTER_DAYS = 90;
    public static final int DEFAULT_SLA_URGENT_HOURS = 4;
    public static final int DEFAULT_SLA_HIGH_HOURS = 24;
    public static final int DEFAULT_SLA_MEDIUM_HOURS = 72;
    public static final int DEFAULT_SLA_LOW_HOURS = 168;

    public static final int DEFAULT_FIRST_RESPONSE_SLA_URGENT_HOURS = 1;
    public static final int DEFAULT_FIRST_RESPONSE_SLA_HIGH_HOURS = 4;
    public static final int DEFAULT_FIRST_RESPONSE_SLA_MEDIUM_HOURS = 8;
    public static final int DEFAULT_FIRST_RESPONSE_SLA_LOW_HOURS = 24;
    public static final String DEFAULT_ALLOWED_ATTACHMENT_KINDS = "IMAGE,PDF,LOG,DOCUMENT";
    public static final String DEFAULT_EMAIL_NOTIFICATION_PRIORITIES = "LOW,MEDIUM,HIGH,URGENT";
    public static final String DEFAULT_SMS_NOTIFICATION_PRIORITIES = "URGENT";
    public static final String DEFAULT_SLA_TIMEZONE = "UTC";
    public static final String DEFAULT_BUSINESS_HOURS_JSON =
            "{\"monday\":{\"start\":\"09:00\",\"end\":\"17:00\"},\"tuesday\":{\"start\":\"09:00\",\"end\":\"17:00\"},"
                    + "\"wednesday\":{\"start\":\"09:00\",\"end\":\"17:00\"},\"thursday\":{\"start\":\"09:00\",\"end\":\"17:00\"},"
                    + "\"friday\":{\"start\":\"09:00\",\"end\":\"17:00\"},\"saturday\":null,\"sunday\":null}";
    public static final String DEFAULT_BUSINESS_HOLIDAYS_JSON = "[]";
    public static final int DEFAULT_SLA_WARN_HOURS_BEFORE = 2;
    public static final int DEFAULT_AUTOMATION_NO_REPLY_HOURS = 48;
    public static final int DEFAULT_AUTOMATION_AUTO_CLOSE_DAYS = 7;

    @Id
    private Long id = SINGLETON_ID;

    /** Days after close during which a ticket may be reopened. */
    @Column(name = "reopen_window_days", nullable = false)
    private int reopenWindowDays = DEFAULT_REOPEN_WINDOW_DAYS;

    /** Max files allowed in a single create/reply upload. */
    @Column(name = "max_attachments", nullable = false, columnDefinition = "integer not null default 5")
    private int maxAttachments = DEFAULT_MAX_ATTACHMENTS;

    /** Max size per attachment file, in megabytes. */
    @Column(name = "max_attachment_size_mb", nullable = false, columnDefinition = "integer not null default 5")
    private int maxAttachmentSizeMb = DEFAULT_MAX_ATTACHMENT_SIZE_MB;

    /** Comma-separated {@link TicketAttachmentKind} names. */
    @Column(
            name = "allowed_attachment_kinds",
            nullable = false,
            length = 120,
            columnDefinition = "varchar(120) not null default 'IMAGE,PDF,LOG,DOCUMENT'"
    )
    private String allowedAttachmentKinds = DEFAULT_ALLOWED_ATTACHMENT_KINDS;

    /**
     * Days after close when a ticket is auto-archived. {@code 0} disables auto-archive.
     */
    @Column(
            name = "auto_archive_closed_after_days",
            nullable = false,
            columnDefinition = "integer not null default 90"
    )
    private int autoArchiveClosedAfterDays = DEFAULT_AUTO_ARCHIVE_CLOSED_AFTER_DAYS;

    @Column(name = "sla_urgent_hours", nullable = false, columnDefinition = "integer not null default 4")
    private int slaUrgentHours = DEFAULT_SLA_URGENT_HOURS;

    @Column(name = "sla_high_hours", nullable = false, columnDefinition = "integer not null default 24")
    private int slaHighHours = DEFAULT_SLA_HIGH_HOURS;

    @Column(name = "sla_medium_hours", nullable = false, columnDefinition = "integer not null default 72")
    private int slaMediumHours = DEFAULT_SLA_MEDIUM_HOURS;

    @Column(name = "sla_low_hours", nullable = false, columnDefinition = "integer not null default 168")
    private int slaLowHours = DEFAULT_SLA_LOW_HOURS;

    /** Org default first-response SLA hours by priority. */
    @Column(name = "first_response_sla_urgent_hours", nullable = true)
    private Integer firstResponseSlaUrgentHours = DEFAULT_FIRST_RESPONSE_SLA_URGENT_HOURS;

    @Column(name = "first_response_sla_high_hours", nullable = true)
    private Integer firstResponseSlaHighHours = DEFAULT_FIRST_RESPONSE_SLA_HIGH_HOURS;

    @Column(name = "first_response_sla_medium_hours", nullable = true)
    private Integer firstResponseSlaMediumHours = DEFAULT_FIRST_RESPONSE_SLA_MEDIUM_HOURS;

    @Column(name = "first_response_sla_low_hours", nullable = true)
    private Integer firstResponseSlaLowHours = DEFAULT_FIRST_RESPONSE_SLA_LOW_HOURS;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_assign_mode", nullable = false, length = 32)
    private TicketAutoAssignMode autoAssignMode = TicketAutoAssignMode.OFF;

    /**
     * When mode is CATEGORY_SKILL and no skilled agents exist, fall back to global round-robin.
     */
    @Column(name = "auto_assign_fallback_round_robin", nullable = false)
    private boolean autoAssignFallbackRoundRobin = true;

    /** Last agent chosen by round-robin (global cursor). */
    @Column(name = "round_robin_last_user_id")
    private Long roundRobinLastUserId;

    /** Optional default queue applied to newly created tickets. */
    @Column(name = "default_queue_id")
    private Long defaultQueueId;

    /** Master switch for ticket event emails (reply / assign / status). Default on. */
    @ColumnDefault("true")
    @Column(name = "ticket_email_notifications_enabled", nullable = true)
    private Boolean ticketEmailNotificationsEnabled = true;

    /** Master switch for URGENT ticket SMS alerts. Default on (users still must opt in). */
    @ColumnDefault("true")
    @Column(name = "ticket_sms_notifications_enabled", nullable = true)
    private Boolean ticketSmsNotificationsEnabled = true;

    /** Comma-separated {@link TicketPriority} names that receive email alerts. */
    @Column(name = "email_notification_priorities", length = 64)
    private String emailNotificationPriorities = DEFAULT_EMAIL_NOTIFICATION_PRIORITIES;

    /** Comma-separated {@link TicketPriority} names that receive SMS alerts. */
    @Column(name = "sms_notification_priorities", length = 64)
    private String smsNotificationPriorities = DEFAULT_SMS_NOTIFICATION_PRIORITIES;

    /** Master switch for daily agent digest emails. Default off. */
    @ColumnDefault("false")
    @Column(name = "agent_digest_enabled", nullable = true)
    private Boolean agentDigestEnabled = false;

    @ColumnDefault("8")
    @Column(name = "agent_digest_send_hour", nullable = true)
    private Integer agentDigestSendHour = 8;

    @ColumnDefault("0")
    @Column(name = "agent_digest_send_minute", nullable = true)
    private Integer agentDigestSendMinute = 0;

    /** Calendar date (server local) of last successful digest run. */
    @Column(name = "agent_digest_last_run_date")
    private LocalDate agentDigestLastRunDate;

    /** When true, SLA due dates advance by business hours in {@link #slaTimezone}. */
    @ColumnDefault("false")
    @Column(name = "sla_use_business_hours", nullable = true)
    private Boolean slaUseBusinessHours = false;

    /** IANA timezone for business-hours SLA calculation. */
    @Column(name = "sla_timezone", length = 64)
    private String slaTimezone = DEFAULT_SLA_TIMEZONE;

    /** Weekly business hours JSON ({@code BusinessHoursWeekDto}). */
    @Column(name = "business_hours_json", columnDefinition = "text")
    private String businessHoursJson = DEFAULT_BUSINESS_HOURS_JSON;

    /** Holiday list JSON ({@code List<BusinessHolidayDto>}). */
    @Column(name = "business_holidays_json", columnDefinition = "text")
    private String businessHolidaysJson = DEFAULT_BUSINESS_HOLIDAYS_JSON;

    /** When true, notify before resolve SLA due date within {@link #slaWarnHoursBefore}. */
    @ColumnDefault("false")
    @Column(name = "sla_warn_enabled", nullable = true)
    private Boolean slaWarnEnabled = false;

    /** Wall-clock hours before resolve due to send approaching-SLA warning. */
    @ColumnDefault("2")
    @Column(name = "sla_warn_hours_before", nullable = true)
    private Integer slaWarnHoursBefore = DEFAULT_SLA_WARN_HOURS_BEFORE;

    /** When false, overdue tickets are not auto-escalated (overdue UI still works). */
    @ColumnDefault("true")
    @Column(name = "sla_breach_escalation_enabled", nullable = true)
    private Boolean slaBreachEscalationEnabled = true;

    /** Bump priority on SLA breach auto-escalation. */
    @ColumnDefault("true")
    @Column(name = "sla_breach_bump_priority", nullable = true)
    private Boolean slaBreachBumpPriority = true;

    /** Optional assignee applied on SLA breach auto-escalation. */
    @Column(name = "sla_breach_assignee_id")
    private Long slaBreachAssigneeId;

    /** Optional queue applied on SLA breach auto-escalation. */
    @Column(name = "sla_breach_queue_id")
    private Long slaBreachQueueId;

    /** When set, force this priority on customer-created tickets (null = keep customer choice). */
    @Enumerated(EnumType.STRING)
    @Column(name = "automation_default_priority", length = 16)
    private TicketPriority automationDefaultPriority;

    /** Send customer acknowledgement notification on ticket create. */
    @ColumnDefault("true")
    @Column(name = "automation_customer_ack_enabled", nullable = true)
    private Boolean automationCustomerAckEnabled = true;

    /** Enable no-reply automations while status is PENDING (waiting on customer). */
    @ColumnDefault("false")
    @Column(name = "automation_no_reply_enabled", nullable = true)
    private Boolean automationNoReplyEnabled = false;

    /** Hours after last staff public reply before no-reply action runs. */
    @ColumnDefault("48")
    @Column(name = "automation_no_reply_hours", nullable = true)
    private Integer automationNoReplyHours = DEFAULT_AUTOMATION_NO_REPLY_HOURS;

    @Enumerated(EnumType.STRING)
    @Column(name = "automation_no_reply_action", length = 32)
    private TicketNoReplyAction automationNoReplyAction = TicketNoReplyAction.REMIND;

    /** Include CSAT invite in resolve emails and allow rating in portal. */
    @ColumnDefault("true")
    @Column(name = "automation_csat_invite_enabled", nullable = true)
    private Boolean automationCsatInviteEnabled = true;

    /** Auto-close RESOLVED tickets when the customer stays silent for N days. */
    @ColumnDefault("false")
    @Column(name = "automation_auto_close_enabled", nullable = true)
    private Boolean automationAutoCloseEnabled = false;

    /** Days after resolve with no customer reply before auto-close. */
    @ColumnDefault("7")
    @Column(name = "automation_auto_close_days", nullable = true)
    private Integer automationAutoCloseDays = DEFAULT_AUTOMATION_AUTO_CLOSE_DAYS;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
        normalize();
    }

    public void normalize() {
        if (reopenWindowDays < 1) {
            reopenWindowDays = DEFAULT_REOPEN_WINDOW_DAYS;
        }
        if (maxAttachments < 1) {
            maxAttachments = DEFAULT_MAX_ATTACHMENTS;
        }
        if (maxAttachmentSizeMb < 1) {
            maxAttachmentSizeMb = DEFAULT_MAX_ATTACHMENT_SIZE_MB;
        }
        if (autoArchiveClosedAfterDays < 0) {
            autoArchiveClosedAfterDays = DEFAULT_AUTO_ARCHIVE_CLOSED_AFTER_DAYS;
        }
        if (slaUrgentHours < 1) {
            slaUrgentHours = DEFAULT_SLA_URGENT_HOURS;
        }
        if (slaHighHours < 1) {
            slaHighHours = DEFAULT_SLA_HIGH_HOURS;
        }
        if (slaMediumHours < 1) {
            slaMediumHours = DEFAULT_SLA_MEDIUM_HOURS;
        }
        if (slaLowHours < 1) {
            slaLowHours = DEFAULT_SLA_LOW_HOURS;
        }
        if (firstResponseSlaUrgentHours == null || firstResponseSlaUrgentHours < 1) {
            firstResponseSlaUrgentHours = DEFAULT_FIRST_RESPONSE_SLA_URGENT_HOURS;
        }
        if (firstResponseSlaHighHours == null || firstResponseSlaHighHours < 1) {
            firstResponseSlaHighHours = DEFAULT_FIRST_RESPONSE_SLA_HIGH_HOURS;
        }
        if (firstResponseSlaMediumHours == null || firstResponseSlaMediumHours < 1) {
            firstResponseSlaMediumHours = DEFAULT_FIRST_RESPONSE_SLA_MEDIUM_HOURS;
        }
        if (firstResponseSlaLowHours == null || firstResponseSlaLowHours < 1) {
            firstResponseSlaLowHours = DEFAULT_FIRST_RESPONSE_SLA_LOW_HOURS;
        }
        if (autoAssignMode == null) {
            autoAssignMode = TicketAutoAssignMode.OFF;
        }
        Set<TicketAttachmentKind> kinds = TicketAttachmentKind.parseCsv(allowedAttachmentKinds);
        if (kinds.isEmpty()) {
            kinds = EnumSet.allOf(TicketAttachmentKind.class);
        }
        allowedAttachmentKinds = TicketAttachmentKind.toCsv(kinds);
        emailNotificationPriorities = toPriorityCsv(resolvedEmailNotificationPriorities());
        smsNotificationPriorities = toPriorityCsv(resolvedSmsNotificationPriorities());
        if (agentDigestEnabled == null) {
            agentDigestEnabled = false;
        }
        if (agentDigestSendHour == null || agentDigestSendHour < 0 || agentDigestSendHour > 23) {
            agentDigestSendHour = 8;
        }
        if (agentDigestSendMinute == null || agentDigestSendMinute < 0 || agentDigestSendMinute > 59) {
            agentDigestSendMinute = 0;
        }
        if (slaUseBusinessHours == null) {
            slaUseBusinessHours = false;
        }
        if (slaTimezone == null || slaTimezone.isBlank()) {
            slaTimezone = DEFAULT_SLA_TIMEZONE;
        }
        if (businessHoursJson == null || businessHoursJson.isBlank()) {
            businessHoursJson = DEFAULT_BUSINESS_HOURS_JSON;
        }
        if (businessHolidaysJson == null || businessHolidaysJson.isBlank()) {
            businessHolidaysJson = DEFAULT_BUSINESS_HOLIDAYS_JSON;
        }
        if (slaWarnEnabled == null) {
            slaWarnEnabled = false;
        }
        if (slaWarnHoursBefore == null || slaWarnHoursBefore < 1 || slaWarnHoursBefore > 8760) {
            slaWarnHoursBefore = DEFAULT_SLA_WARN_HOURS_BEFORE;
        }
        if (slaBreachEscalationEnabled == null) {
            slaBreachEscalationEnabled = true;
        }
        if (slaBreachBumpPriority == null) {
            slaBreachBumpPriority = true;
        }
        if (automationCustomerAckEnabled == null) {
            automationCustomerAckEnabled = true;
        }
        if (automationNoReplyEnabled == null) {
            automationNoReplyEnabled = false;
        }
        if (automationNoReplyHours == null || automationNoReplyHours < 1 || automationNoReplyHours > 8760) {
            automationNoReplyHours = DEFAULT_AUTOMATION_NO_REPLY_HOURS;
        }
        if (automationNoReplyAction == null) {
            automationNoReplyAction = TicketNoReplyAction.REMIND;
        }
        if (automationCsatInviteEnabled == null) {
            automationCsatInviteEnabled = true;
        }
        if (automationAutoCloseEnabled == null) {
            automationAutoCloseEnabled = false;
        }
        if (automationAutoCloseDays == null || automationAutoCloseDays < 1 || automationAutoCloseDays > 3650) {
            automationAutoCloseDays = DEFAULT_AUTOMATION_AUTO_CLOSE_DAYS;
        }
    }

    public static TicketSettings defaults() {
        TicketSettings settings = new TicketSettings();
        settings.setId(SINGLETON_ID);
        settings.setReopenWindowDays(DEFAULT_REOPEN_WINDOW_DAYS);
        settings.setMaxAttachments(DEFAULT_MAX_ATTACHMENTS);
        settings.setMaxAttachmentSizeMb(DEFAULT_MAX_ATTACHMENT_SIZE_MB);
        settings.setAutoArchiveClosedAfterDays(DEFAULT_AUTO_ARCHIVE_CLOSED_AFTER_DAYS);
        settings.setSlaUrgentHours(DEFAULT_SLA_URGENT_HOURS);
        settings.setSlaHighHours(DEFAULT_SLA_HIGH_HOURS);
        settings.setSlaMediumHours(DEFAULT_SLA_MEDIUM_HOURS);
        settings.setSlaLowHours(DEFAULT_SLA_LOW_HOURS);
        settings.setFirstResponseSlaUrgentHours(DEFAULT_FIRST_RESPONSE_SLA_URGENT_HOURS);
        settings.setFirstResponseSlaHighHours(DEFAULT_FIRST_RESPONSE_SLA_HIGH_HOURS);
        settings.setFirstResponseSlaMediumHours(DEFAULT_FIRST_RESPONSE_SLA_MEDIUM_HOURS);
        settings.setFirstResponseSlaLowHours(DEFAULT_FIRST_RESPONSE_SLA_LOW_HOURS);
        settings.setAutoAssignMode(TicketAutoAssignMode.OFF);
        settings.setAutoAssignFallbackRoundRobin(true);
        settings.setTicketEmailNotificationsEnabled(true);
        settings.setTicketSmsNotificationsEnabled(true);
        settings.setEmailNotificationPriorities(DEFAULT_EMAIL_NOTIFICATION_PRIORITIES);
        settings.setSmsNotificationPriorities(DEFAULT_SMS_NOTIFICATION_PRIORITIES);
        settings.setAgentDigestEnabled(false);
        settings.setAgentDigestSendHour(8);
        settings.setAgentDigestSendMinute(0);
        settings.setAllowedAttachmentKinds(DEFAULT_ALLOWED_ATTACHMENT_KINDS);
        settings.setSlaUseBusinessHours(false);
        settings.setSlaTimezone(DEFAULT_SLA_TIMEZONE);
        settings.setBusinessHoursJson(DEFAULT_BUSINESS_HOURS_JSON);
        settings.setBusinessHolidaysJson(DEFAULT_BUSINESS_HOLIDAYS_JSON);
        settings.setSlaWarnEnabled(false);
        settings.setSlaWarnHoursBefore(DEFAULT_SLA_WARN_HOURS_BEFORE);
        settings.setSlaBreachEscalationEnabled(true);
        settings.setSlaBreachBumpPriority(true);
        settings.setAutomationCustomerAckEnabled(true);
        settings.setAutomationNoReplyEnabled(false);
        settings.setAutomationNoReplyHours(DEFAULT_AUTOMATION_NO_REPLY_HOURS);
        settings.setAutomationNoReplyAction(TicketNoReplyAction.REMIND);
        settings.setAutomationCsatInviteEnabled(true);
        settings.setAutomationAutoCloseEnabled(false);
        settings.setAutomationAutoCloseDays(DEFAULT_AUTOMATION_AUTO_CLOSE_DAYS);
        settings.normalize();
        return settings;
    }

    public long maxAttachmentBytes() {
        return Math.max(1, maxAttachmentSizeMb) * 1024L * 1024L;
    }

    public Set<TicketAttachmentKind> resolvedAttachmentKinds() {
        Set<TicketAttachmentKind> kinds = TicketAttachmentKind.parseCsv(allowedAttachmentKinds);
        if (kinds.isEmpty()) {
            return EnumSet.allOf(TicketAttachmentKind.class);
        }
        return kinds;
    }

    public Set<TicketPriority> resolvedEmailNotificationPriorities() {
        Set<TicketPriority> priorities = parsePriorities(emailNotificationPriorities);
        if (priorities.isEmpty()) {
            return EnumSet.allOf(TicketPriority.class);
        }
        return priorities;
    }

    public Set<TicketPriority> resolvedSmsNotificationPriorities() {
        Set<TicketPriority> priorities = parsePriorities(smsNotificationPriorities);
        if (priorities.isEmpty()) {
            return EnumSet.of(TicketPriority.URGENT);
        }
        return priorities;
    }

    public boolean allowsEmailForPriority(TicketPriority priority) {
        if (priority == null) {
            return false;
        }
        return resolvedEmailNotificationPriorities().contains(priority);
    }

    public boolean allowsSmsForPriority(TicketPriority priority) {
        if (priority == null) {
            return false;
        }
        return resolvedSmsNotificationPriorities().contains(priority);
    }

    public static Set<TicketPriority> parsePriorities(String csv) {
        Set<TicketPriority> priorities = EnumSet.noneOf(TicketPriority.class);
        if (csv == null || csv.isBlank()) {
            return priorities;
        }
        for (String token : csv.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                priorities.add(TicketPriority.valueOf(trimmed.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // skip unknown tokens
            }
        }
        return priorities;
    }

    public static String toPriorityCsv(Set<TicketPriority> priorities) {
        if (priorities == null || priorities.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (TicketPriority priority : TicketPriority.values()) {
            if (priorities.contains(priority)) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(priority.name());
            }
        }
        return sb.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getReopenWindowDays() {
        return reopenWindowDays;
    }

    public void setReopenWindowDays(int reopenWindowDays) {
        this.reopenWindowDays = reopenWindowDays;
    }

    public int getMaxAttachments() {
        return maxAttachments;
    }

    public void setMaxAttachments(int maxAttachments) {
        this.maxAttachments = maxAttachments;
    }

    public int getMaxAttachmentSizeMb() {
        return maxAttachmentSizeMb;
    }

    public void setMaxAttachmentSizeMb(int maxAttachmentSizeMb) {
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
    }

    public String getAllowedAttachmentKinds() {
        return allowedAttachmentKinds;
    }

    public void setAllowedAttachmentKinds(String allowedAttachmentKinds) {
        this.allowedAttachmentKinds = allowedAttachmentKinds;
    }

    public int getAutoArchiveClosedAfterDays() {
        return autoArchiveClosedAfterDays;
    }

    public void setAutoArchiveClosedAfterDays(int autoArchiveClosedAfterDays) {
        this.autoArchiveClosedAfterDays = autoArchiveClosedAfterDays;
    }

    public int getSlaUrgentHours() {
        return slaUrgentHours;
    }

    public void setSlaUrgentHours(int slaUrgentHours) {
        this.slaUrgentHours = slaUrgentHours;
    }

    public int getSlaHighHours() {
        return slaHighHours;
    }

    public void setSlaHighHours(int slaHighHours) {
        this.slaHighHours = slaHighHours;
    }

    public int getSlaMediumHours() {
        return slaMediumHours;
    }

    public void setSlaMediumHours(int slaMediumHours) {
        this.slaMediumHours = slaMediumHours;
    }

    public int getSlaLowHours() {
        return slaLowHours;
    }

    public void setSlaLowHours(int slaLowHours) {
        this.slaLowHours = slaLowHours;
    }

    public int getFirstResponseSlaUrgentHours() {
        return firstResponseSlaUrgentHours != null
                ? firstResponseSlaUrgentHours
                : DEFAULT_FIRST_RESPONSE_SLA_URGENT_HOURS;
    }

    public void setFirstResponseSlaUrgentHours(Integer firstResponseSlaUrgentHours) {
        this.firstResponseSlaUrgentHours = firstResponseSlaUrgentHours;
    }

    public int getFirstResponseSlaHighHours() {
        return firstResponseSlaHighHours != null
                ? firstResponseSlaHighHours
                : DEFAULT_FIRST_RESPONSE_SLA_HIGH_HOURS;
    }

    public void setFirstResponseSlaHighHours(Integer firstResponseSlaHighHours) {
        this.firstResponseSlaHighHours = firstResponseSlaHighHours;
    }

    public int getFirstResponseSlaMediumHours() {
        return firstResponseSlaMediumHours != null
                ? firstResponseSlaMediumHours
                : DEFAULT_FIRST_RESPONSE_SLA_MEDIUM_HOURS;
    }

    public void setFirstResponseSlaMediumHours(Integer firstResponseSlaMediumHours) {
        this.firstResponseSlaMediumHours = firstResponseSlaMediumHours;
    }

    public int getFirstResponseSlaLowHours() {
        return firstResponseSlaLowHours != null
                ? firstResponseSlaLowHours
                : DEFAULT_FIRST_RESPONSE_SLA_LOW_HOURS;
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

    public boolean isAutoAssignFallbackRoundRobin() {
        return autoAssignFallbackRoundRobin;
    }

    public void setAutoAssignFallbackRoundRobin(boolean autoAssignFallbackRoundRobin) {
        this.autoAssignFallbackRoundRobin = autoAssignFallbackRoundRobin;
    }

    public Long getRoundRobinLastUserId() {
        return roundRobinLastUserId;
    }

    public void setRoundRobinLastUserId(Long roundRobinLastUserId) {
        this.roundRobinLastUserId = roundRobinLastUserId;
    }

    public Long getDefaultQueueId() {
        return defaultQueueId;
    }

    public void setDefaultQueueId(Long defaultQueueId) {
        this.defaultQueueId = defaultQueueId;
    }

    public boolean isTicketEmailNotificationsEnabled() {
        return ticketEmailNotificationsEnabled == null || Boolean.TRUE.equals(ticketEmailNotificationsEnabled);
    }

    public void setTicketEmailNotificationsEnabled(boolean ticketEmailNotificationsEnabled) {
        this.ticketEmailNotificationsEnabled = ticketEmailNotificationsEnabled;
    }

    public boolean isTicketSmsNotificationsEnabled() {
        return ticketSmsNotificationsEnabled == null || Boolean.TRUE.equals(ticketSmsNotificationsEnabled);
    }

    public void setTicketSmsNotificationsEnabled(boolean ticketSmsNotificationsEnabled) {
        this.ticketSmsNotificationsEnabled = ticketSmsNotificationsEnabled;
    }

    public String getEmailNotificationPriorities() {
        return emailNotificationPriorities;
    }

    public void setEmailNotificationPriorities(String emailNotificationPriorities) {
        this.emailNotificationPriorities = emailNotificationPriorities;
    }

    public String getSmsNotificationPriorities() {
        return smsNotificationPriorities;
    }

    public void setSmsNotificationPriorities(String smsNotificationPriorities) {
        this.smsNotificationPriorities = smsNotificationPriorities;
    }

    public boolean isAgentDigestEnabled() {
        return Boolean.TRUE.equals(agentDigestEnabled);
    }

    public void setAgentDigestEnabled(boolean agentDigestEnabled) {
        this.agentDigestEnabled = agentDigestEnabled;
    }

    public int getAgentDigestSendHour() {
        return agentDigestSendHour != null ? agentDigestSendHour : 8;
    }

    public void setAgentDigestSendHour(Integer agentDigestSendHour) {
        this.agentDigestSendHour = agentDigestSendHour;
    }

    public int getAgentDigestSendMinute() {
        return agentDigestSendMinute != null ? agentDigestSendMinute : 0;
    }

    public void setAgentDigestSendMinute(Integer agentDigestSendMinute) {
        this.agentDigestSendMinute = agentDigestSendMinute;
    }

    public LocalDate getAgentDigestLastRunDate() {
        return agentDigestLastRunDate;
    }

    public void setAgentDigestLastRunDate(LocalDate agentDigestLastRunDate) {
        this.agentDigestLastRunDate = agentDigestLastRunDate;
    }

    public boolean isSlaUseBusinessHours() {
        return Boolean.TRUE.equals(slaUseBusinessHours);
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

    public String getBusinessHoursJson() {
        return businessHoursJson;
    }

    public void setBusinessHoursJson(String businessHoursJson) {
        this.businessHoursJson = businessHoursJson;
    }

    public String getBusinessHolidaysJson() {
        return businessHolidaysJson;
    }

    public void setBusinessHolidaysJson(String businessHolidaysJson) {
        this.businessHolidaysJson = businessHolidaysJson;
    }

    public boolean isSlaWarnEnabled() {
        return Boolean.TRUE.equals(slaWarnEnabled);
    }

    public void setSlaWarnEnabled(Boolean slaWarnEnabled) {
        this.slaWarnEnabled = slaWarnEnabled;
    }

    public int getSlaWarnHoursBefore() {
        return slaWarnHoursBefore != null ? slaWarnHoursBefore : DEFAULT_SLA_WARN_HOURS_BEFORE;
    }

    public void setSlaWarnHoursBefore(Integer slaWarnHoursBefore) {
        this.slaWarnHoursBefore = slaWarnHoursBefore;
    }

    public boolean isSlaBreachEscalationEnabled() {
        return slaBreachEscalationEnabled == null || Boolean.TRUE.equals(slaBreachEscalationEnabled);
    }

    public void setSlaBreachEscalationEnabled(Boolean slaBreachEscalationEnabled) {
        this.slaBreachEscalationEnabled = slaBreachEscalationEnabled;
    }

    public boolean isSlaBreachBumpPriority() {
        return slaBreachBumpPriority == null || Boolean.TRUE.equals(slaBreachBumpPriority);
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

    public boolean isAutomationCustomerAckEnabled() {
        return automationCustomerAckEnabled == null || Boolean.TRUE.equals(automationCustomerAckEnabled);
    }

    public void setAutomationCustomerAckEnabled(Boolean automationCustomerAckEnabled) {
        this.automationCustomerAckEnabled = automationCustomerAckEnabled;
    }

    public boolean isAutomationNoReplyEnabled() {
        return Boolean.TRUE.equals(automationNoReplyEnabled);
    }

    public void setAutomationNoReplyEnabled(Boolean automationNoReplyEnabled) {
        this.automationNoReplyEnabled = automationNoReplyEnabled;
    }

    public int getAutomationNoReplyHours() {
        return automationNoReplyHours != null ? automationNoReplyHours : DEFAULT_AUTOMATION_NO_REPLY_HOURS;
    }

    public void setAutomationNoReplyHours(Integer automationNoReplyHours) {
        this.automationNoReplyHours = automationNoReplyHours;
    }

    public TicketNoReplyAction getAutomationNoReplyAction() {
        return automationNoReplyAction != null ? automationNoReplyAction : TicketNoReplyAction.REMIND;
    }

    public void setAutomationNoReplyAction(TicketNoReplyAction automationNoReplyAction) {
        this.automationNoReplyAction = automationNoReplyAction;
    }

    public boolean isAutomationCsatInviteEnabled() {
        return automationCsatInviteEnabled == null || Boolean.TRUE.equals(automationCsatInviteEnabled);
    }

    public void setAutomationCsatInviteEnabled(Boolean automationCsatInviteEnabled) {
        this.automationCsatInviteEnabled = automationCsatInviteEnabled;
    }

    public boolean isAutomationAutoCloseEnabled() {
        return Boolean.TRUE.equals(automationAutoCloseEnabled);
    }

    public void setAutomationAutoCloseEnabled(Boolean automationAutoCloseEnabled) {
        this.automationAutoCloseEnabled = automationAutoCloseEnabled;
    }

    public int getAutomationAutoCloseDays() {
        return automationAutoCloseDays != null ? automationAutoCloseDays : DEFAULT_AUTOMATION_AUTO_CLOSE_DAYS;
    }

    public void setAutomationAutoCloseDays(Integer automationAutoCloseDays) {
        this.automationAutoCloseDays = automationAutoCloseDays;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
