package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates ticket_watchers join table for followers who receive updates without being assignee.
 */
@Component
@Order(51)
public class TicketWatcherSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketWatcherSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketWatcherSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_watchers (
                        id BIGSERIAL PRIMARY KEY,
                        ticket_id BIGINT NOT NULL REFERENCES tickets(id),
                        user_id BIGINT NOT NULL REFERENCES users(id),
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        CONSTRAINT uk_ticket_watchers_ticket_user UNIQUE (ticket_id, user_id)
                    )
                    """);
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_watchers_user ON ticket_watchers (user_id)");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_watchers_ticket ON ticket_watchers (ticket_id)");
            log.info("Ensured ticket_watchers table exists");
        } catch (Exception ex) {
            log.warn("Ticket watcher schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
