package com.domainify.config;

import com.domainify.service.DomainListingOfferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly job that expires idle PENDING/COUNTERED marketplace offers.
 */
@Component
public class DomainListingOfferExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DomainListingOfferExpiryScheduler.class);

    private final DomainListingOfferService offerService;

    public DomainListingOfferExpiryScheduler(DomainListingOfferService offerService) {
        this.offerService = offerService;
    }

    @Scheduled(cron = "${app.marketplace.offer.expiry-check-cron:0 0 * * * *}")
    public void expireStaleOffers() {
        try {
            int count = offerService.expireStaleOffers();
            if (count > 0) {
                log.info("Expired {} marketplace offer(s)", count);
            } else {
                log.debug("Marketplace offer expiry check completed with no changes");
            }
        } catch (Exception ex) {
            log.warn("Marketplace offer expiry run failed: {}", ex.getMessage());
        }
    }
}
