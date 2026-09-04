package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds category SMS toggle and priority lists for email/SMS ticket alerts.
 */
@Component
@Order(48)
public class TicketNotificationPrioritySchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketNotificationPrioritySchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketNotificationPrioritySchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            repairBooleanColumn("ticket_categories", "sms_notifications_enabled", true);
            repairVarcharColumn(
                    "ticket_settings",
                    "email_notification_priorities",
                    64,
                    "LOW,MEDIUM,HIGH,URGENT");
            repairVarcharColumn(
                    "ticket_settings",
                    "sms_notification_priorities",
                    64,
                    "URGENT");
            log.info("Ticket notification priority schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket notification priority schema repair skipped: {}", ex.getMessage());
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

    private void repairVarcharColumn(String table, String column, int length, String defaultValue) {
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column
                        + " VARCHAR(" + length + ")");
        jdbcTemplate.update(
                "UPDATE " + table + " SET " + column + " = ? WHERE " + column + " IS NULL OR TRIM(" + column + ") = ''",
                defaultValue);
        jdbcTemplate.execute(
                "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET DEFAULT '" + defaultValue + "'");
    }
}
