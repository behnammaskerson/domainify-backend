package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds auto-assign columns on ticket_settings and the agent-category skills table.
 */
@Component
@Order(45)
public class TicketAutoAssignSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketAutoAssignSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketAutoAssignSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE ticket_settings ADD COLUMN IF NOT EXISTS auto_assign_mode VARCHAR(32)"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE ticket_settings ADD COLUMN IF NOT EXISTS auto_assign_fallback_round_robin BOOLEAN"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE ticket_settings ADD COLUMN IF NOT EXISTS round_robin_last_user_id BIGINT"
            );
            jdbcTemplate.update(
                    "UPDATE ticket_settings SET auto_assign_mode = 'OFF' "
                            + "WHERE auto_assign_mode IS NULL OR auto_assign_mode = ''"
            );
            jdbcTemplate.update(
                    "UPDATE ticket_settings SET auto_assign_fallback_round_robin = TRUE "
                            + "WHERE auto_assign_fallback_round_robin IS NULL"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE ticket_settings ALTER COLUMN auto_assign_mode SET DEFAULT 'OFF'"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE ticket_settings ALTER COLUMN auto_assign_fallback_round_robin SET DEFAULT TRUE"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE ticket_settings ALTER COLUMN auto_assign_mode SET NOT NULL"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE ticket_settings ALTER COLUMN auto_assign_fallback_round_robin SET NOT NULL"
            );

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS ticket_agent_category_skills (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        category_id BIGINT NOT NULL,
                        CONSTRAINT uk_ticket_agent_category_skill UNIQUE (user_id, category_id)
                    )
                    """);

            log.info("Ticket auto-assign schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket auto-assign schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
