package com.domainify.service;

import com.domainify.dto.BusinessHolidayDto;
import com.domainify.dto.BusinessHoursWeekDto;
import com.domainify.dto.TicketAttachmentPolicyDto;
import com.domainify.dto.TicketSettingsDto;
import com.domainify.entity.TicketAttachmentKind;
import com.domainify.entity.TicketAutoAssignMode;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketSettings;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketSettingsRepository;
import com.domainify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Service
public class TicketSettingsService {

    private static final int MAX_BUSINESS_HOLIDAYS = 366;

    private final TicketSettingsRepository ticketSettingsRepository;
    private final TicketQueueService ticketQueueService;
    private final TicketBusinessHoursCalculator businessHoursCalculator;
    private final UserRepository userRepository;

    public TicketSettingsService(
            TicketSettingsRepository ticketSettingsRepository,
            TicketQueueService ticketQueueService,
            TicketBusinessHoursCalculator businessHoursCalculator,
            UserRepository userRepository) {
        this.ticketSettingsRepository = ticketSettingsRepository;
        this.ticketQueueService = ticketQueueService;
        this.businessHoursCalculator = businessHoursCalculator;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketSettings getOrCreate() {
        TicketSettings settings = ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .orElseGet(() -> ticketSettingsRepository.save(TicketSettings.defaults()));
        settings.normalize();
        return settings;
    }

    @Transactional
    public TicketSettingsDto getDto() {
        return toDto(getOrCreate());
    }

    @Transactional
    public TicketAttachmentPolicyDto getAttachmentPolicy() {
        return toPolicy(getOrCreate());
    }

    @Transactional(readOnly = true)
    public int getReopenWindowDays() {
        return Math.max(1, ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .map(TicketSettings::getReopenWindowDays)
                .orElse(TicketSettings.DEFAULT_REOPEN_WINDOW_DAYS));
    }

    @Transactional(readOnly = true)
    public int getAutoArchiveClosedAfterDays() {
        return Math.max(0, ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .map(TicketSettings::getAutoArchiveClosedAfterDays)
                .orElse(TicketSettings.DEFAULT_AUTO_ARCHIVE_CLOSED_AFTER_DAYS));
    }

    @Transactional
    public TicketSettingsDto update(TicketSettingsDto request) {
        if (request == null
                || request.getReopenWindowDays() == null
                || request.getMaxAttachments() == null
                || request.getMaxAttachmentSizeMb() == null
                || request.getAutoArchiveClosedAfterDays() == null
                || request.getSlaUrgentHours() == null
                || request.getSlaHighHours() == null
                || request.getSlaMediumHours() == null
                || request.getSlaLowHours() == null
                || request.getFirstResponseSlaUrgentHours() == null
                || request.getFirstResponseSlaHighHours() == null
                || request.getFirstResponseSlaMediumHours() == null
                || request.getFirstResponseSlaLowHours() == null
                || request.getAutoAssignMode() == null
                || request.getAutoAssignFallbackRoundRobin() == null
                || request.getTicketEmailNotificationsEnabled() == null
                || request.getTicketSmsNotificationsEnabled() == null
                || request.getAgentDigestEnabled() == null
                || request.getAgentDigestSendHour() == null
                || request.getAgentDigestSendMinute() == null
                || request.getSlaUseBusinessHours() == null
                || !StringUtils.hasText(request.getSlaTimezone())
                || request.getSlaWarnEnabled() == null
                || request.getSlaBreachEscalationEnabled() == null
                || request.getSlaBreachBumpPriority() == null) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }

        int days = request.getReopenWindowDays();
        int maxAttachments = request.getMaxAttachments();
        int maxSizeMb = request.getMaxAttachmentSizeMb();
        int autoArchiveDays = request.getAutoArchiveClosedAfterDays();
        int slaUrgent = request.getSlaUrgentHours();
        int slaHigh = request.getSlaHighHours();
        int slaMedium = request.getSlaMediumHours();
        int slaLow = request.getSlaLowHours();
        int firstResponseSlaUrgent = request.getFirstResponseSlaUrgentHours();
        int firstResponseSlaHigh = request.getFirstResponseSlaHighHours();
        int firstResponseSlaMedium = request.getFirstResponseSlaMediumHours();
        int firstResponseSlaLow = request.getFirstResponseSlaLowHours();
        TicketAutoAssignMode autoAssignMode = request.getAutoAssignMode();
        boolean autoAssignFallback = request.getAutoAssignFallbackRoundRobin();
        boolean ticketEmailNotificationsEnabled = request.getTicketEmailNotificationsEnabled();
        boolean ticketSmsNotificationsEnabled = request.getTicketSmsNotificationsEnabled();
        boolean agentDigestEnabled = request.getAgentDigestEnabled();
        int agentDigestSendHour = request.getAgentDigestSendHour();
        int agentDigestSendMinute = request.getAgentDigestSendMinute();
        if (days < 1 || days > 3650
                || maxAttachments < 1 || maxAttachments > 20
                || maxSizeMb < 1 || maxSizeMb > 50
                || autoArchiveDays < 0 || autoArchiveDays > 3650
                || slaUrgent < 1 || slaUrgent > 8760
                || slaHigh < 1 || slaHigh > 8760
                || slaMedium < 1 || slaMedium > 8760
                || slaLow < 1 || slaLow > 8760
                || firstResponseSlaUrgent < 1 || firstResponseSlaUrgent > 8760
                || firstResponseSlaHigh < 1 || firstResponseSlaHigh > 8760
                || firstResponseSlaMedium < 1 || firstResponseSlaMedium > 8760
                || firstResponseSlaLow < 1 || firstResponseSlaLow > 8760
                || agentDigestSendHour < 0 || agentDigestSendHour > 23
                || agentDigestSendMinute < 0 || agentDigestSendMinute > 59) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }

