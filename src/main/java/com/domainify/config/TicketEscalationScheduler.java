package com.domainify.config;

import com.domainify.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic SLA breach escalations (also triggered opportunistically on inbox load).
 */
@Component
@EnableScheduling
public class TicketEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketEscalationScheduler.class);

    private final TicketService ticketService;

    public TicketEscalationScheduler(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Scheduled(fixedDelayString = "${app.tickets.sla-escalation-delay-ms:60000}")
    public void escalateOverdueTickets() {
        try {
            int count = ticketService.autoEscalateOverdueTickets();
            if (count > 0) {
                log.info("Auto-escalated {} overdue ticket(s)", count);
            }
        } catch (Exception ex) {
            log.warn("SLA auto-escalation run failed: {}", ex.getMessage());
        }
    }
}
