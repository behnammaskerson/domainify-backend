package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(82)
public class PaymentIntentSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentIntentSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public PaymentIntentSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS payment_intents (
                      id BIGSERIAL PRIMARY KEY,
                      user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      purpose VARCHAR(32) NOT NULL DEFAULT 'WALLET_TOP_UP',
                      amount_irt BIGINT NOT NULL,
                      authority VARCHAR(64) UNIQUE,
                      status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
                      ref_id BIGINT,
                      fee BIGINT,
                      card_pan VARCHAR(32),
                      verified_at TIMESTAMPTZ,
                      order_id BIGINT,
                      listing_id BIGINT,
                      offer_id BIGINT,
                      description VARCHAR(500) NOT NULL DEFAULT '',
                      gateway_code INTEGER,
                      failure_reason VARCHAR(1000),
                      failed_at TIMESTAMPTZ,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE payment_intents
                      ADD COLUMN IF NOT EXISTS gateway_code INTEGER
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE payment_intents
                      ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(1000)
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE payment_intents
                      ADD COLUMN IF NOT EXISTS failed_at TIMESTAMPTZ
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_pi_user_created
                      ON payment_intents (user_id, created_at DESC)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_pi_status
                      ON payment_intents (status)
                    """);
            log.info("Ensured payment_intents table");
        } catch (Exception ex) {
            log.warn("Payment intent schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
