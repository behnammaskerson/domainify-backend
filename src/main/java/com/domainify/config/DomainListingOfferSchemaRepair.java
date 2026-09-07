package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(71)
public class DomainListingOfferSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainListingOfferSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public DomainListingOfferSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS domain_listing_offers (
                      id BIGSERIAL PRIMARY KEY,
                      listing_id BIGINT NOT NULL REFERENCES domain_listings(id) ON DELETE CASCADE,
                      buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      message VARCHAR(1000) NOT NULL DEFAULT '',
                      status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      responded_at TIMESTAMPTZ
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_dlo_listing_status
                      ON domain_listing_offers (listing_id, status)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_dlo_buyer_created
                      ON domain_listing_offers (buyer_id, created_at)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_dlo_seller_status
                      ON domain_listing_offers (seller_id, status)
                    """);

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS domain_listing_offer_events (
                      id BIGSERIAL PRIMARY KEY,
                      offer_id BIGINT NOT NULL REFERENCES domain_listing_offers(id) ON DELETE CASCADE,
                      actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                      action VARCHAR(20) NOT NULL,
                      amount NUMERIC(14, 2),
                      message VARCHAR(1000) NOT NULL DEFAULT '',
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_dloe_offer_created
                      ON domain_listing_offer_events (offer_id, created_at)
                    """);
            log.info("Ensured domain_listing_offers and domain_listing_offer_events tables");
        } catch (Exception ex) {
            log.warn("Domain listing offer schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
