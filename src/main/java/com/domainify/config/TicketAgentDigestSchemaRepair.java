package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds org-level agent digest schedule columns and per-user digest opt-in.
 */
@Component
@Order(86)
public class TicketAgentDigestSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketAgentDigestSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketAgentDigestSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS agent_digest_enabled BOOLEAN DEFAULT false
                    """);
            jdbcTemplate.update(
                    "UPDATE ticket_settings SET agent_digest_enabled = FALSE WHERE agent_digest_enabled IS NULL");
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS agent_digest_send_hour INTEGER DEFAULT 8
                    """);
            jdbcTemplate.update(
                    "UPDATE ticket_settings SET agent_digest_send_hour = 8 WHERE agent_digest_send_hour IS NULL");
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS agent_digest_send_minute INTEGER DEFAULT 0
                    """);
            jdbcTemplate.update(
                    "UPDATE ticket_settings SET agent_digest_send_minute = 0 WHERE agent_digest_send_minute IS NULL");
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS agent_digest_last_run_date DATE
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE users
                      ADD COLUMN IF NOT EXISTS ticket_digest_email_enabled BOOLEAN DEFAULT false
                    """);
            jdbcTemplate.update(
                    "UPDATE users SET ticket_digest_email_enabled = FALSE WHERE ticket_digest_email_enabled IS NULL");
            log.info("Ticket agent digest schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket agent digest schema repair skipped: {}", ex.getMessage());
        }
    }
}
