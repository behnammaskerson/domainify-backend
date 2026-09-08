package com.domainify.service;

import com.domainify.dto.TicketInboxSavedViewDto;
import com.domainify.dto.TicketInboxSavedViewFilterDto;
import com.domainify.dto.TicketInboxSavedViewRequest;
import com.domainify.entity.TicketInboxSavedView;
import com.domainify.entity.TicketInboxView;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketInboxSavedViewRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TicketInboxSavedViewService {

    private static final int MAX_VIEWS_PER_USER = 50;
    private static final int NAME_MAX = 80;

    private final TicketInboxSavedViewRepository repository;
    private final ObjectMapper objectMapper;

    public TicketInboxSavedViewService(TicketInboxSavedViewRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TicketInboxSavedViewDto> listMine(User user) {
        requireAgent(user);
        return repository.findByUserIdOrderBySortOrderAscIdAsc(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TicketInboxSavedViewDto create(User user, TicketInboxSavedViewRequest request) {
        requireAgent(user);
        String name = normalizeName(request.getName());
        if (repository.countByUserId(user.getId()) >= MAX_VIEWS_PER_USER) {
            throw new ApiException(ErrorCode.TICKET_SAVED_VIEW_LIMIT);
        }
        if (repository.existsByUserIdAndNameIgnoreCase(user.getId(), name)) {
            throw new ApiException(ErrorCode.TICKET_SAVED_VIEW_NAME_EXISTS);
        }

        TicketInboxSavedView entity = new TicketInboxSavedView();
        entity.setUser(user);
        entity.setName(name);
        entity.setFilterJson(serializeFilter(sanitizeFilter(request.getFilter())));
        entity.setSortOrder((int) repository.countByUserId(user.getId()));
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault) {
            repository.clearDefaultsForUser(user.getId());
        }
        entity.setDefault(makeDefault);
        return toDto(repository.save(entity));
    }

    @Transactional
    public TicketInboxSavedViewDto update(User user, Long id, TicketInboxSavedViewRequest request) {
        requireAgent(user);
        TicketInboxSavedView entity = requireOwned(user, id);
        String name = normalizeName(request.getName());
        if (repository.existsByUserIdAndNameIgnoreCaseAndIdNot(user.getId(), name, id)) {
            throw new ApiException(ErrorCode.TICKET_SAVED_VIEW_NAME_EXISTS);
        }
        entity.setName(name);
        entity.setFilterJson(serializeFilter(sanitizeFilter(request.getFilter())));
        if (request.getIsDefault() != null) {
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                repository.clearDefaultsForUser(user.getId());
                entity.setDefault(true);
            } else {
                entity.setDefault(false);
            }
        }
        return toDto(repository.save(entity));
    }

    @Transactional
    public TicketInboxSavedViewDto setDefault(User user, Long id) {
        requireAgent(user);
        TicketInboxSavedView entity = requireOwned(user, id);
        repository.clearDefaultsForUser(user.getId());
        entity.setDefault(true);
        return toDto(repository.save(entity));
    }

    @Transactional
    public void delete(User user, Long id) {
        requireAgent(user);
        TicketInboxSavedView entity = requireOwned(user, id);
        repository.delete(entity);
    }

    private TicketInboxSavedView requireOwned(User user, Long id) {
        if (id == null) {
            throw new ApiException(ErrorCode.TICKET_SAVED_VIEW_NOT_FOUND);
        }
        return repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_SAVED_VIEW_NOT_FOUND));
    }

    private String normalizeName(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new ApiException(ErrorCode.TICKET_SAVED_VIEW_NAME_REQUIRED);
        }
        String name = raw.trim();
        if (name.isEmpty() || name.length() > NAME_MAX) {
            throw new ApiException(ErrorCode.TICKET_SAVED_VIEW_NAME_INVALID);
        }
        return name;
    }

    private TicketInboxSavedViewFilterDto sanitizeFilter(TicketInboxSavedViewFilterDto filter) {
        if (filter == null) {
            TicketInboxSavedViewFilterDto empty = new TicketInboxSavedViewFilterDto();
            empty.setView(TicketInboxView.ALL);
            return empty;
        }
        if (filter.getView() == null) {
            filter.setView(TicketInboxView.ALL);
        }
        if (StringUtils.hasText(filter.getQ())) {
            filter.setQ(filter.getQ().trim());
        } else {
            filter.setQ(null);
        }
        if (StringUtils.hasText(filter.getCustomer())) {
            filter.setCustomer(filter.getCustomer().trim());
        } else {
            filter.setCustomer(null);
        }
        if (Boolean.TRUE.equals(filter.getUnassigned())) {
            filter.setAssigneeId(null);
            filter.setUnassigned(true);
        } else {
            filter.setUnassigned(null);
        }
        return filter;
    }

    private String serializeFilter(TicketInboxSavedViewFilterDto filter) {
        try {
            return objectMapper.writeValueAsString(filter);
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.TICKET_SAVED_VIEW_FILTER_INVALID);
        }
    }

    private TicketInboxSavedViewFilterDto deserializeFilter(String json) {
        if (!StringUtils.hasText(json)) {
            TicketInboxSavedViewFilterDto empty = new TicketInboxSavedViewFilterDto();
            empty.setView(TicketInboxView.ALL);
            return empty;
        }
        try {
            TicketInboxSavedViewFilterDto filter = objectMapper.readValue(json, TicketInboxSavedViewFilterDto.class);
            return sanitizeFilter(filter);
        } catch (JsonProcessingException ex) {
            TicketInboxSavedViewFilterDto empty = new TicketInboxSavedViewFilterDto();
            empty.setView(TicketInboxView.ALL);
            return empty;
        }
    }

    private TicketInboxSavedViewDto toDto(TicketInboxSavedView entity) {
        TicketInboxSavedViewDto dto = new TicketInboxSavedViewDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDefault(entity.isDefault());
        dto.setSortOrder(entity.getSortOrder());
        dto.setFilter(deserializeFilter(entity.getFilterJson()));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private void requireAgent(User user) {
        if (user == null || user.getId() == null || user.getRole() != User.Role.ADMIN) {
            throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
        }
    }
}
