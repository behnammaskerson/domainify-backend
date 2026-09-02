package com.domainify.service;

import com.domainify.dto.TicketCategoryDto;
import com.domainify.dto.TicketCategoryRequest;
import com.domainify.entity.TicketCategory;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketCategoryRepository;
import com.domainify.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TicketCategoryService {

    /** Legacy seed codes from an earlier release; removed when unused. */
    private static final List<String> LEGACY_DEFAULT_CODES = List.of(
            "BILLING", "TECHNICAL", "DOMAINS", "SMS", "ACCOUNT", "OTHER"
    );

    private final TicketCategoryRepository ticketCategoryRepository;
    private final TicketRepository ticketRepository;

    public TicketCategoryService(
            TicketCategoryRepository ticketCategoryRepository,
            TicketRepository ticketRepository) {
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Deletes previously seeded default categories that are not referenced by any ticket.
     * Categories still in use are left alone (admins can deactivate via UI).
     */
    @Transactional
    public void removeUnusedLegacyDefaults() {
        for (String code : LEGACY_DEFAULT_CODES) {
            ticketCategoryRepository.findByCodeIgnoreCase(code).ifPresent(category -> {
                if (ticketRepository.countByCategory(category) == 0) {
                    ticketCategoryRepository.delete(category);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public List<TicketCategoryDto> listActive() {
        return ticketCategoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketCategoryDto> listAll() {
        return ticketCategoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketCategory requireActiveCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ApiException(ErrorCode.TICKET_CATEGORY_REQUIRED);
        }
        TicketCategory category = ticketCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new ApiException(ErrorCode.TICKET_CATEGORY_INACTIVE);
        }
        return category;
    }

    @Transactional
    public TicketCategoryDto create(TicketCategoryRequest request) {
        String name = normalizeName(request.getName());
        String code = normalizeCode(StringUtils.hasText(request.getCode()) ? request.getCode() : name);
        if (!StringUtils.hasText(code)) {
            throw new ApiException(ErrorCode.TICKET_CATEGORY_CODE_INVALID);
        }
        if (ticketCategoryRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(ErrorCode.TICKET_CATEGORY_CODE_EXISTS);
        }

        TicketCategory category = new TicketCategory();
        category.setName(name);
        category.setCode(code);
        category.setActive(request.getActive() == null || request.getActive());
        category.setSortOrder(request.getSortOrder() == null
                ? nextSortOrder()
                : request.getSortOrder());
        return toDto(ticketCategoryRepository.save(category));
    }

    @Transactional
    public TicketCategoryDto update(Long id, TicketCategoryRequest request) {
        TicketCategory category = ticketCategoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_CATEGORY_NOT_FOUND));

        if (StringUtils.hasText(request.getName())) {
            category.setName(normalizeName(request.getName()));
        }
        if (StringUtils.hasText(request.getCode())) {
            String code = normalizeCode(request.getCode());
            if (!StringUtils.hasText(code)) {
                throw new ApiException(ErrorCode.TICKET_CATEGORY_CODE_INVALID);
            }
            if (ticketCategoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new ApiException(ErrorCode.TICKET_CATEGORY_CODE_EXISTS);
            }
            category.setCode(code);
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        return toDto(ticketCategoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        TicketCategory category = ticketCategoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_CATEGORY_NOT_FOUND));

        long usage = ticketRepository.countByCategory(category);
        if (usage > 0) {
            category.setActive(false);
            ticketCategoryRepository.save(category);
            return;
        }
        ticketCategoryRepository.delete(category);
    }

    private int nextSortOrder() {
        return ticketCategoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .mapToInt(TicketCategory::getSortOrder)
                .max()
                .orElse(0) + 10;
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ApiException(ErrorCode.TICKET_CATEGORY_NAME_REQUIRED);
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new ApiException(ErrorCode.TICKET_CATEGORY_NAME_INVALID);
        }
        return trimmed;
    }

    private String normalizeCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }
        return normalized;
    }

    public TicketCategoryDto toDto(TicketCategory category) {
        return new TicketCategoryDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.isActive(),
                category.getSortOrder()
        );
    }
}
