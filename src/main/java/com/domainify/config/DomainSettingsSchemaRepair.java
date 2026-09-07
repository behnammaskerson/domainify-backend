package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures {@code domain_settings} singleton table and channel/time columns exist.
 */
@Component
@Order(66)
public class DomainSettingsSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainSettingsSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public DomainSettingsSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS domain_settings (
                      id BIGINT PRIMARY KEY,
                      renewal_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      renewal_in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      renewal_email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      renewal_sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      renewal_windows VARCHAR(64) NOT NULL DEFAULT '90,60,30',
                      renewal_send_hour INTEGER NOT NULL DEFAULT 9,
                      renewal_send_minute INTEGER NOT NULL DEFAULT 0,
                      renewal_last_run_date DATE,
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domain_settings
                      ADD COLUMN IF NOT EXISTS renewal_in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domain_settings
                      ADD COLUMN IF NOT EXISTS renewal_email_enabled BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domain_settings
                      ADD COLUMN IF NOT EXISTS renewal_sms_enabled BOOLEAN NOT NULL DEFAULT TRUE
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domain_settings
                      ADD COLUMN IF NOT EXISTS renewal_send_minute INTEGER NOT NULL DEFAULT 0
                    """);
            log.info("Ensured domain_settings table");
        } catch (Exception ex) {
            log.warn("Domain settings schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
