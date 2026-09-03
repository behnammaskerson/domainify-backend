package com.domainify.service;

import com.domainify.dto.TicketReplyTemplateDto;
import com.domainify.dto.TicketReplyTemplateRequest;
import com.domainify.entity.TicketReplyTemplate;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketReplyTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TicketReplyTemplateService {

    private static final int TITLE_MAX = 120;
    private static final int BODY_MAX = 10000;
    private static final int SORT_ORDER_MAX = 9999;

    private final TicketReplyTemplateRepository ticketReplyTemplateRepository;

    public TicketReplyTemplateService(TicketReplyTemplateRepository ticketReplyTemplateRepository) {
        this.ticketReplyTemplateRepository = ticketReplyTemplateRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketReplyTemplateDto> listAll() {
        return ticketReplyTemplateRepository.findAllByOrderBySortOrderAscTitleAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketReplyTemplateDto> listActive() {
        return ticketReplyTemplateRepository.findByActiveTrueOrderBySortOrderAscTitleAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TicketReplyTemplateDto create(TicketReplyTemplateRequest request) {
        String title = requireValidTitle(request != null ? request.getTitle() : null);
        String body = requireValidBody(request != null ? request.getBody() : null);
        if (ticketReplyTemplateRepository.existsByTitleIgnoreCase(title)) {
            throw new ApiException(ErrorCode.TICKET_REPLY_TEMPLATE_EXISTS);
        }

        TicketReplyTemplate template = new TicketReplyTemplate();
        template.setTitle(title);
        template.setBody(body);
        template.setActive(request == null || request.getActive() == null || request.getActive());
        template.setSortOrder(clampSortOrder(request != null ? request.getSortOrder() : null));
        return toDto(ticketReplyTemplateRepository.save(template));
    }

    @Transactional
    public TicketReplyTemplateDto update(Long id, TicketReplyTemplateRequest request) {
        TicketReplyTemplate template = ticketReplyTemplateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_REPLY_TEMPLATE_NOT_FOUND));

        String title = requireValidTitle(request != null ? request.getTitle() : null);
        String body = requireValidBody(request != null ? request.getBody() : null);
        if (!title.equalsIgnoreCase(template.getTitle())
                && ticketReplyTemplateRepository.existsByTitleIgnoreCase(title)) {
            throw new ApiException(ErrorCode.TICKET_REPLY_TEMPLATE_EXISTS);
        }

        template.setTitle(title);
        template.setBody(body);
        if (request != null && request.getActive() != null) {
            template.setActive(request.getActive());
        }
        if (request != null && request.getSortOrder() != null) {
            template.setSortOrder(clampSortOrder(request.getSortOrder()));
        }
        return toDto(ticketReplyTemplateRepository.save(template));
    }

    @Transactional
    public void delete(Long id) {
        TicketReplyTemplate template = ticketReplyTemplateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_REPLY_TEMPLATE_NOT_FOUND));
        ticketReplyTemplateRepository.delete(template);
    }

    public TicketReplyTemplateDto toDto(TicketReplyTemplate template) {
        return new TicketReplyTemplateDto(
                template.getId(),
                template.getTitle(),
                template.getBody(),
                template.isActive(),
                template.getSortOrder());
    }

    private String requireValidTitle(String raw) {
        String title = normalizeTitle(raw);
        if (!StringUtils.hasText(title)) {
            throw new ApiException(ErrorCode.TICKET_REPLY_TEMPLATE_TITLE_REQUIRED);
        }
        if (title.length() > TITLE_MAX) {
            throw new ApiException(ErrorCode.TICKET_REPLY_TEMPLATE_TITLE_INVALID);
        }
        return title;
    }

    private String requireValidBody(String raw) {
        String body = raw == null ? "" : raw.trim();
        if (!StringUtils.hasText(body)) {
            throw new ApiException(ErrorCode.TICKET_REPLY_TEMPLATE_BODY_REQUIRED);
        }
        if (body.length() > BODY_MAX) {
            throw new ApiException(ErrorCode.TICKET_REPLY_TEMPLATE_BODY_INVALID);
        }
        return body;
    }

    private String normalizeTitle(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    private int clampSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return 0;
        }
        return Math.max(0, Math.min(SORT_ORDER_MAX, sortOrder));
    }
}
