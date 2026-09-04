package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds users.email_notifications_enabled for ticket email alert opt-in (default on).
 */
@Component
public class EmailNotificationsSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationsSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public EmailNotificationsSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN DEFAULT true");
            jdbcTemplate.execute("""
                    UPDATE users
                    SET email_notifications_enabled = true
                    WHERE email_notifications_enabled IS NULL
                    """);
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN email_notifications_enabled SET DEFAULT true");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN email_notifications_enabled SET NOT NULL");
        } catch (Exception ex) {
            log.warn("Email notifications preference schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
