package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds SLA pause timestamp column to tickets.
 */
@Component
@Order(89)
public class TicketSlaPauseSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketSlaPauseSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketSlaPauseSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS sla_paused_at TIMESTAMPTZ
                    """);
        } catch (Exception ex) {
            log.warn("Ticket SLA pause schema repair failed: {}", ex.getMessage());
        }
    }
}
