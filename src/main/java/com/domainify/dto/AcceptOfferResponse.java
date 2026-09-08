package com.domainify.dto;

public class AcceptOfferResponse {

    private ListingOfferDto offer;
    private MarketplaceOrderDto order;

    public AcceptOfferResponse() {
    }

    public AcceptOfferResponse(ListingOfferDto offer, MarketplaceOrderDto order) {
        this.offer = offer;
        this.order = order;
    }

    public ListingOfferDto getOffer() {
        return offer;
    }

    public void setOffer(ListingOfferDto offer) {
        this.offer = offer;
    }

    public MarketplaceOrderDto getOrder() {
        return order;
    }

    public void setOrder(MarketplaceOrderDto order) {
        this.order = order;
    }
}
