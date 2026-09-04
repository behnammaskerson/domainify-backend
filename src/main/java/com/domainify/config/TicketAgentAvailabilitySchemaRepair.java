package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds users.ticket_available for optional agent presence (default available).
 */
@Component
@Order(54)
public class TicketAgentAvailabilitySchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketAgentAvailabilitySchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketAgentAvailabilitySchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS ticket_available BOOLEAN DEFAULT true");
            jdbcTemplate.execute("""
                    UPDATE users
                    SET ticket_available = true
                    WHERE ticket_available IS NULL
                    """);
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN ticket_available SET DEFAULT true");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN ticket_available SET NOT NULL");
            log.info("Ensured users.ticket_available column exists");
        } catch (Exception ex) {
            log.warn("Agent ticket availability schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
