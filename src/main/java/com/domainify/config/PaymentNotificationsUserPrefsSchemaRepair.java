package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds users.payment_notifications_enabled for payment/wallet alert opt-in (default on).
 */
@Component
@Order(48)
public class PaymentNotificationsUserPrefsSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationsUserPrefsSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public PaymentNotificationsUserPrefsSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS payment_notifications_enabled BOOLEAN DEFAULT true");
            jdbcTemplate.execute("""
                    UPDATE users
                    SET payment_notifications_enabled = true
                    WHERE payment_notifications_enabled IS NULL
                    """);
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN payment_notifications_enabled SET DEFAULT true");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN payment_notifications_enabled SET NOT NULL");
            log.info("Payment notifications user preference schema repair applied");
        } catch (Exception ex) {
            log.warn("Payment notifications user preference schema repair skipped or partially applied: {}",
                    ex.getMessage());
        }
    }
}
