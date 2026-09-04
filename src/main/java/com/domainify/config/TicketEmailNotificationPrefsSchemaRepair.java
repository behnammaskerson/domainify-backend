package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds global + per-category ticket email notification toggles.
 * Columns are added nullable (with DEFAULT) so Hibernate ddl-auto can succeed on existing rows;
 * this runner backfills nulls and enforces NOT NULL afterward.
 */
@Component
@Order(46)
public class TicketEmailNotificationPrefsSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketEmailNotificationPrefsSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketEmailNotificationPrefsSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            repairBooleanColumn(
                    "ticket_settings",
                    "ticket_email_notifications_enabled");
            repairBooleanColumn(
                    "ticket_categories",
                    "email_notifications_enabled");
            log.info("Ticket email notification preference schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket email notification preference schema repair skipped: {}", ex.getMessage());
        }
    }

    private void repairBooleanColumn(String table, String column) {
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " BOOLEAN DEFAULT true");
        jdbcTemplate.update(
                "UPDATE " + table + " SET " + column + " = TRUE WHERE " + column + " IS NULL");
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET DEFAULT true");
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET NOT NULL");
    }
}
