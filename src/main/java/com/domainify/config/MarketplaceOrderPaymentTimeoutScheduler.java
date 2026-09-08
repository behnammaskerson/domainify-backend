package com.domainify.config;

import com.domainify.service.MarketplaceOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cancels unpaid PENDING_PAYMENT marketplace orders past their payment deadline.
 */
@Component
public class MarketplaceOrderPaymentTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceOrderPaymentTimeoutScheduler.class);

    private final MarketplaceOrderService orderService;

    public MarketplaceOrderPaymentTimeoutScheduler(MarketplaceOrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(cron = "${app.marketplace.order.payment-timeout-check-cron:0 */15 * * * *}")
    public void cancelExpiredPendingOrders() {
        try {
            int count = orderService.cancelExpiredPending();
            if (count > 0) {
                log.info("Cancelled {} unpaid marketplace order(s)", count);
            } else {
                log.debug("Marketplace order payment-timeout check completed with no changes");
            }
        } catch (Exception ex) {
            log.warn("Marketplace order payment-timeout run failed: {}", ex.getMessage());
        }
    }
}
