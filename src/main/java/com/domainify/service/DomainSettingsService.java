package com.domainify.service;

import com.domainify.dto.DomainSettingsDto;
import com.domainify.dto.DomainSettingsUpdateRequest;
import com.domainify.entity.Domain;
import com.domainify.entity.DomainSettings;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.DomainSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DomainSettingsService {

    private static final Logger log = LoggerFactory.getLogger(DomainSettingsService.class);

    private final DomainSettingsRepository settingsRepository;

    @Value("${app.domain.renewal.windows:90,60,30}")
    private String defaultWindows;

    @Value("${app.domain.renewal.send-hour:9}")
    private int defaultSendHour;

    @Value("${app.domain.renewal.send-minute:0}")
    private int defaultSendMinute;

    public DomainSettingsService(DomainSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Transactional
    public DomainSettings getOrCreate() {
        return settingsRepository.findById(DomainSettings.SINGLETON_ID)
                .orElseGet(() -> settingsRepository.save(seedDefaults()));
    }

    @Transactional(readOnly = true)
    public DomainSettingsDto getDto() {
        return toDto(getOrCreate());
    }

    @Transactional
    public DomainSettingsDto update(DomainSettingsUpdateRequest request) {
        DomainSettings settings = getOrCreate();
        List<Integer> windows = validateWindowsList(request.getRenewalWindows());
        int hour = requireInRange(request.getRenewalSendHour(), 0, 23);
        int minute = requireInRange(request.getRenewalSendMinute(), 0, 59);

        boolean master = Boolean.TRUE.equals(request.getRenewalRemindersEnabled());
        boolean inApp = Boolean.TRUE.equals(request.getRenewalInAppEnabled());
        boolean email = Boolean.TRUE.equals(request.getRenewalEmailEnabled());
        boolean sms = Boolean.TRUE.equals(request.getRenewalSmsEnabled());
        if (master && !inApp && !email && !sms) {
            throw new ApiException(ErrorCode.DOMAIN_RENEWAL_SETTINGS_INVALID);
        }

        settings.setRenewalRemindersEnabled(master);
        settings.setRenewalInAppEnabled(inApp);
        settings.setRenewalEmailEnabled(email);
        settings.setRenewalSmsEnabled(sms);
        settings.setRenewalWindows(formatWindows(windows));
        settings.setRenewalSendHour(hour);
        settings.setRenewalSendMinute(minute);
        return toDto(settingsRepository.save(settings));
    }

    /**
     * True when reminders are enabled, at least one channel is on,
     * current time is at/after the configured send time, and not already run today.
     */
    @Transactional
    public boolean shouldRunNow() {
        DomainSettings settings = getOrCreate();
        if (!settings.isRenewalRemindersEnabled()) {
            return false;
        }
        if (!settings.isRenewalInAppEnabled()
                && !settings.isRenewalEmailEnabled()
                && !settings.isRenewalSmsEnabled()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (settings.getRenewalLastRunDate() != null && today.equals(settings.getRenewalLastRunDate())) {
            return false;
        }
        LocalTime sendAt = LocalTime.of(
                coerce(settings.getRenewalSendHour(), 0, 23, DomainSettings.DEFAULT_SEND_HOUR),
                coerce(settings.getRenewalSendMinute(), 0, 59, DomainSettings.DEFAULT_SEND_MINUTE));
        return !LocalTime.now().isBefore(sendAt);
    }

    @Transactional
    public void markRanToday() {
        DomainSettings settings = getOrCreate();
        settings.setRenewalLastRunDate(LocalDate.now());
        settingsRepository.save(settings);
    }

    public List<Integer> resolveWindows(DomainSettings settings) {
        if (settings == null || !StringUtils.hasText(settings.getRenewalWindows())) {
            List<Integer> fallback = parseWindowsLenient(defaultWindows);
            return fallback.isEmpty() ? List.of(90, 60, 30) : fallback;
        }
        List<Integer> parsed = parseWindowsLenient(settings.getRenewalWindows());
        if (!parsed.isEmpty()) {
            return parsed;
        }
        List<Integer> fallback = parseWindowsLenient(defaultWindows);
        return fallback.isEmpty() ? List.of(90, 60, 30) : fallback;
    }

    public List<Integer> resolveWindowsForDomain(Domain domain, DomainSettings settings) {
        if (domain != null && StringUtils.hasText(domain.getRenewalWindows())) {
            List<Integer> custom = parseWindowsLenient(domain.getRenewalWindows());
            if (!custom.isEmpty()) {
                return custom;
            }
        }
        return resolveWindows(settings);
    }

    public int maxHorizonDays(DomainSettings settings, Iterable<Domain> domainsWithCustomWindows) {
        int max = resolveWindows(settings).stream().mapToInt(Integer::intValue).max().orElse(90);
        if (domainsWithCustomWindows != null) {
            for (Domain domain : domainsWithCustomWindows) {
                for (Integer days : parseWindowsLenient(domain.getRenewalWindows())) {
                    max = Math.max(max, days);
                }
            }
        }
        return Math.max(1, Math.min(max, 3650));
    }

    public List<Integer> validateWindowsList(List<Integer> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new ApiException(ErrorCode.DOMAIN_RENEWAL_SETTINGS_INVALID);
        }
        Set<Integer> values = new LinkedHashSet<>();
        for (Integer days : raw) {
            if (days == null || days < 1 || days > 3650) {
                throw new ApiException(ErrorCode.DOMAIN_RENEWAL_SETTINGS_INVALID);
            }
            values.add(days);
        }
        if (values.isEmpty()) {
            throw new ApiException(ErrorCode.DOMAIN_RENEWAL_SETTINGS_INVALID);
        }
        return values.stream().sorted().collect(Collectors.toList());
    }

    public String formatWindows(List<Integer> windows) {
        return windows.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Integer> parseWindowsLenient(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            try {
                int days = Integer.parseInt(part.trim());
                if (days > 0 && days <= 3650) {
                    result.add(days);
                }
            } catch (NumberFormatException ignored) {
                log.warn("Ignoring invalid domain renewal window: {}", part);
            }
        }
        return result.stream().distinct().sorted().collect(Collectors.toList());
    }

    private int requireInRange(Integer value, int min, int max) {
        if (value == null || value < min || value > max) {
            throw new ApiException(ErrorCode.DOMAIN_RENEWAL_SETTINGS_INVALID);
        }
        return value;
    }

    private int coerce(int value, int min, int max, int fallback) {
        return value < min || value > max ? fallback : value;
    }

    private DomainSettings seedDefaults() {
        DomainSettings settings = DomainSettings.defaults();
        List<Integer> windows = parseWindowsLenient(defaultWindows);
        if (windows.isEmpty()) {
            windows = List.of(90, 60, 30);
        }
        settings.setRenewalWindows(formatWindows(windows));
        int hour = defaultSendHour;
        if (hour < 0 || hour > 23) {
            hour = DomainSettings.DEFAULT_SEND_HOUR;
        }
        int minute = defaultSendMinute;
        if (minute < 0 || minute > 59) {
            minute = DomainSettings.DEFAULT_SEND_MINUTE;
        }
        settings.setRenewalSendHour(hour);
        settings.setRenewalSendMinute(minute);
        settings.setRenewalRemindersEnabled(true);
        settings.setRenewalInAppEnabled(true);
        settings.setRenewalEmailEnabled(true);
        settings.setRenewalSmsEnabled(true);
        return settings;
    }

    private DomainSettingsDto toDto(DomainSettings settings) {
        DomainSettingsDto dto = new DomainSettingsDto();
        dto.setRenewalRemindersEnabled(settings.isRenewalRemindersEnabled());
        dto.setRenewalInAppEnabled(settings.isRenewalInAppEnabled());
        dto.setRenewalEmailEnabled(settings.isRenewalEmailEnabled());
        dto.setRenewalSmsEnabled(settings.isRenewalSmsEnabled());
        dto.setRenewalWindows(settings.getRenewalWindows());
        dto.setRenewalWindowsParsed(resolveWindows(settings));
        dto.setRenewalSendHour(settings.getRenewalSendHour());
        dto.setRenewalSendMinute(settings.getRenewalSendMinute());
        dto.setRenewalLastRunDate(settings.getRenewalLastRunDate());
        dto.setUpdatedAt(settings.getUpdatedAt());
        return dto;
    }
}
