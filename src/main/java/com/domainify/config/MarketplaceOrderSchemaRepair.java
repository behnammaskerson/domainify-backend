package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(85)
public class MarketplaceOrderSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceOrderSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public MarketplaceOrderSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS marketplace_orders (
                      id BIGSERIAL PRIMARY KEY,
                      listing_id BIGINT NOT NULL REFERENCES domain_listings(id) ON DELETE RESTRICT,
                      buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      offer_id BIGINT REFERENCES domain_listing_offers(id) ON DELETE SET NULL,
                      gross_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      commission_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      seller_net NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
                      payment_method VARCHAR(20),
                      payment_intent_id BIGINT,
                      paid_at TIMESTAMPTZ,
                      released_at TIMESTAMPTZ,
                      cancelled_at TIMESTAMPTZ,
                      payment_deadline TIMESTAMPTZ,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_mo_buyer
                      ON marketplace_orders (buyer_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_mo_seller
                      ON marketplace_orders (seller_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_mo_status
                      ON marketplace_orders (status)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_mo_listing
                      ON marketplace_orders (listing_id)
                    """);
            log.info("Ensured marketplace_orders table");
        } catch (Exception ex) {
            log.warn("Marketplace order schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
