package com.domainify.service;

import com.domainify.dto.TicketTagDto;
import com.domainify.dto.TicketTagRequest;
import com.domainify.entity.TicketTag;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketRepository;
import com.domainify.repository.TicketTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TicketTagService {

    private static final int NAME_MAX = 64;
    private static final int MAX_TAGS_PER_TICKET = 20;

    private final TicketTagRepository ticketTagRepository;
    private final TicketRepository ticketRepository;

    public TicketTagService(TicketTagRepository ticketTagRepository, TicketRepository ticketRepository) {
        this.ticketTagRepository = ticketTagRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketTagDto> listAll() {
        return ticketTagRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TicketTagDto create(TicketTagRequest request) {
        String name = requireValidName(request != null ? request.getName() : null);
        if (ticketTagRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException(ErrorCode.TICKET_TAG_EXISTS);
        }
        TicketTag tag = new TicketTag();
        tag.setName(name);
        return toDto(ticketTagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        TicketTag tag = ticketTagRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_TAG_NOT_FOUND));
        if (ticketRepository.countByTag(tag) > 0) {
            throw new ApiException(ErrorCode.TICKET_TAG_IN_USE);
        }
        ticketTagRepository.delete(tag);
    }

    /**
     * Resolve a mix of existing tag IDs and free-form names into persisted tags.
     * Names create tags on demand (catalogue grows with free-form use).
     */
    @Transactional
    public Set<TicketTag> resolveTags(List<Long> tagIds, List<String> names) {
        Set<TicketTag> resolved = new LinkedHashSet<>();
        Set<Long> seenIds = new HashSet<>();
        Set<String> seenNames = new HashSet<>();

        if (tagIds != null) {
            for (Long id : tagIds) {
                if (id == null || !seenIds.add(id)) {
                    continue;
                }
                TicketTag tag = ticketTagRepository.findById(id)
                        .orElseThrow(() -> new ApiException(ErrorCode.TICKET_TAG_NOT_FOUND));
                resolved.add(tag);
                seenNames.add(tag.getName().toLowerCase(Locale.ROOT));
            }
        }

        if (names != null) {
            for (String raw : names) {
                String name = normalizeName(raw);
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                if (name.length() > NAME_MAX) {
                    throw new ApiException(ErrorCode.TICKET_TAG_NAME_INVALID);
                }
                String key = name.toLowerCase(Locale.ROOT);
                if (!seenNames.add(key)) {
                    continue;
                }
                TicketTag tag = ticketTagRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                    TicketTag created = new TicketTag();
                    created.setName(name);
                    return ticketTagRepository.save(created);
                });
                resolved.add(tag);
                seenIds.add(tag.getId());
            }
        }

        if (resolved.size() > MAX_TAGS_PER_TICKET) {
            throw new ApiException(ErrorCode.TICKET_TAG_NAME_INVALID);
        }
        return resolved;
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

    private String requireValidName(String raw) {
        String name = normalizeName(raw);
        if (!StringUtils.hasText(name)) {
            throw new ApiException(ErrorCode.TICKET_TAG_NAME_REQUIRED);
        }
        if (name.length() > NAME_MAX) {
            throw new ApiException(ErrorCode.TICKET_TAG_NAME_INVALID);
        }
        return name;
    }
}
