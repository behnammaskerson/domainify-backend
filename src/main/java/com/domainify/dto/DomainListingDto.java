package com.domainify.dto;

import com.domainify.entity.Domain;
import com.domainify.entity.DomainListing;
import com.domainify.entity.DomainListingStatus;
import com.domainify.entity.User;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

public class DomainListingDto {

    private Long id;
    private Long domainId;
    private String domainName;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private String sellerName;
    private BigDecimal askingPrice;
    private String description;
    private DomainListingStatus status;
    private boolean featured;
    private Instant createdAt;
    private Instant updatedAt;

    public static DomainListingDto from(DomainListing listing) {
        DomainListingDto dto = new DomainListingDto();
        dto.id = listing.getId();
        Domain domain = listing.getDomain();
        if (domain != null) {
            dto.domainId = domain.getId();
            dto.domainName = domain.getName();
            if (domain.getCategory() != null) {
                dto.categoryId = domain.getCategory().getId();
                dto.categoryName = domain.getCategory().getName();
            }
        }
        User seller = listing.getSeller();
        if (seller != null) {
            dto.sellerId = seller.getId();
            String name = ((StringUtils.hasText(seller.getFirstName()) ? seller.getFirstName() : "")
                    + " "
                    + (StringUtils.hasText(seller.getLastName()) ? seller.getLastName() : "")).trim();
            dto.sellerName = StringUtils.hasText(name) ? name : seller.getEmail();
        }
        dto.askingPrice = listing.getAskingPrice();
        dto.description = listing.getDescription();
        dto.status = listing.getStatus();
        dto.featured = listing.isFeatured();
        dto.createdAt = listing.getCreatedAt();
        dto.updatedAt = listing.getUpdatedAt();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public BigDecimal getAskingPrice() {
        return askingPrice;
    }

    public void setAskingPrice(BigDecimal askingPrice) {
        this.askingPrice = askingPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DomainListingStatus getStatus() {
        return status;
    }

    public void setStatus(DomainListingStatus status) {
        this.status = status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
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
