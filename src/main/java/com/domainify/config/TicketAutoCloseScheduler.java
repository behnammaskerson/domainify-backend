package com.domainify.config;

import com.domainify.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Auto-closes RESOLVED tickets when the customer stays silent for N days.
 */
@Component
public class TicketAutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketAutoCloseScheduler.class);

    private final TicketService ticketService;

    public TicketAutoCloseScheduler(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Scheduled(fixedDelayString = "${app.tickets.auto-close-after-resolve-delay-ms:60000}")
    public void processAutoCloseAfterResolve() {
        try {
            int processed = ticketService.processAutoCloseAfterResolve();
            if (processed > 0) {
                log.info("Auto-closed {} resolved ticket(s) after customer silence", processed);
            }
        } catch (Exception ex) {
            log.warn("Auto-close after resolve run failed: {}", ex.getMessage());
        }
    }
}
