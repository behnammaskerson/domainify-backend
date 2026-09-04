package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates ticket queue tables and tickets.queue_id.
 */
@Component
@Order(50)
public class TicketQueueSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketQueueSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketQueueSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_queues (
                        id BIGSERIAL PRIMARY KEY,
                        code VARCHAR(64) NOT NULL UNIQUE,
                        name VARCHAR(100) NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT true,
                        sort_order INTEGER NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_agent_queue_memberships (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL REFERENCES users(id),
                        queue_id BIGINT NOT NULL REFERENCES ticket_queues(id),
                        CONSTRAINT uk_ticket_agent_queue_membership UNIQUE (user_id, queue_id)
                    )
                    """);
            jdbcTemplate.execute(
                    "ALTER TABLE tickets ADD COLUMN IF NOT EXISTS queue_id BIGINT");
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                      IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tickets_queue'
                      ) THEN
                        ALTER TABLE tickets
                          ADD CONSTRAINT fk_tickets_queue
                          FOREIGN KEY (queue_id) REFERENCES ticket_queues(id);
                      END IF;
                    END $$;
                    """);
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_tickets_queue ON tickets (queue_id)");
            jdbcTemplate.execute(
                    "ALTER TABLE ticket_settings ADD COLUMN IF NOT EXISTS default_queue_id BIGINT");
        } catch (Exception ex) {
            log.warn("Ticket queue schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
