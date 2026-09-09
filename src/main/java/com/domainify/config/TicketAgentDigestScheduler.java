package com.domainify.config;

import com.domainify.service.TicketAgentDigestService;
import com.domainify.service.TicketSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Minute tick that sends the agent ticket digest once per day at/after the configured send time.
 */
@Component
public class TicketAgentDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketAgentDigestScheduler.class);

    private final TicketSettingsService ticketSettingsService;
    private final TicketAgentDigestService ticketAgentDigestService;

    public TicketAgentDigestScheduler(
            TicketSettingsService ticketSettingsService,
            TicketAgentDigestService ticketAgentDigestService) {
        this.ticketSettingsService = ticketSettingsService;
        this.ticketAgentDigestService = ticketAgentDigestService;
    }

    @Scheduled(cron = "${app.ticket.agent-digest.check-cron:0 * * * * *}")
    public void sendAgentDigests() {
        try {
            if (!ticketSettingsService.shouldRunAgentDigestNow()) {
                return;
            }
            int count = ticketAgentDigestService.processDailyDigest();
            ticketSettingsService.markAgentDigestRanToday();
            if (count > 0) {
                log.info("Sent agent ticket digest email(s) to {} recipient(s)", count);
            } else {
                log.debug("Agent ticket digest check completed with no emails sent");
            }
        } catch (Exception ex) {
            log.warn("Agent ticket digest run failed: {}", ex.getMessage());
        }
    }
}
