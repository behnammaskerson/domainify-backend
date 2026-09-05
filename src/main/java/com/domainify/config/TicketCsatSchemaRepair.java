package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates ticket_csat table for post-resolve customer satisfaction ratings.
 */
@Component
@Order(55)
public class TicketCsatSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketCsatSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketCsatSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_csat (
                        id BIGSERIAL PRIMARY KEY,
                        ticket_id BIGINT NOT NULL REFERENCES tickets(id),
                        rater_id BIGINT NOT NULL REFERENCES users(id),
                        score SMALLINT NOT NULL,
                        comment VARCHAR(1000),
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        CONSTRAINT uk_ticket_csat_ticket UNIQUE (ticket_id),
                        CONSTRAINT chk_ticket_csat_score CHECK (score >= 1 AND score <= 5)
                    )
                    """);
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_csat_ticket ON ticket_csat (ticket_id)");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_csat_created ON ticket_csat (created_at)");
            log.info("Ensured ticket_csat schema exists");
        } catch (Exception ex) {
            log.warn("Ticket CSAT schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
