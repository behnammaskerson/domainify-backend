package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds ticket automation settings and no-reply tracking columns.
 */
@Component
@Order(91)
public class TicketAutomationsSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketAutomationsSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketAutomationsSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS automation_default_priority VARCHAR(16)
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS automation_customer_ack_enabled BOOLEAN DEFAULT true
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET automation_customer_ack_enabled = true
                     WHERE automation_customer_ack_enabled IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS automation_no_reply_enabled BOOLEAN DEFAULT false
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET automation_no_reply_enabled = false
                     WHERE automation_no_reply_enabled IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS automation_no_reply_hours INTEGER DEFAULT 48
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET automation_no_reply_hours = 48
                     WHERE automation_no_reply_hours IS NULL OR automation_no_reply_hours < 1
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS automation_no_reply_action VARCHAR(32) DEFAULT 'REMIND'
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET automation_no_reply_action = 'REMIND'
                     WHERE automation_no_reply_action IS NULL OR automation_no_reply_action = ''
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS automation_csat_invite_enabled BOOLEAN DEFAULT true
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET automation_csat_invite_enabled = true
                     WHERE automation_csat_invite_enabled IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS last_staff_public_reply_at TIMESTAMPTZ
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS last_customer_public_reply_at TIMESTAMPTZ
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS no_reply_reminded_at TIMESTAMPTZ
                    """);

            log.info("Ticket automations schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket automations schema repair skipped: {}", ex.getMessage());
        }
    }
}
