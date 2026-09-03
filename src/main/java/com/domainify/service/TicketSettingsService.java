package com.domainify.service;

import com.domainify.dto.TicketSettingsDto;
import com.domainify.entity.TicketSettings;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketSettingsService {

    private final TicketSettingsRepository ticketSettingsRepository;

    public TicketSettingsService(TicketSettingsRepository ticketSettingsRepository) {
        this.ticketSettingsRepository = ticketSettingsRepository;
    }

    @Transactional
    public TicketSettings getOrCreate() {
        return ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .orElseGet(() -> ticketSettingsRepository.save(TicketSettings.defaults()));
    }

    @Transactional
    public TicketSettingsDto getDto() {
        return toDto(getOrCreate());
    }

    @Transactional(readOnly = true)
    public int getReopenWindowDays() {
        return Math.max(1, ticketSettingsRepository.findById(TicketSettings.SINGLETON_ID)
                .map(TicketSettings::getReopenWindowDays)
                .orElse(TicketSettings.DEFAULT_REOPEN_WINDOW_DAYS));
    }

    @Transactional
    public TicketSettingsDto update(TicketSettingsDto request) {
        if (request == null || request.getReopenWindowDays() == null) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }
        int days = request.getReopenWindowDays();
        if (days < 1 || days > 3650) {
            throw new ApiException(ErrorCode.TICKET_SETTINGS_INVALID);
        }
        TicketSettings settings = getOrCreate();
        settings.setReopenWindowDays(days);
        return toDto(ticketSettingsRepository.save(settings));
    }

    private TicketSettingsDto toDto(TicketSettings settings) {
        return new TicketSettingsDto(settings.getReopenWindowDays());
    }
}
