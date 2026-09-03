package com.domainify.service;

import com.domainify.dto.TicketTagDto;
import com.domainify.entity.TicketTag;
import com.domainify.repository.TicketTagRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class TicketTagService implements ApplicationRunner {

    private static final List<String> DEFAULT_TAGS = List.of(
            "VIP", "Billing", "Escalated", "Follow-up"
    );

    private final TicketTagRepository ticketTagRepository;

    public TicketTagService(TicketTagRepository ticketTagRepository) {
        this.ticketTagRepository = ticketTagRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String name : DEFAULT_TAGS) {
            if (!ticketTagRepository.existsByNameIgnoreCase(name)) {
                TicketTag tag = new TicketTag();
                tag.setName(name);
                ticketTagRepository.save(tag);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TicketTagDto> listAll() {
        return ticketTagRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public TicketTagDto toDto(TicketTag tag) {
        return new TicketTagDto(tag.getId(), tag.getName());
    }

    public String normalizeName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }
}
