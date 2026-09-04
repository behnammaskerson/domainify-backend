package com.domainify.service;

import com.domainify.dto.TicketQueueDto;
import com.domainify.dto.TicketQueueRequest;
import com.domainify.dto.UpdateCategoryAgentsRequest;
import com.domainify.entity.TicketAgentQueueMembership;
import com.domainify.entity.TicketQueue;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketAgentQueueMembershipRepository;
import com.domainify.repository.TicketQueueRepository;
import com.domainify.repository.TicketRepository;
import com.domainify.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TicketQueueService {

    private final TicketQueueRepository ticketQueueRepository;
    private final TicketRepository ticketRepository;
    private final TicketAgentQueueMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public TicketQueueService(
            TicketQueueRepository ticketQueueRepository,
            TicketRepository ticketRepository,
            TicketAgentQueueMembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.ticketQueueRepository = ticketQueueRepository;
        this.ticketRepository = ticketRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketQueueDto> listActive() {
        return ticketQueueRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketQueueDto> listAll() {
        return ticketQueueRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketQueue requireActiveQueue(Long queueId) {
        if (queueId == null) {
            throw new ApiException(ErrorCode.TICKET_QUEUE_REQUIRED);
        }
        TicketQueue queue = ticketQueueRepository.findById(queueId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_QUEUE_NOT_FOUND));
        if (!queue.isActive()) {
            throw new ApiException(ErrorCode.TICKET_QUEUE_INACTIVE);
        }
        return queue;
    }

    @Transactional(readOnly = true)
    public TicketQueue findActiveOrNull(Long queueId) {
        if (queueId == null) {
            return null;
        }
        return requireActiveQueue(queueId);
    }

    @Transactional
    public TicketQueueDto create(TicketQueueRequest request) {
        String name = normalizeName(request.getName());
        boolean codeProvided = StringUtils.hasText(request.getCode());
        String code = normalizeCode(codeProvided ? request.getCode() : name);
        if (!StringUtils.hasText(code)) {
            if (codeProvided) {
                throw new ApiException(ErrorCode.TICKET_QUEUE_CODE_INVALID);
            }
            code = uniqueGeneratedCode();
        } else if (ticketQueueRepository.existsByCodeIgnoreCase(code)) {
            if (codeProvided) {
                throw new ApiException(ErrorCode.TICKET_QUEUE_CODE_EXISTS);
            }
            code = uniqueCodeWithSuffix(code);
        }

        TicketQueue queue = new TicketQueue();
        queue.setName(name);
        queue.setCode(code);
        queue.setActive(request.getActive() == null || request.getActive());
        queue.setSortOrder(request.getSortOrder() == null ? nextSortOrder() : request.getSortOrder());
        return toDto(ticketQueueRepository.save(queue));
    }

    @Transactional
    public TicketQueueDto update(Long id, TicketQueueRequest request) {
        TicketQueue queue = ticketQueueRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_QUEUE_NOT_FOUND));

        if (StringUtils.hasText(request.getName())) {
            queue.setName(normalizeName(request.getName()));
        }
        if (StringUtils.hasText(request.getCode())) {
            String code = normalizeCode(request.getCode());
            if (!StringUtils.hasText(code)) {
                throw new ApiException(ErrorCode.TICKET_QUEUE_CODE_INVALID);
            }
            if (ticketQueueRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new ApiException(ErrorCode.TICKET_QUEUE_CODE_EXISTS);
            }
            queue.setCode(code);
        }
        if (request.getActive() != null) {
            queue.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            queue.setSortOrder(request.getSortOrder());
        }
        return toDto(ticketQueueRepository.save(queue));
    }

    @Transactional
    public void delete(Long id) {
        TicketQueue queue = ticketQueueRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_QUEUE_NOT_FOUND));

        long usage = ticketRepository.countByQueue(queue);
        if (usage > 0) {
            queue.setActive(false);
            ticketQueueRepository.save(queue);
            return;
        }
        membershipRepository.deleteByQueueId(queue.getId());
        ticketQueueRepository.delete(queue);
    }

    @Transactional
    public TicketQueueDto updateQueueAgents(Long queueId, UpdateCategoryAgentsRequest request) {
        TicketQueue queue = ticketQueueRepository.findById(queueId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_QUEUE_NOT_FOUND));

        Set<Long> requestedIds = new LinkedHashSet<>();
        if (request != null && request.getAgentIds() != null) {
            for (Long agentId : request.getAgentIds()) {
                if (agentId != null) {
                    requestedIds.add(agentId);
                }
            }
        }

        List<User> agents = requestedIds.isEmpty()
                ? List.of()
                : userRepository.findAllById(requestedIds).stream()
                .filter(user -> user.getRole() == User.Role.ADMIN && user.isEnabled())
                .toList();

        if (agents.size() != requestedIds.size()) {
            throw new ApiException(ErrorCode.TICKET_ASSIGNEE_INVALID);
        }

        membershipRepository.deleteByQueueId(queue.getId());
        for (User agent : agents) {
            TicketAgentQueueMembership membership = new TicketAgentQueueMembership();
            membership.setQueue(queue);
            membership.setUser(agent);
            membershipRepository.save(membership);
        }
        return toDto(queue);
    }

    private int nextSortOrder() {
        return ticketQueueRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .mapToInt(TicketQueue::getSortOrder)
                .max()
                .orElse(0) + 10;
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ApiException(ErrorCode.TICKET_QUEUE_NAME_REQUIRED);
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new ApiException(ErrorCode.TICKET_QUEUE_NAME_INVALID);
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

    private String uniqueGeneratedCode() {
        for (int i = 0; i < 20; i++) {
            String candidate = "QUEUE_" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT)
                    + (i == 0 ? "" : ("_" + i));
            if (candidate.length() > 64) {
                candidate = candidate.substring(0, 64);
            }
            if (!ticketQueueRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(ErrorCode.TICKET_QUEUE_CODE_INVALID);
    }

    private String uniqueCodeWithSuffix(String base) {
        for (int i = 2; i < 1000; i++) {
            String suffix = "_" + i;
            String candidate = base.length() + suffix.length() > 64
                    ? base.substring(0, 64 - suffix.length()) + suffix
                    : base + suffix;
            if (!ticketQueueRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return uniqueGeneratedCode();
    }

    public TicketQueueDto toDto(TicketQueue queue) {
        TicketQueueDto dto = new TicketQueueDto(
                queue.getId(),
                queue.getCode(),
                queue.getName(),
                queue.isActive(),
                queue.getSortOrder()
        );
        if (queue.getId() != null) {
            dto.setAgentIds(new ArrayList<>(membershipRepository.findUserIdsByQueueId(queue.getId())));
        }
        return dto;
    }

    /** Lightweight DTO without agent IDs for ticket list payloads. */
    public TicketQueueDto toSummaryDto(TicketQueue queue) {
        if (queue == null) {
            return null;
        }
        return new TicketQueueDto(
                queue.getId(),
                queue.getCode(),
                queue.getName(),
                queue.isActive(),
                queue.getSortOrder()
        );
    }
}
