package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates ticket escalation audit table and tickets.escalated_at.
 */
@Component
@Order(53)
public class TicketEscalationSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketEscalationSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketEscalationSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE tickets ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_tickets_escalated_at ON tickets (escalated_at)");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_escalations (
                        id BIGSERIAL PRIMARY KEY,
                        ticket_id BIGINT NOT NULL REFERENCES tickets(id),
                        escalated_by_id BIGINT REFERENCES users(id),
                        trigger_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
                        from_priority VARCHAR(16),
                        to_priority VARCHAR(16),
                        from_assignee_id BIGINT REFERENCES users(id),
                        to_assignee_id BIGINT REFERENCES users(id),
                        from_queue_id BIGINT REFERENCES ticket_queues(id),
                        to_queue_id BIGINT REFERENCES ticket_queues(id),
                        note TEXT,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_escalations_ticket ON ticket_escalations (ticket_id)");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_escalations_created ON ticket_escalations (created_at)");
            log.info("Ensured ticket escalation schema exists");
        } catch (Exception ex) {
            log.warn("Ticket escalation schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
