package com.domainify.service;

import com.domainify.dto.TicketAttachmentPolicyDto;
import com.domainify.dto.TicketSettingsDto;
import com.domainify.entity.TicketAttachmentKind;
import com.domainify.entity.TicketAutoAssignMode;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketSettings;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketSettingsRepository;
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
import java.time.Instant;

@Service
public class TicketSettingsService {

    private final TicketSettingsRepository ticketSettingsRepository;
    private final TicketQueueService ticketQueueService;

    public TicketSettingsService(
            TicketSettingsRepository ticketSettingsRepository,
            TicketQueueService ticketQueueService) {
        this.ticketSettingsRepository = ticketSettingsRepository;
        this.ticketQueueService = ticketQueueService;
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
                || request.getAutoAssignMode() == null
                || request.getAutoAssignFallbackRoundRobin() == null
                || request.getTicketEmailNotificationsEnabled() == null
                || request.getTicketSmsNotificationsEnabled() == null) {
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
        TicketAutoAssignMode autoAssignMode = request.getAutoAssignMode();
        boolean autoAssignFallback = request.getAutoAssignFallbackRoundRobin();
        boolean ticketEmailNotificationsEnabled = request.getTicketEmailNotificationsEnabled();
        boolean ticketSmsNotificationsEnabled = request.getTicketSmsNotificationsEnabled();
        if (days < 1 || days > 3650
                || maxAttachments < 1 || maxAttachments > 20
                || maxSizeMb < 1 || maxSizeMb > 50
                || autoArchiveDays < 0 || autoArchiveDays > 3650
                || slaUrgent < 1 || slaUrgent > 8760
                || slaHigh < 1 || slaHigh > 8760
                || slaMedium < 1 || slaMedium > 8760
                || slaLow < 1 || slaLow > 8760) {
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

        TicketSettings settings = getOrCreate();
        settings.setReopenWindowDays(days);
        settings.setMaxAttachments(maxAttachments);
        settings.setMaxAttachmentSizeMb(maxSizeMb);
        settings.setAutoArchiveClosedAfterDays(autoArchiveDays);
        settings.setSlaUrgentHours(slaUrgent);
        settings.setSlaHighHours(slaHigh);
        settings.setSlaMediumHours(slaMedium);
        settings.setSlaLowHours(slaLow);
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
        settings.setAllowedAttachmentKinds(TicketAttachmentKind.toCsv(kinds));
        settings.normalize();
        return toDto(ticketSettingsRepository.save(settings));
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

    @Transactional(readOnly = true)
    public Instant computeDueAt(TicketPriority priority, Instant from) {
        if (priority == null || from == null) {
            return null;
        }
        TicketSettings settings = getOrCreate();
        long hours = switch (priority) {
            case URGENT -> settings.getSlaUrgentHours();
            case HIGH -> settings.getSlaHighHours();
            case MEDIUM -> settings.getSlaMediumHours();
            case LOW -> settings.getSlaLowHours();
        };
        return from.plusSeconds(hours * 3600L);
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
                settings.getAutoAssignMode() != null ? settings.getAutoAssignMode() : TicketAutoAssignMode.OFF,
                settings.isAutoAssignFallbackRoundRobin(),
                settings.isTicketEmailNotificationsEnabled(),
                settings.isTicketSmsNotificationsEnabled(),
                emailPriorities,
                smsPriorities
        );
        dto.setDefaultQueueId(settings.getDefaultQueueId());
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
