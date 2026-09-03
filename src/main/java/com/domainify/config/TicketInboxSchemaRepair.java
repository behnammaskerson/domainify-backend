package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures ticket assignee/due_at columns exist and backfills due dates for legacy rows.
 */
@Component
public class TicketInboxSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketInboxSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketInboxSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assignee_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS due_at TIMESTAMPTZ");

            jdbcTemplate.execute("""
                    UPDATE tickets
                    SET due_at = created_at + (
                        CASE priority
                            WHEN 'URGENT' THEN INTERVAL '4 hours'
                            WHEN 'HIGH' THEN INTERVAL '24 hours'
                            WHEN 'MEDIUM' THEN INTERVAL '72 hours'
                            ELSE INTERVAL '168 hours'
                        END
                    )
                    WHERE due_at IS NULL
                    """);
        } catch (Exception ex) {
            log.warn("Ticket inbox schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
