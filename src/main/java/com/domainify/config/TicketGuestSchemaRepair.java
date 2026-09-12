package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Guest ticket columns: nullable requester, guest identity, access token, contact default category.
 */
@Component
@Order(92)
public class TicketGuestSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketGuestSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketGuestSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ALTER COLUMN requester_id DROP NOT NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS guest_name VARCHAR(120)
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS guest_email VARCHAR(255)
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS guest_email_verified BOOLEAN DEFAULT false
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS guest_access_token_hash VARCHAR(64)
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE tickets
                      ADD COLUMN IF NOT EXISTS guest_access_token_expires_at TIMESTAMP WITH TIME ZONE
                    """);

            jdbcTemplate.update("""
                    UPDATE tickets
                       SET guest_email_verified = true
                     WHERE guest_email IS NULL
                       AND (guest_email_verified IS NULL OR guest_email_verified = false)
                    """);

            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tickets_guest_email
                        ON tickets (guest_email)
                    """);
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_tickets_guest_access_token_hash
                        ON tickets (guest_access_token_hash)
                     WHERE guest_access_token_hash IS NOT NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS contact_default_category_id BIGINT
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS guest_ticket_create_enabled BOOLEAN DEFAULT true
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET guest_ticket_create_enabled = true
                     WHERE guest_ticket_create_enabled IS NULL
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS guest_ticket_attachments_enabled BOOLEAN DEFAULT true
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET guest_ticket_attachments_enabled = true
                     WHERE guest_ticket_attachments_enabled IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_messages
                      ALTER COLUMN author_id DROP NOT NULL
                    """);

            log.info("Ticket guest schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket guest schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
