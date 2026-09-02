package com.domainify.config;

import com.domainify.service.TicketCategoryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * One-shot cleanup of legacy seeded ticket categories after defaults were removed.
 */
@Component
public class TicketCategoryCleanup implements ApplicationRunner {

    private final TicketCategoryService ticketCategoryService;

    public TicketCategoryCleanup(TicketCategoryService ticketCategoryService) {
        this.ticketCategoryService = ticketCategoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ticketCategoryService.removeUnusedLegacyDefaults();
    }
}
