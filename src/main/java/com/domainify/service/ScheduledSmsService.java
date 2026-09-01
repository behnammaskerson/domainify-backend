package com.domainify.service;

import com.domainify.dto.*;
import com.domainify.entity.ScheduledSms;
import com.domainify.entity.ScheduledSmsSourceType;
import com.domainify.entity.ScheduledSmsStatus;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.ScheduledSmsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ScheduledSmsService {

    private static final int PROVIDER_SUCCESS = 1;
    private static final long CANCEL_DEADLINE_MINUTES = 3;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "scheduledAt", "createdAt", "recipientCount", "cost", "packId", "lineNumber", "sourceType", "status"
    );

    private final ScheduledSmsRepository scheduledSmsRepository;
    private final SmsConfigService smsConfigService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ScheduledSmsService(
            ScheduledSmsRepository scheduledSmsRepository,
            SmsConfigService smsConfigService,
            ObjectMapper objectMapper) {
        this.scheduledSmsRepository = scheduledSmsRepository;
        this.smsConfigService = smsConfigService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Transactional
    public void recordScheduledSend(
            SmsBulkSendRequest request,
            SmsBulkSendDataDto data,
            Long sendDateTimeEpochSeconds) {
        if (sendDateTimeEpochSeconds == null || data == null || !StringUtils.hasText(data.getPackId())) {
            return;
        }

        ScheduledSmsSourceType sourceType = resolveSourceType(request.getSendSource());
        ScheduledSms scheduled = new ScheduledSms();
        scheduled.setPackId(data.getPackId().trim());
        scheduled.setSourceType(sourceType);
        scheduled.setLineNumber(resolveLineNumber(request.getLineNumber()));
        scheduled.setMessageText(request.getMessageText().trim());
        scheduled.setRecipientCount(request.getMobiles() != null ? request.getMobiles().size() : 0);
        scheduled.setCost(data.getCost());
        scheduled.setScheduledAt(Instant.ofEpochSecond(sendDateTimeEpochSeconds));
        scheduled.setStatus(ScheduledSmsStatus.PENDING);
        scheduled.setCreatedAt(Instant.now());
        scheduledSmsRepository.save(scheduled);
    }

    @Transactional
    public SmsScheduledPagedResponse listScheduled(
            ScheduledSmsStatus status,
            ScheduledSmsSourceType sourceType,
            String search,
            Instant scheduledFrom,
            Instant scheduledTo,
            Pageable pageable) {
        markPastDueAsSent();

        Specification<ScheduledSms> baseSpec = buildListSpec(sourceType, search, scheduledFrom, scheduledTo);
        Specification<ScheduledSms> listSpec = baseSpec;
        if (status != null) {
            listSpec = listSpec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Pageable safePageable = sanitizePageable(pageable);
        Instant cancelDeadline = Instant.now().plus(CANCEL_DEADLINE_MINUTES, ChronoUnit.MINUTES);
        Page<SmsScheduledItemDto> page = scheduledSmsRepository.findAll(listSpec, safePageable)
                .map(item -> toDto(item, cancelDeadline));

        long allCount = scheduledSmsRepository.count(baseSpec);
        long pendingCount = scheduledSmsRepository.count(
                baseSpec.and((root, query, cb) -> cb.equal(root.get("status"), ScheduledSmsStatus.PENDING)));
        long cancelledCount = scheduledSmsRepository.count(
                baseSpec.and((root, query, cb) -> cb.equal(root.get("status"), ScheduledSmsStatus.CANCELLED)));
        long sentCount = scheduledSmsRepository.count(
                baseSpec.and((root, query, cb) -> cb.equal(root.get("status"), ScheduledSmsStatus.SENT)));

        return SmsScheduledPagedResponse.from(page, allCount, pendingCount, cancelledCount, sentCount);
    }

    private Specification<ScheduledSms> buildListSpec(
            ScheduledSmsSourceType sourceType,
            String search,
            Instant scheduledFrom,
            Instant scheduledTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (sourceType != null) {
                predicates.add(cb.equal(root.get("sourceType"), sourceType));
            }
            if (StringUtils.hasText(search)) {
                String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("packId")), term),
                        cb.like(cb.lower(root.get("messageText")), term),
                        cb.like(cb.lower(root.get("lineNumber")), term)
                ));
            }
            if (scheduledFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), scheduledFrom));
            }
            if (scheduledTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledAt"), scheduledTo));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? 10 : Math.min(pageable.getPageSize(), 100);

        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                continue;
            }
            Sort.Order next = new Sort.Order(order.getDirection(), order.getProperty());
            if ("packId".equals(order.getProperty()) || "lineNumber".equals(order.getProperty())) {
                next = next.ignoreCase();
            }
            orders.add(next);
        }
        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("scheduledAt"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    @Transactional
    public SmsScheduledCancelResultDto cancelScheduled(String packId) {
        if (!StringUtils.hasText(packId)) {
            throw new ApiException(ErrorCode.SMS_SCHEDULED_NOT_FOUND);
        }

        ScheduledSms scheduled = scheduledSmsRepository.findByPackId(packId.trim())
                .orElseThrow(() -> new ApiException(ErrorCode.SMS_SCHEDULED_NOT_FOUND));

        if (scheduled.getStatus() == ScheduledSmsStatus.CANCELLED) {
            throw new ApiException(ErrorCode.SMS_SCHEDULED_ALREADY_CANCELLED);
        }
        if (scheduled.getStatus() == ScheduledSmsStatus.SENT) {
            throw new ApiException(ErrorCode.SMS_SCHEDULED_ALREADY_SENT);
        }

        Instant cancelDeadline = scheduled.getScheduledAt().minus(CANCEL_DEADLINE_MINUTES, ChronoUnit.MINUTES);
        if (Instant.now().isAfter(cancelDeadline)) {
            throw new ApiException(ErrorCode.SMS_SCHEDULED_CANCEL_TOO_LATE);
        }

        SmsScheduledCancelResultDto providerResult = callProviderCancel(scheduled.getPackId());
        if (!providerResult.isSuccess()) {
            throw new ApiException(ErrorCode.SMS_SCHEDULED_CANCEL_FAILED);
        }

        scheduled.setStatus(ScheduledSmsStatus.CANCELLED);
        scheduled.setCancelledAt(Instant.now());
        scheduled.setReturnedCreditCount(providerResult.getReturnedCreditCount());
        scheduled.setSmsCount(providerResult.getSmsCount());
        scheduledSmsRepository.save(scheduled);
        return providerResult;
    }

    @Transactional
    public void removeScheduledRecord(String packId) {
        if (!StringUtils.hasText(packId)) {
            throw new ApiException(ErrorCode.SMS_SCHEDULED_NOT_FOUND);
        }

        ScheduledSms scheduled = scheduledSmsRepository.findByPackId(packId.trim())
                .orElseThrow(() -> new ApiException(ErrorCode.SMS_SCHEDULED_NOT_FOUND));

        if (scheduled.getStatus() == ScheduledSmsStatus.PENDING) {
            Instant cancelDeadline = scheduled.getScheduledAt().minus(CANCEL_DEADLINE_MINUTES, ChronoUnit.MINUTES);
            if (Instant.now().isBefore(cancelDeadline)) {
                SmsScheduledCancelResultDto providerResult = callProviderCancel(scheduled.getPackId());
                if (!providerResult.isSuccess()) {
                    throw new ApiException(ErrorCode.SMS_SCHEDULED_CANCEL_FAILED);
                }
            }
        }

        scheduledSmsRepository.delete(scheduled);
    }

    private void markPastDueAsSent() {
        List<ScheduledSms> overdue = scheduledSmsRepository.findByStatusAndScheduledAtBefore(
                ScheduledSmsStatus.PENDING, Instant.now());
        for (ScheduledSms item : overdue) {
            item.setStatus(ScheduledSmsStatus.SENT);
        }
        if (!overdue.isEmpty()) {
            scheduledSmsRepository.saveAll(overdue);
        }
    }

    private SmsScheduledCancelResultDto callProviderCancel(String packId) {
        String apiKey = smsConfigService.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(ErrorCode.SMS_API_KEY_REQUIRED);
        }

        String url = smsConfigService.getServerUrl() + "v1/send/scheduled/" + packId;

        try {
            return restClient.delete()
                    .uri(url)
                    .header("X-API-KEY", apiKey)
                    .exchange((request, response) -> mapCancelResponse(response));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return SmsScheduledCancelResultDto.failure(null, null);
        }
    }

    private SmsScheduledCancelResultDto mapCancelResponse(ClientHttpResponse response) throws IOException {
        int httpStatus = response.getStatusCode().value();
        String rawBody = readBody(response);
        SmsIrScheduledCancelEnvelope envelope = parseEnvelope(rawBody, SmsIrScheduledCancelEnvelope.class);

        if (httpStatus == 200 && envelope != null && envelope.getStatus() != null
                && envelope.getStatus() == PROVIDER_SUCCESS) {
            SmsScheduledCancelDataDto data = envelope.getData() != null
                    ? envelope.getData()
                    : new SmsScheduledCancelDataDto();
            return SmsScheduledCancelResultDto.success(
                    data.getReturnedCreditCount() != null ? data.getReturnedCreditCount() : BigDecimal.ZERO,
                    data.getSmsCount() != null ? data.getSmsCount() : 0);
        }

        Integer providerStatus = envelope != null ? envelope.getStatus() : null;
        return SmsScheduledCancelResultDto.failure(httpStatus, providerStatus);
    }

    private SmsScheduledItemDto toDto(ScheduledSms item, Instant cancelDeadline) {
        SmsScheduledItemDto dto = new SmsScheduledItemDto();
        dto.setPackId(item.getPackId());
        dto.setSourceType(item.getSourceType());
        dto.setLineNumber(item.getLineNumber());
        dto.setMessageText(item.getMessageText());
        dto.setRecipientCount(item.getRecipientCount());
        dto.setCost(item.getCost());
        dto.setScheduledAt(item.getScheduledAt());
        dto.setStatus(item.getStatus());
        dto.setCancelledAt(item.getCancelledAt());
        dto.setReturnedCreditCount(item.getReturnedCreditCount());
        dto.setSmsCount(item.getSmsCount());
        dto.setCreatedAt(item.getCreatedAt());
        boolean cancellable = item.getStatus() == ScheduledSmsStatus.PENDING
                && item.getScheduledAt().isAfter(cancelDeadline);
        dto.setCancellable(cancellable);
        return dto;
    }

    private ScheduledSmsSourceType resolveSourceType(String sendSource) {
        if (!StringUtils.hasText(sendSource)) {
            return ScheduledSmsSourceType.BULK;
        }
        try {
            return ScheduledSmsSourceType.valueOf(sendSource.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ScheduledSmsSourceType.BULK;
        }
    }

    private String resolveLineNumber(String requestedLine) {
        if (StringUtils.hasText(requestedLine)) {
            return requestedLine.trim();
        }
        String defaultLine = smsConfigService.getDefaultLine();
        return StringUtils.hasText(defaultLine) ? defaultLine.trim() : "";
    }

    private String readBody(ClientHttpResponse response) throws IOException {
        return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private <T> T parseEnvelope(String rawBody, Class<T> type) {
        if (!StringUtils.hasText(rawBody)) {
            return null;
        }
        try {
            return objectMapper.readValue(rawBody, type);
        } catch (Exception ex) {
            return null;
        }
    }
}
