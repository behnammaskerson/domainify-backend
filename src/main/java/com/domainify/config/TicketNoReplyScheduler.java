package com.domainify.config;

import com.domainify.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic no-reply automations (remind customer or escalate while waiting on customer).
 */
@Component
public class TicketNoReplyScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketNoReplyScheduler.class);

    private final TicketService ticketService;

    public TicketNoReplyScheduler(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Scheduled(fixedDelayString = "${app.tickets.no-reply-automation-delay-ms:60000}")
    public void processNoReplyAutomations() {
        try {
            int processed = ticketService.processNoReplyAutomations();
            if (processed > 0) {
                log.info("Processed no-reply automations for {} ticket(s)", processed);
            }
        } catch (Exception ex) {
            log.warn("No-reply automation run failed: {}", ex.getMessage());
        }
    }
}
