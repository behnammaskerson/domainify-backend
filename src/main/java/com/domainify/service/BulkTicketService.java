package com.domainify.service;

import com.domainify.dto.BulkTicketActionRequest;
import com.domainify.dto.BulkTicketActionResultDto;
import com.domainify.dto.BulkTicketFailureDto;
import com.domainify.dto.UpdateTicketTagsRequest;
import com.domainify.entity.BulkTicketAction;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class BulkTicketService {

    private static final int MAX_BULK_IDS = 100;

    private final TicketService ticketService;
    private final MessageService messageService;
    private final TransactionTemplate transactionTemplate;

    public BulkTicketService(
            TicketService ticketService,
            MessageService messageService,
            PlatformTransactionManager transactionManager) {
        this.ticketService = ticketService;
        this.messageService = messageService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public BulkTicketActionResultDto execute(User agent, BulkTicketActionRequest request) {
        if (agent == null || agent.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (request == null || request.getAction() == null) {
            throw new ApiException(ErrorCode.TICKET_BULK_ACTION_INVALID);
        }

        List<Long> ids = distinctTicketIds(request.getTicketIds());
        if (ids.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_BULK_EMPTY);
        }
        if (ids.size() > MAX_BULK_IDS) {
            throw new ApiException(ErrorCode.TICKET_BULK_TOO_MANY);
        }

        BulkTicketAction action = request.getAction();
        validatePayload(action, request);

        List<Long> succeeded = new ArrayList<>();
        List<BulkTicketFailureDto> failed = new ArrayList<>();

        for (Long ticketId : ids) {
            try {
                transactionTemplate.executeWithoutResult(status -> applyOne(agent, action, ticketId, request));
                succeeded.add(ticketId);
            } catch (ApiException ex) {
                failed.add(new BulkTicketFailureDto(
                        ticketId,
                        ex.getCode().name(),
                        messageService.get(ex.getCode(), ex.getArgs())));
            } catch (RuntimeException ex) {
                ApiException api = findApiException(ex);
                if (api != null) {
                    failed.add(new BulkTicketFailureDto(
                            ticketId,
                            api.getCode().name(),
                            messageService.get(api.getCode(), api.getArgs())));
                } else {
                    failed.add(new BulkTicketFailureDto(
                            ticketId,
                            ErrorCode.UNEXPECTED_ERROR.name(),
                            messageService.get(ErrorCode.UNEXPECTED_ERROR)));
                }
            }
        }

        return new BulkTicketActionResultDto(succeeded, failed);
    }

    private void applyOne(User agent, BulkTicketAction action, Long ticketId, BulkTicketActionRequest request) {
        switch (action) {
            case ASSIGN -> ticketService.assignAsStaff(agent, ticketId, request.getAssigneeId());
            case CHANGE_STATUS -> ticketService.updateStatusAsStaff(agent, ticketId, request.getStatus());
            case ADD_TAG -> {
                UpdateTicketTagsRequest tagsRequest = new UpdateTicketTagsRequest();
                tagsRequest.setTagIds(request.getTagIds());
                tagsRequest.setNames(request.getNames());
                ticketService.addTagsAsStaff(agent, ticketId, tagsRequest);
            }
            case CLOSE -> ticketService.closeAsStaff(agent, ticketId);
            default -> throw new ApiException(ErrorCode.TICKET_BULK_ACTION_INVALID);
        }
    }

    private void validatePayload(BulkTicketAction action, BulkTicketActionRequest request) {
        switch (action) {
            case ASSIGN -> {
                // assigneeId may be null (unassign)
            }
            case CHANGE_STATUS -> {
                if (request.getStatus() == null) {
                    throw new ApiException(ErrorCode.TICKET_STATUS_INVALID);
                }
            }
            case ADD_TAG -> {
                boolean hasTagId = request.getTagIds() != null && request.getTagIds().stream().anyMatch(id -> id != null);
                boolean hasName = request.getNames() != null && request.getNames().stream().anyMatch(StringUtils::hasText);
                if (!hasTagId && !hasName) {
                    throw new ApiException(ErrorCode.TICKET_BULK_ACTION_INVALID);
                }
            }
            case CLOSE -> {
                // no extra payload
            }
            default -> throw new ApiException(ErrorCode.TICKET_BULK_ACTION_INVALID);
        }
    }

    private static List<Long> distinctTicketIds(List<Long> raw) {
        Set<Long> seen = new LinkedHashSet<>();
        if (raw == null) {
            return List.of();
        }
        for (Long id : raw) {
            if (id != null && id > 0) {
                seen.add(id);
            }
        }
        return new ArrayList<>(seen);
    }

    private static ApiException findApiException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ApiException api) {
                return api;
            }
            current = current.getCause();
        }
        return null;
    }
}
