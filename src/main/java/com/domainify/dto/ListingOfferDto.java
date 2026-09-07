package com.domainify.dto;

import com.domainify.entity.Domain;
import com.domainify.entity.DomainListing;
import com.domainify.entity.DomainListingOffer;
import com.domainify.entity.DomainListingOfferEvent;
import com.domainify.entity.DomainListingOfferEventAction;
import com.domainify.entity.DomainListingOfferStatus;
import com.domainify.entity.User;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ListingOfferDto {

    private Long id;
    private Long listingId;
    private Long domainId;
    private String domainName;
    private BigDecimal askingPrice;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private BigDecimal amount;
    private String message;
    private DomainListingOfferStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant respondedAt;
    private List<OfferEventDto> events = new ArrayList<>();

    public static ListingOfferDto from(DomainListingOffer offer) {
        return from(offer, null);
    }

    public static ListingOfferDto from(DomainListingOffer offer, List<DomainListingOfferEvent> events) {
        ListingOfferDto dto = new ListingOfferDto();
        dto.id = offer.getId();
        DomainListing listing = offer.getListing();
        if (listing != null) {
            dto.listingId = listing.getId();
            dto.askingPrice = listing.getAskingPrice();
            Domain domain = listing.getDomain();
            if (domain != null) {
                dto.domainId = domain.getId();
                dto.domainName = domain.getName();
            }
        }
        dto.buyerId = offer.getBuyer() != null ? offer.getBuyer().getId() : null;
        dto.buyerName = displayName(offer.getBuyer());
        dto.sellerId = offer.getSeller() != null ? offer.getSeller().getId() : null;
        dto.sellerName = displayName(offer.getSeller());
        dto.amount = offer.getAmount();
        dto.message = offer.getMessage();
        dto.status = offer.getStatus();
        dto.createdAt = offer.getCreatedAt();
        dto.updatedAt = offer.getUpdatedAt();
        dto.respondedAt = offer.getRespondedAt();
        if (events != null) {
            for (DomainListingOfferEvent event : events) {
                dto.events.add(OfferEventDto.from(event));
            }
        }
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

    public BigDecimal getAskingPrice() {
        return askingPrice;
    }

    public void setAskingPrice(BigDecimal askingPrice) {
        this.askingPrice = askingPrice;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DomainListingOfferStatus getStatus() {
        return status;
    }

    public void setStatus(DomainListingOfferStatus status) {
        this.status = status;
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

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public List<OfferEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<OfferEventDto> events) {
        this.events = events;
    }

    public static class OfferEventDto {
        private Long id;
        private Long actorId;
        private String actorName;
        private DomainListingOfferEventAction action;
        private BigDecimal amount;
        private String message;
        private Instant createdAt;

        public static OfferEventDto from(DomainListingOfferEvent event) {
            OfferEventDto dto = new OfferEventDto();
            dto.id = event.getId();
            if (event.getActor() != null) {
                dto.actorId = event.getActor().getId();
                dto.actorName = displayName(event.getActor());
            }
            dto.action = event.getAction();
            dto.amount = event.getAmount();
            dto.message = event.getMessage();
            dto.createdAt = event.getCreatedAt();
            return dto;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getActorId() {
            return actorId;
        }

        public void setActorId(Long actorId) {
            this.actorId = actorId;
        }

        public String getActorName() {
            return actorName;
        }

        public void setActorName(String actorName) {
            this.actorName = actorName;
        }

        public DomainListingOfferEventAction getAction() {
            return action;
        }

        public void setAction(DomainListingOfferEventAction action) {
            this.action = action;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
}
