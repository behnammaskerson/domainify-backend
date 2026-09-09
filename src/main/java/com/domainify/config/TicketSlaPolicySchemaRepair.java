package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds first-response SLA settings, ticket first-response timestamps, and per-category SLA overrides.
 */
@Component
@Order(87)
public class TicketSlaPolicySchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketSlaPolicySchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketSlaPolicySchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS first_response_sla_urgent_hours INTEGER DEFAULT 1
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET first_response_sla_urgent_hours = 1
                     WHERE first_response_sla_urgent_hours IS NULL
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS first_response_sla_high_hours INTEGER DEFAULT 4
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET first_response_sla_high_hours = 4
                     WHERE first_response_sla_high_hours IS NULL
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS first_response_sla_medium_hours INTEGER DEFAULT 8
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET first_response_sla_medium_hours = 8
                     WHERE first_response_sla_medium_hours IS NULL
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS first_response_sla_low_hours INTEGER DEFAULT 24
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET first_response_sla_low_hours = 24
                     WHERE first_response_sla_low_hours IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS first_response_due_at TIMESTAMPTZ
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS first_responded_at TIMESTAMPTZ
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_categories
                      ADD COLUMN IF NOT EXISTS first_response_sla_urgent_hours INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_categories
                      ADD COLUMN IF NOT EXISTS first_response_sla_high_hours INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_categories
                      ADD COLUMN IF NOT EXISTS first_response_sla_medium_hours INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_categories
                      ADD COLUMN IF NOT EXISTS first_response_sla_low_hours INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_categories
                      ADD COLUMN IF NOT EXISTS resolve_sla_urgent_hours INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_categories
                      ADD COLUMN IF NOT EXISTS resolve_sla_high_hours INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_categories
                      ADD COLUMN IF NOT EXISTS resolve_sla_medium_hours INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_categories
                      ADD COLUMN IF NOT EXISTS resolve_sla_low_hours INTEGER
                    """);

            jdbcTemplate.update("""
                    UPDATE tickets t
                       SET first_response_due_at = t.created_at + (
                           CASE t.priority
                               WHEN 'URGENT' THEN INTERVAL '1 hour' * COALESCE(ts.first_response_sla_urgent_hours, 1)
                               WHEN 'HIGH' THEN INTERVAL '1 hour' * COALESCE(ts.first_response_sla_high_hours, 4)
                               WHEN 'MEDIUM' THEN INTERVAL '1 hour' * COALESCE(ts.first_response_sla_medium_hours, 8)
                               WHEN 'LOW' THEN INTERVAL '1 hour' * COALESCE(ts.first_response_sla_low_hours, 24)
                               ELSE INTERVAL '24 hours'
                           END
                       )
                      FROM ticket_settings ts
                     WHERE t.first_response_due_at IS NULL
                       AND t.due_at IS NOT NULL
                       AND ts.id = 1
                    """);

            log.info("Ticket SLA policy schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket SLA policy schema repair skipped: {}", ex.getMessage());
        }
    }
}
