package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Idempotent post-start repair for payment_settings (table seed + notification columns).
 * Notification columns are also ensured pre-Hibernate by
 * {@link PaymentNotificationsEarlySchemaConfiguration}.
 */
@Component
@Order(80)
public class PaymentSettingsSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentSettingsSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public PaymentSettingsSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS payment_settings (
                      id BIGINT PRIMARY KEY,
                      merchant_id VARCHAR(128) NOT NULL DEFAULT '',
                      sandbox BOOLEAN NOT NULL DEFAULT TRUE,
                      access_token VARCHAR(1024) NOT NULL DEFAULT '',
                      enabled BOOLEAN NOT NULL DEFAULT FALSE,
                      commission_percent NUMERIC(7, 2) NOT NULL DEFAULT 5.00,
                      min_top_up_irt BIGINT NOT NULL DEFAULT 10000,
                      featured_listing_price_irt BIGINT NOT NULL DEFAULT 50000,
                      escrow_hold_days INTEGER NOT NULL DEFAULT 3,
                      callback_public_base_url VARCHAR(512) NOT NULL DEFAULT '',
                      payment_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      payment_in_app_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      payment_email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      payment_sms_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.update("""
                    INSERT INTO payment_settings (id)
                    VALUES (1)
                    ON CONFLICT (id) DO NOTHING
                    """);
            repairBooleanColumn("payment_notifications_enabled");
            repairBooleanColumn("payment_in_app_notifications_enabled");
            repairBooleanColumn("payment_email_notifications_enabled");
            repairBooleanColumn("payment_sms_notifications_enabled");
            log.info("Ensured payment_settings table");
        } catch (Exception ex) {
            log.warn("Payment settings schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }

    private void repairBooleanColumn(String column) {
        jdbcTemplate.execute(
                "ALTER TABLE payment_settings ADD COLUMN IF NOT EXISTS " + column
                        + " BOOLEAN DEFAULT true");
        jdbcTemplate.update(
                "UPDATE payment_settings SET " + column + " = TRUE WHERE " + column + " IS NULL");
        jdbcTemplate.execute(
                "ALTER TABLE payment_settings ALTER COLUMN " + column + " SET DEFAULT true");
        jdbcTemplate.execute(
                "ALTER TABLE payment_settings ALTER COLUMN " + column + " SET NOT NULL");
    }
}
