package com.domainify.service;

import com.domainify.dto.DomainDto;
import com.domainify.dto.MarketplaceOrderDto;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.SmsReceivePagedResultDto;
import com.domainify.dto.SmsReceivedMessageDto;
import com.domainify.dto.TicketCustomerContextDto;
import com.domainify.dto.UserDto;
import com.domainify.dto.WalletDto;
import com.domainify.entity.Ticket;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.TicketRepository;
import com.domainify.util.PhoneSmsUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketCustomerContextService {

    private static final int DEFAULT_LIMIT = 8;

    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final WalletService walletService;
    private final DomainService domainService;
    private final MarketplaceOrderService marketplaceOrderService;
    private final SmsService smsService;
    private final SmsConfigService smsConfigService;

    public TicketCustomerContextService(
            TicketRepository ticketRepository,
            UserService userService,
            WalletService walletService,
            DomainService domainService,
            MarketplaceOrderService marketplaceOrderService,
            SmsService smsService,
            SmsConfigService smsConfigService) {
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.walletService = walletService;
        this.domainService = domainService;
        this.marketplaceOrderService = marketplaceOrderService;
        this.smsService = smsService;
        this.smsConfigService = smsConfigService;
    }

    @Transactional(readOnly = true)
    public TicketCustomerContextDto getForTicket(Long ticketId, int limit) {
        if (ticketId == null) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
        User requester = ticket.getRequester();
        if (requester == null || requester.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        int size = Math.min(Math.max(limit, 1), 25);
        Long userId = requester.getId();

        TicketCustomerContextDto dto = new TicketCustomerContextDto();
        dto.setTicketId(ticket.getId());
        dto.setRequesterId(userId);
        dto.setProfile(userService.getById(userId));
        dto.setWallet(toWalletSummary(walletService.getWalletDtoForUserId(userId, 1)));

        PagedResponse<DomainDto> domains = domainService.listForOwnerId(
                userId, null, null, null, null, null, PageRequest.of(0, size));
        dto.setDomains(domains.getContent() != null ? domains.getContent() : List.of());
        dto.setDomainTotal(domains.getTotalElements());

        PagedResponse<MarketplaceOrderDto> orders = marketplaceOrderService.listForUserId(
                userId, "all", PageRequest.of(0, size));
        dto.setOrders(orders.getContent() != null ? orders.getContent() : List.of());
        dto.setOrderTotal(orders.getTotalElements());

        fillSms(dto, requester, size);
        return dto;
    }

    private void fillSms(TicketCustomerContextDto dto, User requester, int size) {
        String mobile = PhoneSmsUtil.toSmsMobile(requester.getPhoneCountryCode(), requester.getPhoneNumber());
        dto.setSmsMobile(mobile);
        if (!StringUtils.hasText(mobile)) {
            dto.setSmsAvailable(false);
            dto.setSmsUnavailableReason("NO_PHONE");
            dto.setRecentSms(List.of());
            return;
        }
        if (!StringUtils.hasText(smsConfigService.getApiKey())) {
            dto.setSmsAvailable(false);
            dto.setSmsUnavailableReason("SMS_DISABLED");
            dto.setRecentSms(List.of());
            return;
        }
        try {
            SmsReceivePagedResultDto result = smsService.fetchLiveReceived(size, 1, true, mobile);
            if (result == null || !result.isSuccess()) {
                dto.setSmsAvailable(false);
                dto.setSmsUnavailableReason("PROVIDER_ERROR");
                dto.setRecentSms(List.of());
                return;
            }
            dto.setSmsAvailable(true);
            List<TicketCustomerContextDto.SmsSnippet> snippets = new ArrayList<>();
            for (SmsReceivedMessageDto msg : result.getData()) {
                if (msg == null) {
                    continue;
                }
                TicketCustomerContextDto.SmsSnippet snip = new TicketCustomerContextDto.SmsSnippet();
                snip.setReceiveReturnId(msg.getReceiveReturnId());
                snip.setMessageText(msg.getMessageText());
                snip.setMobile(msg.getMobile() != null ? String.valueOf(msg.getMobile()) : mobile);
                snip.setReceivedDateTime(msg.getReceivedDateTime());
                snip.setLineNumber(msg.getNumber());
                snippets.add(snip);
            }
            dto.setRecentSms(snippets);
        } catch (Exception ex) {
            dto.setSmsAvailable(false);
            dto.setSmsUnavailableReason("PROVIDER_ERROR");
            dto.setRecentSms(List.of());
        }
    }

    private TicketCustomerContextDto.WalletSummary toWalletSummary(WalletDto wallet) {
        TicketCustomerContextDto.WalletSummary summary = new TicketCustomerContextDto.WalletSummary();
        if (wallet == null) {
            summary.setAvailableBalance(BigDecimal.ZERO);
            summary.setHeldBalance(BigDecimal.ZERO);
            summary.setPaymentsEnabled(false);
            return summary;
        }
        summary.setWalletId(wallet.getWalletId());
        summary.setAvailableBalance(wallet.getAvailableBalance() != null
                ? wallet.getAvailableBalance() : BigDecimal.ZERO);
        summary.setHeldBalance(wallet.getHeldBalance() != null
                ? wallet.getHeldBalance() : BigDecimal.ZERO);
        summary.setPaymentsEnabled(wallet.isPaymentsEnabled());
        return summary;
    }
}
