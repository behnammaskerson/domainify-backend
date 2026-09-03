package com.domainify.service;

import com.domainify.dto.TicketStatusDefinitionDto;
import com.domainify.dto.TicketStatusWorkflowDto;
import com.domainify.entity.TicketStatus;
import com.domainify.entity.TicketStatusDefinition;
import com.domainify.entity.TicketStatusTransition;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketStatusDefinitionRepository;
import com.domainify.repository.TicketStatusTransitionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TicketStatusWorkflowService implements ApplicationRunner {

    private static final List<DefaultStatus> DEFAULT_STATUSES = List.of(
            new DefaultStatus(TicketStatus.NEW, 0),
            new DefaultStatus(TicketStatus.OPEN, 1),
            new DefaultStatus(TicketStatus.PENDING, 2),
            new DefaultStatus(TicketStatus.ON_HOLD, 3),
            new DefaultStatus(TicketStatus.RESOLVED, 4),
            new DefaultStatus(TicketStatus.CLOSED, 5)
    );

    /** Sensible helpdesk defaults; admins can customize via the workflow UI. */
    private static final List<TransitionPair> DEFAULT_TRANSITIONS = List.of(
            pair(TicketStatus.NEW, TicketStatus.OPEN),
            pair(TicketStatus.NEW, TicketStatus.PENDING),
            pair(TicketStatus.NEW, TicketStatus.ON_HOLD),
            pair(TicketStatus.NEW, TicketStatus.RESOLVED),
            pair(TicketStatus.NEW, TicketStatus.CLOSED),
            pair(TicketStatus.OPEN, TicketStatus.PENDING),
            pair(TicketStatus.OPEN, TicketStatus.ON_HOLD),
            pair(TicketStatus.OPEN, TicketStatus.RESOLVED),
            pair(TicketStatus.OPEN, TicketStatus.CLOSED),
            pair(TicketStatus.PENDING, TicketStatus.OPEN),
            pair(TicketStatus.PENDING, TicketStatus.ON_HOLD),
            pair(TicketStatus.PENDING, TicketStatus.RESOLVED),
            pair(TicketStatus.PENDING, TicketStatus.CLOSED),
            pair(TicketStatus.ON_HOLD, TicketStatus.OPEN),
            pair(TicketStatus.ON_HOLD, TicketStatus.PENDING),
            pair(TicketStatus.ON_HOLD, TicketStatus.RESOLVED),
            pair(TicketStatus.ON_HOLD, TicketStatus.CLOSED),
            pair(TicketStatus.RESOLVED, TicketStatus.OPEN),
            pair(TicketStatus.RESOLVED, TicketStatus.CLOSED),
            pair(TicketStatus.CLOSED, TicketStatus.OPEN)
    );

    private final TicketStatusDefinitionRepository definitionRepository;
    private final TicketStatusTransitionRepository transitionRepository;

    public TicketStatusWorkflowService(
            TicketStatusDefinitionRepository definitionRepository,
            TicketStatusTransitionRepository transitionRepository) {
        this.definitionRepository = definitionRepository;
        this.transitionRepository = transitionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDefinitions();
        if (transitionRepository.count() == 0) {
            for (TransitionPair pair : DEFAULT_TRANSITIONS) {
                TicketStatusTransition transition = new TicketStatusTransition();
                transition.setFromStatus(pair.from());
                transition.setToStatus(pair.to());
                transitionRepository.save(transition);
            }
        }
    }

    @Transactional(readOnly = true)
    public TicketStatusWorkflowDto getWorkflow() {
        List<TicketStatusDefinitionDto> statuses = definitionRepository.findAllByOrderBySortOrderAscStatusAsc()
                .stream()
                .map(this::toDefinitionDto)
                .toList();
        List<TicketStatusWorkflowDto.TicketStatusTransitionDto> transitions = transitionRepository
                .findAllByOrderByFromStatusAscToStatusAsc()
                .stream()
                .map(t -> new TicketStatusWorkflowDto.TicketStatusTransitionDto(t.getFromStatus(), t.getToStatus()))
                .toList();
        return new TicketStatusWorkflowDto(statuses, transitions);
    }

    @Transactional
    public TicketStatusWorkflowDto saveWorkflow(TicketStatusWorkflowDto request) {
        if (request == null) {
            throw new ApiException(ErrorCode.TICKET_STATUS_WORKFLOW_INVALID);
        }

        Map<TicketStatus, TicketStatusDefinitionDto> incoming = new EnumMap<>(TicketStatus.class);
        if (request.getStatuses() != null) {
            for (TicketStatusDefinitionDto dto : request.getStatuses()) {
                if (dto == null || dto.getStatus() == null) {
                    continue;
                }
                incoming.put(dto.getStatus(), dto);
            }
        }
        for (TicketStatus status : TicketStatus.values()) {
            if (!incoming.containsKey(status)) {
                throw new ApiException(ErrorCode.TICKET_STATUS_WORKFLOW_INVALID);
            }
        }

        TicketStatusDefinitionDto newDto = incoming.get(TicketStatus.NEW);
        if (newDto == null || !newDto.isActive()) {
            throw new ApiException(ErrorCode.TICKET_STATUS_NEW_REQUIRED);
        }

        for (TicketStatus status : TicketStatus.values()) {
            TicketStatusDefinition definition = definitionRepository.findById(status)
                    .orElseGet(() -> {
                        TicketStatusDefinition created = new TicketStatusDefinition();
                        created.setStatus(status);
                        return created;
                    });
            TicketStatusDefinitionDto dto = incoming.get(status);
            definition.setActive(dto.isActive());
            definition.setSortOrder(dto.getSortOrder());
            definition.setLabel(normalizeLabel(dto.getLabel()));
            definitionRepository.save(definition);
        }

        Set<String> enabledPairs = new HashSet<>();
        List<TicketStatusTransition> nextTransitions = new ArrayList<>();
        if (request.getTransitions() != null) {
            for (TicketStatusWorkflowDto.TicketStatusTransitionDto edge : request.getTransitions()) {
                if (edge == null || edge.getFrom() == null || edge.getTo() == null) {
                    continue;
                }
                if (edge.getFrom() == edge.getTo()) {
                    continue;
                }
                TicketStatusDefinition fromDef = definitionRepository.findById(edge.getFrom())
                        .orElseThrow(() -> new ApiException(ErrorCode.TICKET_STATUS_INVALID));
                TicketStatusDefinition toDef = definitionRepository.findById(edge.getTo())
                        .orElseThrow(() -> new ApiException(ErrorCode.TICKET_STATUS_INVALID));
                if (!fromDef.isActive() || !toDef.isActive()) {
                    continue;
                }
                String key = edge.getFrom().name() + ">" + edge.getTo().name();
                if (!enabledPairs.add(key)) {
                    continue;
                }
                TicketStatusTransition transition = new TicketStatusTransition();
                transition.setFromStatus(edge.getFrom());
                transition.setToStatus(edge.getTo());
                nextTransitions.add(transition);
            }
        }

        transitionRepository.deleteAllInBatch();
        transitionRepository.flush();
        transitionRepository.saveAll(nextTransitions);

        return getWorkflow();
    }

    @Transactional(readOnly = true)
    public List<TicketStatus> allowedNextStatuses(TicketStatus from) {
        if (from == null) {
            return List.of();
        }
        Set<TicketStatus> active = activeStatuses();
        if (!active.contains(from) && from != TicketStatus.NEW) {
            // Ticket may sit on a deactivated status; still allow configured exits to active targets.
        }
        return transitionRepository.findByFromStatusOrderByToStatusAsc(from).stream()
                .map(TicketStatusTransition::getToStatus)
                .filter(active::contains)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isTransitionAllowed(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        if (!activeStatuses().contains(to)) {
            return false;
        }
        return transitionRepository.existsByFromStatusAndToStatus(from, to);
    }

    @Transactional(readOnly = true)
    public void assertTransitionAllowed(TicketStatus from, TicketStatus to) {
        if (from == to) {
            return;
        }
        if (!isTransitionAllowed(from, to)) {
            throw new ApiException(ErrorCode.TICKET_STATUS_TRANSITION_INVALID);
        }
    }

    private void seedDefinitions() {
        for (DefaultStatus seed : DEFAULT_STATUSES) {
            if (!definitionRepository.existsById(seed.status())) {
                TicketStatusDefinition definition = new TicketStatusDefinition();
                definition.setStatus(seed.status());
                definition.setActive(true);
                definition.setSortOrder(seed.sortOrder());
                definitionRepository.save(definition);
            }
        }
    }

    private Set<TicketStatus> activeStatuses() {
        Set<TicketStatus> active = EnumSet.noneOf(TicketStatus.class);
        for (TicketStatusDefinition definition : definitionRepository.findByActiveTrueOrderBySortOrderAscStatusAsc()) {
            active.add(definition.getStatus());
        }
        if (active.isEmpty()) {
            active.add(TicketStatus.NEW);
        }
        return active;
    }

    private TicketStatusDefinitionDto toDefinitionDto(TicketStatusDefinition definition) {
        return new TicketStatusDefinitionDto(
                definition.getStatus(),
                definition.getLabel(),
                definition.isActive(),
                definition.getSortOrder()
        );
    }

    private String normalizeLabel(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    private static TransitionPair pair(TicketStatus from, TicketStatus to) {
        return new TransitionPair(from, to);
    }

    private record DefaultStatus(TicketStatus status, int sortOrder) {
    }

    private record TransitionPair(TicketStatus from, TicketStatus to) {
    }
}
