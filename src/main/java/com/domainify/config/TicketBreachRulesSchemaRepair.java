package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds SLA breach warning and escalation rule columns to ticket_settings and tickets.
 */
@Component
@Order(90)
public class TicketBreachRulesSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketBreachRulesSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketBreachRulesSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS sla_warn_enabled BOOLEAN DEFAULT false
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET sla_warn_enabled = false
                     WHERE sla_warn_enabled IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS sla_warn_hours_before INTEGER DEFAULT 2
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET sla_warn_hours_before = 2
                     WHERE sla_warn_hours_before IS NULL OR sla_warn_hours_before < 1
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS sla_breach_escalation_enabled BOOLEAN DEFAULT true
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET sla_breach_escalation_enabled = true
                     WHERE sla_breach_escalation_enabled IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS sla_breach_bump_priority BOOLEAN DEFAULT true
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET sla_breach_bump_priority = true
                     WHERE sla_breach_bump_priority IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS sla_breach_assignee_id BIGINT
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS sla_breach_queue_id BIGINT
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS sla_warned_at TIMESTAMPTZ
                    """);

            log.info("Ticket breach rules schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket breach rules schema repair skipped: {}", ex.getMessage());
        }
    }
}
