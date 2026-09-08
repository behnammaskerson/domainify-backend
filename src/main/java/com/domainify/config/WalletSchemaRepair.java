package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(81)
public class WalletSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WalletSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public WalletSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS wallets (
                      id BIGSERIAL PRIMARY KEY,
                      user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                      available_balance NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      held_balance NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      version BIGINT NOT NULL DEFAULT 0,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS wallet_ledger_entries (
                      id BIGSERIAL PRIMARY KEY,
                      wallet_id BIGINT NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
                      direction VARCHAR(16) NOT NULL,
                      amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      available_after NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      held_after NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      entry_type VARCHAR(32) NOT NULL,
                      ref_type VARCHAR(64),
                      ref_id BIGINT,
                      payment_intent_id BIGINT,
                      actor_id BIGINT,
                      note VARCHAR(500) NOT NULL DEFAULT '',
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_wle_wallet_created
                      ON wallet_ledger_entries (wallet_id, created_at DESC)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_wle_payment_intent
                      ON wallet_ledger_entries (payment_intent_id)
                    """);
            log.info("Ensured wallets and wallet_ledger_entries tables");
        } catch (Exception ex) {
            log.warn("Wallet schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