        Set<TicketAttachmentKind> kinds = parseKinds(request.getAllowedAttachmentKinds());
        if (kinds.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }
        Set<TicketPriority> emailPriorities = TicketSettings.parsePriorities(
                String.join(",", request.getEmailNotificationPriorities() == null
                        ? List.<String>of()
                        : request.getEmailNotificationPriorities()));
        Set<TicketPriority> smsPriorities = TicketSettings.parsePriorities(
                String.join(",", request.getSmsNotificationPriorities() == null
                        ? List.<String>of()
                        : request.getSmsNotificationPriorities()));
        if (emailPriorities.isEmpty() || smsPriorities.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }

        ZoneId slaZone;
        try {
            slaZone = ZoneId.of(request.getSlaTimezone().trim());
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }

        BusinessHoursWeekDto businessHours = request.getBusinessHours() != null
                ? request.getBusinessHours()
                : businessHoursCalculator.defaultWeek();
        if (!businessHoursCalculator.validateWeek(businessHours).isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }

        List<BusinessHolidayDto> businessHolidays = request.getBusinessHolidays() != null
                ? request.getBusinessHolidays()
                : List.of();
        if (businessHolidays.size() > MAX_BUSINESS_HOLIDAYS) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }
        for (BusinessHolidayDto holiday : businessHolidays) {
            if (holiday == null || !StringUtils.hasText(holiday.getDate())) {
                throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
            }
            try {
                LocalDate.parse(holiday.getDate().trim());
            } catch (DateTimeParseException ex) {
                throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
            }
        }

        boolean slaWarnEnabled = request.getSlaWarnEnabled();
        Integer slaWarnHoursBefore = request.getSlaWarnHoursBefore();
        if (slaWarnEnabled) {
            if (slaWarnHoursBefore == null || slaWarnHoursBefore < 1 || slaWarnHoursBefore > 8760) {
                throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
            }
        } else if (slaWarnHoursBefore != null
                && (slaWarnHoursBefore < 1 || slaWarnHoursBefore > 8760)) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }

        Long slaBreachAssigneeId = request.getSlaBreachAssigneeId();
        if (slaBreachAssigneeId != null) {
            User assignee = userRepository.findById(slaBreachAssigneeId)
                    .orElseThrow(() -> new ApiException(ErrorCode.TICKET_ASSIGNEE_NOT_FOUND));
            if (assignee.getRole() != User.Role.ADMIN || !assignee.isEnabled()) {
                throw new ApiException(ErrorCode.TICKET_ASSIGNEE_INVALID);
            }
        }

        Long slaBreachQueueId = request.getSlaBreachQueueId();
        if (slaBreachQueueId != null) {
            ticketQueueService.requireActiveQueue(slaBreachQueueId);
        }

        TicketSettings settings = getOrCreate();
        settings.setReopenWindowDays(days);
        settings.setMaxAttachments(maxAttachments);
        settings.setMaxAttachmentSizeMb(maxSizeMb);
        settings.setAutoArchiveClosedAfterDays(autoArchiveDays);
        settings.setSlaUrgentHours(slaUrgent);
        settings.setSlaHighHours(slaHigh);
        settings.setSlaMediumHours(slaMedium);
        settings.setSlaLowHours(slaLow);
        settings.setFirstResponseSlaUrgentHours(firstResponseSlaUrgent);
        settings.setFirstResponseSlaHighHours(firstResponseSlaHigh);
        settings.setFirstResponseSlaMediumHours(firstResponseSlaMedium);
        settings.setFirstResponseSlaLowHours(firstResponseSlaLow);
        settings.setAutoAssignMode(autoAssignMode);
        settings.setAutoAssignFallbackRoundRobin(autoAssignFallback);
        if (request.getDefaultQueueId() != null) {
            ticketQueueService.requireActiveQueue(request.getDefaultQueueId());
            settings.setDefaultQueueId(request.getDefaultQueueId());
        } else {
            settings.setDefaultQueueId(null);
        }
        settings.setTicketEmailNotificationsEnabled(ticketEmailNotificationsEnabled);
        settings.setTicketSmsNotificationsEnabled(ticketSmsNotificationsEnabled);
        settings.setEmailNotificationPriorities(TicketSettings.toPriorityCsv(emailPriorities));
        settings.setSmsNotificationPriorities(TicketSettings.toPriorityCsv(smsPriorities));
        settings.setAgentDigestEnabled(agentDigestEnabled);
        settings.setAgentDigestSendHour(agentDigestSendHour);
        settings.setAgentDigestSendMinute(agentDigestSendMinute);
        settings.setAllowedAttachmentKinds(TicketAttachmentKind.toCsv(kinds));
        settings.setSlaUseBusinessHours(request.getSlaUseBusinessHours());
        settings.setSlaTimezone(slaZone.getId());
        settings.setBusinessHoursJson(businessHoursCalculator.serializeWeek(businessHours));
        settings.setBusinessHolidaysJson(businessHoursCalculator.serializeHolidays(businessHolidays));
        settings.setSlaWarnEnabled(slaWarnEnabled);
        settings.setSlaWarnHoursBefore(slaWarnHoursBefore != null
                ? slaWarnHoursBefore
                : TicketSettings.DEFAULT_SLA_WARN_HOURS_BEFORE);
        settings.setSlaBreachEscalationEnabled(request.getSlaBreachEscalationEnabled());
        settings.setSlaBreachBumpPriority(request.getSlaBreachBumpPriority());
        settings.setSlaBreachAssigneeId(slaBreachAssigneeId);
        settings.setSlaBreachQueueId(slaBreachQueueId);
        settings.normalize();
        return toDto(ticketSettingsRepository.save(settings));
    }

    /**
     * True when agent digest is enabled, current time is at/after the configured send time,
     * and not already run today.
     */
    @Transactional
    public boolean shouldRunAgentDigestNow() {
        TicketSettings settings = getOrCreate();
        if (!settings.isAgentDigestEnabled()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (settings.getAgentDigestLastRunDate() != null && today.equals(settings.getAgentDigestLastRunDate())) {
            return false;
        }
        LocalTime sendAt = LocalTime.of(
                coerce(settings.getAgentDigestSendHour(), 0, 23, 8),
                coerce(settings.getAgentDigestSendMinute(), 0, 59, 0));
        return !LocalTime.now().isBefore(sendAt);
    }

    @Transactional
    public void markAgentDigestRanToday() {
        TicketSettings settings = getOrCreate();
        settings.setAgentDigestLastRunDate(LocalDate.now());
        ticketSettingsRepository.save(settings);
    }

    private static int coerce(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }

    public void validateAttachmentBatch(List<MultipartFile> files) {
        TicketSettings settings = getOrCreate();
        if (files.size() > settings.getMaxAttachments()) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENTS_LIMIT);
        }
        for (MultipartFile file : files) {
            validateAttachmentFile(file, settings);
        }
    }

    public void validateAttachmentFile(MultipartFile file, TicketSettings settings) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }
        long maxBytes = settings.maxAttachmentBytes();
        if (file.getSize() > maxBytes) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }
        if (!isAllowedFile(file, settings.resolvedAttachmentKinds())) {
            throw new ApiException(ErrorCode.TICKET_ATTACHMENT_INVALID);
        }
    }

    /** Resolve SLA due date using org settings only (no category override). */
    @Transactional(readOnly = true)
    public boolean isApproachingSla(Ticket ticket, Instant now) {
        if (ticket == null || now == null) {
            return false;
        }
        TicketSettings settings = getOrCreate();
        if (!settings.isSlaWarnEnabled()) {
            return false;
        }
        if (ticket.getSlaPausedAt() != null) {
            return false;
        }
        Instant dueAt = ticket.getDueAt();
        if (dueAt == null || !dueAt.isAfter(now)) {
            return false;
        }
        TicketStatus status = ticket.getStatus();
        if (status != TicketStatus.NEW && status != TicketStatus.OPEN && status != TicketStatus.ON_HOLD) {
            return false;
        }
        Instant warnUntil = now.plus(Duration.ofHours(settings.getSlaWarnHoursBefore()));
        return !dueAt.isAfter(warnUntil);
    }

    @Transactional(readOnly = true)
    public Instant computeDueAt(TicketPriority priority, Instant from) {
        return computeResolveDueAt(priority, null, from);
    }

    @Transactional(readOnly = true)
    public int resolveHours(TicketPriority priority, TicketCategory category) {
        if (priority == null) {
            throw new IllegalArgumentException("priority is required");
        }
        if (category != null) {
            Integer override = category.resolveSlaHoursFor(priority);
            if (override != null) {
                return override;
            }
        }
        TicketSettings settings = getOrCreate();
        return switch (priority) {
            case URGENT -> settings.getSlaUrgentHours();
            case HIGH -> settings.getSlaHighHours();
            case MEDIUM -> settings.getSlaMediumHours();
            case LOW -> settings.getSlaLowHours();
        };
    }

    @Transactional(readOnly = true)
    public int firstResponseHours(TicketPriority priority, TicketCategory category) {
        if (priority == null) {
            throw new IllegalArgumentException("priority is required");
        }
        if (category != null) {
            Integer override = category.firstResponseSlaHoursFor(priority);
            if (override != null) {
                return override;
            }
        }
        TicketSettings settings = getOrCreate();
        return switch (priority) {
            case URGENT -> settings.getFirstResponseSlaUrgentHours();
            case HIGH -> settings.getFirstResponseSlaHighHours();
            case MEDIUM -> settings.getFirstResponseSlaMediumHours();
            case LOW -> settings.getFirstResponseSlaLowHours();
        };
    }

    @Transactional(readOnly = true)
    public Instant computeResolveDueAt(TicketPriority priority, TicketCategory category, Instant from) {
        if (priority == null || from == null) {
            return null;
        }
        long hours = resolveHours(priority, category);
        return computeSlaDueAt(from, hours);
    }

    @Transactional(readOnly = true)
    public Instant computeFirstResponseDueAt(TicketPriority priority, TicketCategory category, Instant from) {
        if (priority == null || from == null) {
            return null;
        }
        long hours = firstResponseHours(priority, category);
        return computeSlaDueAt(from, hours);
    }

    private Instant computeSlaDueAt(Instant from, long hours) {
        TicketSettings settings = getOrCreate();
        if (!settings.isSlaUseBusinessHours()) {
            return from.plusSeconds(hours * 3600L);
        }
        return businessHoursCalculator.addBusinessHours(
                from, hours, getSlaZone(settings), getBusinessHoursWeek(settings), getBusinessHolidays(settings));
    }

    @Transactional(readOnly = true)
    public Instant shiftDueAfterPause(Instant dueAt, Instant pausedAt, Instant resumedAt) {
        if (dueAt == null || pausedAt == null || resumedAt == null) {
            return dueAt;
        }
        TicketSettings settings = getOrCreate();
        long remainingMinutes;
        if (settings.isSlaUseBusinessHours()) {
            remainingMinutes = businessHoursCalculator.businessMinutesBetween(
                    pausedAt, dueAt, getSlaZone(settings), getBusinessHoursWeek(settings), getBusinessHolidays(settings));
        } else {
            remainingMinutes = java.time.Duration.between(pausedAt, dueAt).toMinutes();
        }
        remainingMinutes = Math.max(0, remainingMinutes);
        if (remainingMinutes == 0) {
            return resumedAt;
        }
        if (settings.isSlaUseBusinessHours()) {
            return businessHoursCalculator.addBusinessMinutes(
                    resumedAt, remainingMinutes, getSlaZone(settings), getBusinessHoursWeek(settings),
                    getBusinessHolidays(settings));
        }
        return resumedAt.plusSeconds(remainingMinutes * 60L);
    }

    private ZoneId getSlaZone(TicketSettings settings) {
        return ZoneId.of(settings.getSlaTimezone());
    }

    private BusinessHoursWeekDto getBusinessHoursWeek(TicketSettings settings) {
        return businessHoursCalculator.parseWeek(settings.getBusinessHoursJson());
    }

    private Set<LocalDate> getBusinessHolidays(TicketSettings settings) {
        return businessHoursCalculator.toHolidayDates(
                businessHoursCalculator.parseHolidays(settings.getBusinessHolidaysJson()));
    }

    public boolean isAllowedFile(MultipartFile file, Set<TicketAttachmentKind> kinds) {
        if (kinds == null || kinds.isEmpty()) {
            kinds = EnumSet.allOf(TicketAttachmentKind.class);
        }
        String contentType = file.getContentType();
        String normalizedType = contentType != null ? contentType.toLowerCase(Locale.ROOT).trim() : "";
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = extensionOf(fileName);

        for (TicketAttachmentKind kind : kinds) {
            if (StringUtils.hasText(normalizedType) && kind.getContentTypes().contains(normalizedType)) {
                return true;
            }
            if (StringUtils.hasText(extension) && kind.getExtensions().contains(extension)) {
                return true;
            }
        }
        return false;
    }

    private static String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        String name = fileName.trim().toLowerCase(Locale.ROOT);
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < name.length()) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return name.substring(dot);
    }

    private Set<TicketAttachmentKind> parseKinds(List<String> tokens) {
        Set<TicketAttachmentKind> kinds = new LinkedHashSet<>();
        if (tokens == null) {
            return kinds;
        }
        for (String token : tokens) {
            TicketAttachmentKind.fromToken(token).ifPresent(kinds::add);
        }
        return kinds;
    }

    private TicketSettingsDto toDto(TicketSettings settings) {
        List<String> kinds = settings.resolvedAttachmentKinds().stream()
                .map(Enum::name)
                .toList();
        List<String> emailPriorities = settings.resolvedEmailNotificationPriorities().stream()
                .map(Enum::name)
                .toList();
        List<String> smsPriorities = settings.resolvedSmsNotificationPriorities().stream()
                .map(Enum::name)
                .toList();
        TicketSettingsDto dto = new TicketSettingsDto(
                settings.getReopenWindowDays(),
                settings.getMaxAttachments(),
                settings.getMaxAttachmentSizeMb(),
                kinds,
                settings.getAutoArchiveClosedAfterDays(),
                settings.getSlaUrgentHours(),
                settings.getSlaHighHours(),
                settings.getSlaMediumHours(),
                settings.getSlaLowHours(),
                settings.getFirstResponseSlaUrgentHours(),
                settings.getFirstResponseSlaHighHours(),
                settings.getFirstResponseSlaMediumHours(),
                settings.getFirstResponseSlaLowHours(),
                settings.getAutoAssignMode() != null ? settings.getAutoAssignMode() : TicketAutoAssignMode.OFF,
                settings.isAutoAssignFallbackRoundRobin(),
                settings.isTicketEmailNotificationsEnabled(),
                settings.isTicketSmsNotificationsEnabled(),
                emailPriorities,
                smsPriorities,
                settings.isAgentDigestEnabled(),
                settings.getAgentDigestSendHour(),
                settings.getAgentDigestSendMinute()
        );
        dto.setDefaultQueueId(settings.getDefaultQueueId());
        dto.setSlaUseBusinessHours(settings.isSlaUseBusinessHours());
        dto.setSlaTimezone(settings.getSlaTimezone());
        dto.setBusinessHours(businessHoursCalculator.parseWeek(settings.getBusinessHoursJson()));
        dto.setBusinessHolidays(businessHoursCalculator.parseHolidays(settings.getBusinessHolidaysJson()));
        dto.setSlaWarnEnabled(settings.isSlaWarnEnabled());
        dto.setSlaWarnHoursBefore(settings.getSlaWarnHoursBefore());
        dto.setSlaBreachEscalationEnabled(settings.isSlaBreachEscalationEnabled());
        dto.setSlaBreachBumpPriority(settings.isSlaBreachBumpPriority());
        dto.setSlaBreachAssigneeId(settings.getSlaBreachAssigneeId());
        dto.setSlaBreachQueueId(settings.getSlaBreachQueueId());
        return dto;
    }

    private TicketAttachmentPolicyDto toPolicy(TicketSettings settings) {
        Set<TicketAttachmentKind> kinds = settings.resolvedAttachmentKinds();
        Set<String> contentTypes = new LinkedHashSet<>();
        Set<String> extensions = new LinkedHashSet<>();
        List<String> kindNames = new ArrayList<>();
        for (TicketAttachmentKind kind : kinds) {
            kindNames.add(kind.name());
            contentTypes.addAll(kind.getContentTypes());
            extensions.addAll(kind.getExtensions());
        }
        TicketAttachmentPolicyDto dto = new TicketAttachmentPolicyDto();
        dto.setMaxAttachments(settings.getMaxAttachments());
        dto.setMaxAttachmentSizeMb(settings.getMaxAttachmentSizeMb());
        dto.setMaxAttachmentBytes(settings.maxAttachmentBytes());
        dto.setAllowedAttachmentKinds(kindNames);
        dto.setAllowedContentTypes(new ArrayList<>(contentTypes));
        dto.setAllowedExtensions(new ArrayList<>(extensions));
        return dto;
    }
}
