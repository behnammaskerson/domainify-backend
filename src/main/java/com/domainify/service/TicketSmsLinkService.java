package com.domainify.service;

import com.domainify.dto.LinkTicketSmsRequest;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.RelatedSmsDto;
import com.domainify.dto.SmsDailyPackItemDto;
import com.domainify.dto.SmsDailyPackResultDto;
import com.domainify.dto.SmsDeliveryStatusDataDto;
import com.domainify.dto.SmsLiveSendResultDto;
import com.domainify.dto.SmsPackReportResultDto;
import com.domainify.dto.SmsScheduledItemDto;
import com.domainify.dto.SmsScheduledPagedResponse;
import com.domainify.dto.TicketDetailDto;
import com.domainify.dto.TicketSmsLinkableItemDto;
import com.domainify.entity.ScheduledSms;
import com.domainify.entity.Ticket;
import com.domainify.entity.TicketSmsLink;
import com.domainify.entity.TicketSmsLinkType;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.ScheduledSmsRepository;
import com.domainify.repository.TicketRepository;
import com.domainify.repository.TicketSmsLinkRepository;
import com.domainify.util.PhoneSmsUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class TicketSmsLinkService {

    private final TicketSmsLinkRepository ticketSmsLinkRepository;
    private final TicketRepository ticketRepository;
    private final ScheduledSmsRepository scheduledSmsRepository;
    private final ScheduledSmsService scheduledSmsService;
    private final SmsService smsService;
    private final TicketService ticketService;

    public TicketSmsLinkService(
            TicketSmsLinkRepository ticketSmsLinkRepository,
            TicketRepository ticketRepository,
            ScheduledSmsRepository scheduledSmsRepository,
            ScheduledSmsService scheduledSmsService,
            SmsService smsService,
            @Lazy TicketService ticketService) {
        this.ticketSmsLinkRepository = ticketSmsLinkRepository;
        this.ticketRepository = ticketRepository;
        this.scheduledSmsRepository = scheduledSmsRepository;
        this.scheduledSmsService = scheduledSmsService;
        this.smsService = smsService;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    public List<RelatedSmsDto> listRelated(Long ticketId) {
        if (ticketId == null) {
            return List.of();
        }
        return ticketSmsLinkRepository.findByTicketIdOrderByCreatedAtDescIdDesc(ticketId).stream()
                .map(this::toRelatedDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<TicketSmsLinkableItemDto> listLinkable(
            User agent,
            Long ticketId,
            TicketSmsLinkType type,
            String q,
            Pageable pageable) {
        requireAgent(agent);
        Ticket ticket = requireStaffTicket(ticketId);
        if (type == null) {
            throw new ApiException(ErrorCode.TICKET_SMS_TYPE_INVALID);
        }
        if (ticket.getRequester() == null || ticket.getRequester().getId() == null) {
            throw new ApiException(ErrorCode.TICKET_SMS_NO_REQUESTER);
        }

        int page = pageable != null ? Math.max(0, pageable.getPageNumber()) : 0;
        int size = pageable != null ? Math.min(50, Math.max(1, pageable.getPageSize())) : 20;
        Set<String> linked = new HashSet<>(
                ticketSmsLinkRepository.findExternalIdsByTicketIdAndLinkType(ticketId, type));

        return switch (type) {
            case SCHEDULED -> listLinkableScheduled(q, linked, page, size);
            case SEND -> listLinkableSends(ticket, q, linked, page, size);
            case PACK -> listLinkablePacks(ticket, q, linked, page, size);
        };
    }

    @Transactional
    public TicketDetailDto link(User agent, Long ticketId, LinkTicketSmsRequest request) {
        requireAgent(agent);
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_SMS_TARGET_REQUIRED);
        }
        Ticket ticket = requireStaffTicket(ticketId);
        if (ticket.isDeleted()) {
            throw new ApiException(ErrorCode.TICKET_DELETED);
        }
        if (ticket.isMerged()) {
            throw new ApiException(ErrorCode.TICKET_LINK_INVALID);
        }
        if (ticket.getRequester() == null || ticket.getRequester().getId() == null) {
            throw new ApiException(ErrorCode.TICKET_SMS_NO_REQUESTER);
        }

        String customerMobile = customerMobile(ticket.getRequester());
        List<LinkTicketSmsRequest.Item> items = request.getItems().stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getType() != null && StringUtils.hasText(i.getExternalId()))
                .toList();
        if (items.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_SMS_TARGET_REQUIRED);
        }

        for (LinkTicketSmsRequest.Item item : items) {
            TicketSmsLinkType type = item.getType();
            String externalId = item.getExternalId().trim();
            if (ticketSmsLinkRepository.existsByTicketIdAndLinkTypeAndExternalId(ticketId, type, externalId)) {
                throw new ApiException(ErrorCode.TICKET_SMS_ALREADY_LINKED);
            }
            TicketSmsLink link = resolveAndBuildLink(ticket, type, externalId, customerMobile);
            ticketSmsLinkRepository.save(link);
        }
        return ticketService.getForStaff(agent, ticketId);
    }

    @Transactional
    public TicketDetailDto unlink(User agent, Long ticketId, Long linkId) {
        requireAgent(agent);
        requireStaffTicket(ticketId);
        if (linkId == null) {
            throw new ApiException(ErrorCode.TICKET_SMS_NOT_LINKED);
        }
        int deleted = ticketSmsLinkRepository.deleteByTicketIdAndId(ticketId, linkId);
        if (deleted == 0) {
            throw new ApiException(ErrorCode.TICKET_SMS_NOT_LINKED);
        }
        return ticketService.getForStaff(agent, ticketId);
    }

    private TicketSmsLink resolveAndBuildLink(
            Ticket ticket,
            TicketSmsLinkType type,
            String externalId,
            String customerMobile) {
        return switch (type) {
            case SCHEDULED -> linkScheduled(ticket, externalId);
            case SEND -> linkSend(ticket, externalId, customerMobile);
            case PACK -> linkPack(ticket, externalId, customerMobile);
        };
    }

    private TicketSmsLink linkScheduled(Ticket ticket, String packId) {
        ScheduledSms scheduled = scheduledSmsRepository.findByPackId(packId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_SMS_NOT_RELATED));
        TicketSmsLink link = baseLink(ticket, TicketSmsLinkType.SCHEDULED, packId);
        link.setMessagePreview(trimPreview(scheduled.getMessageText()));
        link.setLineNumber(scheduled.getLineNumber());
        link.setStatusLabel(scheduled.getStatus() != null ? scheduled.getStatus().name() : null);
        link.setRecipientCount(scheduled.getRecipientCount());
        link.setOccurredAt(scheduled.getScheduledAt());
        return link;
    }

    private TicketSmsLink linkSend(Ticket ticket, String messageId, String customerMobile) {
        if (!StringUtils.hasText(customerMobile)) {
            throw new ApiException(ErrorCode.TICKET_SMS_NOT_RELATED);
        }
        SmsDeliveryStatusDataDto match = findSendByMessageId(messageId);
        if (match == null || !mobileMatches(customerMobile, match.getMobile())) {
            throw new ApiException(ErrorCode.TICKET_SMS_NOT_RELATED);
        }
        TicketSmsLink link = baseLink(ticket, TicketSmsLinkType.SEND, messageId);
        link.setMobile(String.valueOf(match.getMobile()));
        link.setMessagePreview(trimPreview(match.getMessageText()));
        link.setLineNumber(match.getLineNumber() != null ? String.valueOf(match.getLineNumber()) : null);
        link.setStatusLabel(match.getDeliveryState() != null ? String.valueOf(match.getDeliveryState()) : null);
        link.setOccurredAt(epochSecondsToInstant(match.getSendDateTime()));
        link.setRecipientCount(1);
        return link;
    }

    private TicketSmsLink linkPack(Ticket ticket, String packId, String customerMobile) {
        if (!StringUtils.hasText(customerMobile)) {
            throw new ApiException(ErrorCode.TICKET_SMS_NOT_RELATED);
        }
        SmsPackReportResultDto report;
        try {
            report = smsService.fetchPackReport(packId);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.TICKET_SMS_PROVIDER_ERROR);
        }
        if (report == null || !report.isSuccess() || report.getData() == null) {
            throw new ApiException(ErrorCode.TICKET_SMS_PROVIDER_ERROR);
        }
        SmsDeliveryStatusDataDto related = report.getData().stream()
                .filter(row -> row != null && mobileMatches(customerMobile, row.getMobile()))
                .findFirst()
                .orElse(null);
        if (related == null) {
            throw new ApiException(ErrorCode.TICKET_SMS_NOT_RELATED);
        }
        TicketSmsLink link = baseLink(ticket, TicketSmsLinkType.PACK, packId);
        link.setMobile(customerMobile);
        link.setMessagePreview(trimPreview(related.getMessageText()));
        link.setLineNumber(related.getLineNumber() != null ? String.valueOf(related.getLineNumber()) : null);
        link.setStatusLabel("PACK");
        link.setRecipientCount(report.getData().size());
        link.setOccurredAt(epochSecondsToInstant(related.getSendDateTime()));
        return link;
    }

    private PagedResponse<TicketSmsLinkableItemDto> listLinkableScheduled(
            String q, Set<String> linked, int page, int size) {
        SmsScheduledPagedResponse result = scheduledSmsService.listScheduled(
                null, null, q, null, null, PageRequest.of(page, size));
        List<TicketSmsLinkableItemDto> items = new ArrayList<>();
        for (SmsScheduledItemDto row : result.getContent() != null ? result.getContent() : List.<SmsScheduledItemDto>of()) {
            if (row == null || !StringUtils.hasText(row.getPackId()) || linked.contains(row.getPackId())) {
                continue;
            }
            TicketSmsLinkableItemDto item = new TicketSmsLinkableItemDto();
            item.setType(TicketSmsLinkType.SCHEDULED);
            item.setExternalId(row.getPackId());
            item.setMessagePreview(trimPreview(row.getMessageText()));
            item.setLineNumber(row.getLineNumber());
            item.setStatusLabel(row.getStatus() != null ? row.getStatus().name() : null);
            item.setRecipientCount(row.getRecipientCount());
            item.setOccurredAt(row.getScheduledAt());
            items.add(item);
        }
        return new PagedResponse<>(
                items,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize());
    }

    private PagedResponse<TicketSmsLinkableItemDto> listLinkableSends(
            Ticket ticket, String q, Set<String> linked, int page, int size) {
        String mobile = customerMobile(ticket.getRequester());
        if (!StringUtils.hasText(mobile)) {
            return emptyPage(page, size);
        }
        SmsLiveSendResultDto result;
        try {
            // Provider pages are 1-based.
            result = smsService.fetchLiveSends(Math.max(size, 20), page + 1);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.TICKET_SMS_PROVIDER_ERROR);
        }
        if (result == null || !result.isSuccess()) {
            throw new ApiException(ErrorCode.TICKET_SMS_PROVIDER_ERROR);
        }
        String term = StringUtils.hasText(q) ? q.trim().toLowerCase(Locale.ROOT) : null;
        List<TicketSmsLinkableItemDto> items = new ArrayList<>();
        for (SmsDeliveryStatusDataDto row : result.getData() != null ? result.getData() : List.<SmsDeliveryStatusDataDto>of()) {
            if (row == null || row.getMessageId() == null || !mobileMatches(mobile, row.getMobile())) {
                continue;
            }
            String externalId = String.valueOf(row.getMessageId());
            if (linked.contains(externalId)) {
                continue;
            }
            if (term != null) {
                String hay = ((row.getMessageText() != null ? row.getMessageText() : "")
                        + " " + externalId).toLowerCase(Locale.ROOT);
                if (!hay.contains(term)) {
                    continue;
                }
            }
            TicketSmsLinkableItemDto item = new TicketSmsLinkableItemDto();
            item.setType(TicketSmsLinkType.SEND);
            item.setExternalId(externalId);
            item.setMobile(String.valueOf(row.getMobile()));
            item.setMessagePreview(trimPreview(row.getMessageText()));
            item.setLineNumber(row.getLineNumber() != null ? String.valueOf(row.getLineNumber()) : null);
            item.setStatusLabel(row.getDeliveryState() != null ? String.valueOf(row.getDeliveryState()) : null);
            item.setOccurredAt(epochSecondsToInstant(row.getSendDateTime()));
            item.setRecipientCount(1);
            items.add(item);
        }
        return new PagedResponse<>(
                items,
                items.size(),
                result.isHasMore() ? page + 2 : page + 1,
                page,
                size);
    }

    private PagedResponse<TicketSmsLinkableItemDto> listLinkablePacks(
            Ticket ticket, String q, Set<String> linked, int page, int size) {
        String mobile = customerMobile(ticket.getRequester());
        if (!StringUtils.hasText(mobile)) {
            return emptyPage(page, size);
        }
        SmsDailyPackResultDto result;
        try {
            result = smsService.fetchDailyPacks(Math.max(size, 20), page + 1);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.TICKET_SMS_PROVIDER_ERROR);
        }
        if (result == null || !result.isSuccess()) {
            throw new ApiException(ErrorCode.TICKET_SMS_PROVIDER_ERROR);
        }
        String term = StringUtils.hasText(q) ? q.trim().toLowerCase(Locale.ROOT) : null;
        List<TicketSmsLinkableItemDto> items = new ArrayList<>();
        for (SmsDailyPackItemDto row : result.getData() != null ? result.getData() : List.<SmsDailyPackItemDto>of()) {
            if (row == null || !StringUtils.hasText(row.getPackId()) || linked.contains(row.getPackId())) {
                continue;
            }
            if (term != null && !row.getPackId().toLowerCase(Locale.ROOT).contains(term)) {
                continue;
            }
            // Only include packs that actually touched this customer.
            if (!packTouchesMobile(row.getPackId(), mobile)) {
                continue;
            }
            TicketSmsLinkableItemDto item = new TicketSmsLinkableItemDto();
            item.setType(TicketSmsLinkType.PACK);
            item.setExternalId(row.getPackId());
            item.setMobile(mobile);
            item.setRecipientCount(row.getRecipientCount());
            item.setOccurredAt(epochSecondsToInstant(row.getCreationDateTime()));
            item.setStatusLabel("PACK");
            item.setMessagePreview(row.getPackId());
            items.add(item);
        }
        return new PagedResponse<>(
                items,
                items.size(),
                result.isHasMore() ? page + 2 : page + 1,
                page,
                size);
    }

    private boolean packTouchesMobile(String packId, String mobile) {
        try {
            SmsPackReportResultDto report = smsService.fetchPackReport(packId);
            if (report == null || !report.isSuccess() || report.getData() == null) {
                return false;
            }
            return report.getData().stream()
                    .anyMatch(row -> row != null && mobileMatches(mobile, row.getMobile()));
        } catch (Exception ex) {
            return false;
        }
    }

    private SmsDeliveryStatusDataDto findSendByMessageId(String messageId) {
        for (int page = 1; page <= 5; page++) {
            SmsLiveSendResultDto result;
            try {
                result = smsService.fetchLiveSends(50, page);
            } catch (Exception ex) {
                throw new ApiException(ErrorCode.TICKET_SMS_PROVIDER_ERROR);
            }
            if (result == null || !result.isSuccess() || result.getData() == null) {
                throw new ApiException(ErrorCode.TICKET_SMS_PROVIDER_ERROR);
            }
            for (SmsDeliveryStatusDataDto row : result.getData()) {
                if (row != null && row.getMessageId() != null
                        && messageId.equals(String.valueOf(row.getMessageId()))) {
                    return row;
                }
            }
            if (!result.isHasMore()) {
                break;
            }
        }
        return null;
    }

    private TicketSmsLink baseLink(Ticket ticket, TicketSmsLinkType type, String externalId) {
        TicketSmsLink link = new TicketSmsLink();
        link.setTicket(ticket);
        link.setLinkType(type);
        link.setExternalId(externalId);
        return link;
    }

    private RelatedSmsDto toRelatedDto(TicketSmsLink link) {
        RelatedSmsDto dto = new RelatedSmsDto();
        dto.setId(link.getId());
        dto.setType(link.getLinkType());
        dto.setExternalId(link.getExternalId());
        dto.setMobile(link.getMobile());
        dto.setMessagePreview(link.getMessagePreview());
        dto.setLineNumber(link.getLineNumber());
        dto.setStatusLabel(link.getStatusLabel());
        dto.setRecipientCount(link.getRecipientCount());
        dto.setOccurredAt(link.getOccurredAt());
        dto.setCreatedAt(link.getCreatedAt());
        return dto;
    }

    private static PagedResponse<TicketSmsLinkableItemDto> emptyPage(int page, int size) {
        return new PagedResponse<>(List.of(), 0, 0, page, size);
    }

    private static String trimPreview(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String trimmed = text.trim().replaceAll("\\s+", " ");
        return trimmed.length() > 240 ? trimmed.substring(0, 240) : trimmed;
    }

    private static Instant epochSecondsToInstant(Long epochSeconds) {
        if (epochSeconds == null || epochSeconds <= 0) {
            return null;
        }
        // Provider may send seconds or ms; treat large values as ms.
        if (epochSeconds > 10_000_000_000L) {
            return Instant.ofEpochMilli(epochSeconds);
        }
        return Instant.ofEpochSecond(epochSeconds);
    }

    private static boolean mobileMatches(String expected, Long actual) {
        if (!StringUtils.hasText(expected) || actual == null) {
            return false;
        }
        String a = expected.replaceAll("\\D", "");
        String b = String.valueOf(actual).replaceAll("\\D", "");
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return false;
        }
        return a.equals(b) || a.endsWith(b) || b.endsWith(a);
    }

    private static String customerMobile(User requester) {
        if (requester == null) {
            return null;
        }
        return PhoneSmsUtil.toSmsMobile(requester.getPhoneCountryCode(), requester.getPhoneNumber());
    }

    private void requireAgent(User agent) {
        if (agent == null || agent.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Ticket requireStaffTicket(Long ticketId) {
        if (ticketId == null) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
    }
}
