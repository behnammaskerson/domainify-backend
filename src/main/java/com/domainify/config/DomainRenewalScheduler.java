package com.domainify.config;

import com.domainify.service.DomainRenewalReminderService;
import com.domainify.service.DomainSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Minute tick that runs domain renewal reminders once per day at/after the configured send time.
 */
@Component
@EnableScheduling
public class DomainRenewalScheduler {

    private static final Logger log = LoggerFactory.getLogger(DomainRenewalScheduler.class);

    private final DomainSettingsService domainSettingsService;
    private final DomainRenewalReminderService domainRenewalReminderService;

    public DomainRenewalScheduler(
            DomainSettingsService domainSettingsService,
            DomainRenewalReminderService domainRenewalReminderService) {
        this.domainSettingsService = domainSettingsService;
        this.domainRenewalReminderService = domainRenewalReminderService;
    }

    @Scheduled(cron = "${app.domain.renewal.check-cron:0 * * * * *}")
    public void sendRenewalReminders() {
        try {
            if (!domainSettingsService.shouldRunNow()) {
                return;
            }
            int count = domainRenewalReminderService.processDueReminders();
            domainSettingsService.markRanToday();
            if (count > 0) {
                log.info("Sent domain renewal reminder(s) for {} domain window(s)", count);
            } else {
                log.debug("Domain renewal check completed with no new reminders");
            }
        } catch (Exception ex) {
            log.warn("Domain renewal reminder run failed: {}", ex.getMessage());
        }
    }
}
