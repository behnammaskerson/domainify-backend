package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures tickets.channel exists and is backfilled for rows created before the column.
 */
@Component
public class TicketChannelSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketChannelSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketChannelSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE tickets ADD COLUMN IF NOT EXISTS channel VARCHAR(16)"
            );
            jdbcTemplate.update(
                    "UPDATE tickets SET channel = 'PORTAL' WHERE channel IS NULL OR channel = ''"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE tickets ALTER COLUMN channel SET DEFAULT 'PORTAL'"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE tickets ALTER COLUMN channel SET NOT NULL"
            );
        } catch (Exception ex) {
            log.warn("Ticket channel schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
