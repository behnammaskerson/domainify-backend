package com.domainify.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated customer context for the admin ticket detail side panel.
 */
public class TicketCustomerContextDto {

    private Long ticketId;
    private Long requesterId;
    private UserDto profile;
    private WalletSummary wallet;
    private List<DomainDto> domains = new ArrayList<>();
    private long domainTotal;
    private List<MarketplaceOrderDto> orders = new ArrayList<>();
    private long orderTotal;
    private List<SmsSnippet> recentSms = new ArrayList<>();
    private String smsMobile;
    private boolean smsAvailable;
    private String smsUnavailableReason;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public UserDto getProfile() {
        return profile;
    }

    public void setProfile(UserDto profile) {
        this.profile = profile;
    }

    public WalletSummary getWallet() {
        return wallet;
    }

    public void setWallet(WalletSummary wallet) {
        this.wallet = wallet;
    }

    public List<DomainDto> getDomains() {
        return domains;
    }

    public void setDomains(List<DomainDto> domains) {
        this.domains = domains != null ? domains : new ArrayList<>();
    }

    public long getDomainTotal() {
        return domainTotal;
    }

    public void setDomainTotal(long domainTotal) {
        this.domainTotal = domainTotal;
    }

    public List<MarketplaceOrderDto> getOrders() {
        return orders;
    }

    public void setOrders(List<MarketplaceOrderDto> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
    }

    public long getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(long orderTotal) {
        this.orderTotal = orderTotal;
    }

    public List<SmsSnippet> getRecentSms() {
        return recentSms;
    }

    public void setRecentSms(List<SmsSnippet> recentSms) {
        this.recentSms = recentSms != null ? recentSms : new ArrayList<>();
    }

    public String getSmsMobile() {
        return smsMobile;
    }

    public void setSmsMobile(String smsMobile) {
        this.smsMobile = smsMobile;
    }

    public boolean isSmsAvailable() {
        return smsAvailable;
    }

    public void setSmsAvailable(boolean smsAvailable) {
        this.smsAvailable = smsAvailable;
    }

    public String getSmsUnavailableReason() {
        return smsUnavailableReason;
    }

    public void setSmsUnavailableReason(String smsUnavailableReason) {
        this.smsUnavailableReason = smsUnavailableReason;
    }

    public static class WalletSummary {
        private Long walletId;
        private BigDecimal availableBalance;
        private BigDecimal heldBalance;
        private boolean paymentsEnabled;

        public Long getWalletId() {
            return walletId;
        }

        public void setWalletId(Long walletId) {
            this.walletId = walletId;
        }

        public BigDecimal getAvailableBalance() {
            return availableBalance;
        }

        public void setAvailableBalance(BigDecimal availableBalance) {
            this.availableBalance = availableBalance;
        }

        public BigDecimal getHeldBalance() {
            return heldBalance;
        }

        public void setHeldBalance(BigDecimal heldBalance) {
            this.heldBalance = heldBalance;
        }

        public boolean isPaymentsEnabled() {
            return paymentsEnabled;
        }

        public void setPaymentsEnabled(boolean paymentsEnabled) {
            this.paymentsEnabled = paymentsEnabled;
        }
    }

    public static class SmsSnippet {
        private Long receiveReturnId;
        private String messageText;
        private String mobile;
        private Long receivedDateTime;
        private Long lineNumber;

        public Long getReceiveReturnId() {
            return receiveReturnId;
        }

        public void setReceiveReturnId(Long receiveReturnId) {
            this.receiveReturnId = receiveReturnId;
        }

        public String getMessageText() {
            return messageText;
        }

        public void setMessageText(String messageText) {
            this.messageText = messageText;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public Long getReceivedDateTime() {
            return receivedDateTime;
        }

        public void setReceivedDateTime(Long receivedDateTime) {
            this.receivedDateTime = receivedDateTime;
        }

        public Long getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(Long lineNumber) {
            this.lineNumber = lineNumber;
        }
    }
}
