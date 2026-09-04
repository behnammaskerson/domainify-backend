package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds user opt-in + global ticket SMS notification columns.
 * Columns are added nullable with DEFAULT so Hibernate ddl-auto succeeds on existing rows.
 */
@Component
@Order(47)
public class TicketSmsNotificationSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketSmsNotificationSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketSmsNotificationSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Opt-in: default false
            repairBooleanColumn("users", "sms_notifications_enabled", false);
            // Global master switch: default true
            repairBooleanColumn("ticket_settings", "ticket_sms_notifications_enabled", true);
            log.info("Ticket SMS notification schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket SMS notification schema repair skipped: {}", ex.getMessage());
        }
    }

    private void repairBooleanColumn(String table, String column, boolean defaultValue) {
        String sqlDefault = defaultValue ? "true" : "false";
        String javaDefault = defaultValue ? "TRUE" : "FALSE";
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column
                        + " BOOLEAN DEFAULT " + sqlDefault);
        jdbcTemplate.update(
                "UPDATE " + table + " SET " + column + " = " + javaDefault + " WHERE " + column + " IS NULL");
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET DEFAULT " + sqlDefault);
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET NOT NULL");
    }
}
