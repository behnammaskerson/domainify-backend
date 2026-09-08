package com.domainify.dto;

import com.domainify.entity.Domain;
import com.domainify.entity.DomainListing;
import com.domainify.entity.MarketplaceOrder;
import com.domainify.entity.MarketplaceOrderPaymentMethod;
import com.domainify.entity.MarketplaceOrderStatus;
import com.domainify.entity.User;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

public class MarketplaceOrderDto {

    private Long id;
    private Long listingId;
    private String domainName;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private Long offerId;
    private BigDecimal grossAmount;
    private BigDecimal commissionAmount;
    private BigDecimal sellerNet;
    private MarketplaceOrderStatus status;
    private MarketplaceOrderPaymentMethod paymentMethod;
    private Long paymentIntentId;
    private Instant paidAt;
    private Instant releasedAt;
    private Instant cancelledAt;
    private Instant paymentDeadline;
    private Instant createdAt;
    private Instant updatedAt;

    public static MarketplaceOrderDto from(MarketplaceOrder order) {
        MarketplaceOrderDto dto = new MarketplaceOrderDto();
        dto.id = order.getId();
        DomainListing listing = order.getListing();
        if (listing != null) {
            dto.listingId = listing.getId();
            Domain domain = listing.getDomain();
            if (domain != null) {
                dto.domainName = domain.getName();
            }
        }
        dto.buyerId = order.getBuyer() != null ? order.getBuyer().getId() : null;
        dto.buyerName = displayName(order.getBuyer());
        dto.sellerId = order.getSeller() != null ? order.getSeller().getId() : null;
        dto.sellerName = displayName(order.getSeller());
        dto.offerId = order.getOffer() != null ? order.getOffer().getId() : null;
        dto.grossAmount = order.getGrossAmount();
        dto.commissionAmount = order.getCommissionAmount();
        dto.sellerNet = order.getSellerNet();
        dto.status = order.getStatus();
        dto.paymentMethod = order.getPaymentMethod();
        dto.paymentIntentId = order.getPaymentIntentId();
        dto.paidAt = order.getPaidAt();
        dto.releasedAt = order.getReleasedAt();
        dto.cancelledAt = order.getCancelledAt();
        dto.paymentDeadline = order.getPaymentDeadline();
        dto.createdAt = order.getCreatedAt();
        dto.updatedAt = order.getUpdatedAt();
        return dto;
    }

    private static String displayName(User user) {
        if (user == null) {
            return null;
        }
        String name = ((StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : "")
                + " "
                + (StringUtils.hasText(user.getLastName()) ? user.getLastName() : "")).trim();
        return StringUtils.hasText(name) ? name : user.getEmail();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public BigDecimal getSellerNet() {
        return sellerNet;
    }

    public void setSellerNet(BigDecimal sellerNet) {
        this.sellerNet = sellerNet;
    }

    public MarketplaceOrderStatus getStatus() {
        return status;
    }

    public void setStatus(MarketplaceOrderStatus status) {
        this.status = status;
    }

    public MarketplaceOrderPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(MarketplaceOrderPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(Long paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getPaymentDeadline() {
        return paymentDeadline;
    }

    public void setPaymentDeadline(Instant paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
