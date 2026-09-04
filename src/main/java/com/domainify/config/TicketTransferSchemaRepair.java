package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates ticket_transfers audit table for agent/department handoffs.
 */
@Component
@Order(52)
public class TicketTransferSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketTransferSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketTransferSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_transfers (
                        id BIGSERIAL PRIMARY KEY,
                        ticket_id BIGINT NOT NULL REFERENCES tickets(id),
                        transferred_by_id BIGINT NOT NULL REFERENCES users(id),
                        from_assignee_id BIGINT REFERENCES users(id),
                        to_assignee_id BIGINT REFERENCES users(id),
                        from_queue_id BIGINT REFERENCES ticket_queues(id),
                        to_queue_id BIGINT REFERENCES ticket_queues(id),
                        note TEXT,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_transfers_ticket ON ticket_transfers (ticket_id)");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_transfers_created ON ticket_transfers (created_at)");
            log.info("Ensured ticket_transfers table exists");
        } catch (Exception ex) {
            log.warn("Ticket transfer schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
