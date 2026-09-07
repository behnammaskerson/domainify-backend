package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(70)
public class DomainListingSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainListingSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public DomainListingSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS domain_listings (
                      id BIGSERIAL PRIMARY KEY,
                      seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      domain_id BIGINT NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                      asking_price NUMERIC(14, 2) NOT NULL DEFAULT 0,
                      description VARCHAR(2000) NOT NULL DEFAULT '',
                      status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                      featured BOOLEAN NOT NULL DEFAULT FALSE,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      CONSTRAINT uk_domain_listings_domain UNIQUE (domain_id)
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_domain_listings_seller ON domain_listings (seller_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_domain_listings_status ON domain_listings (status)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_domain_listings_featured_status
                      ON domain_listings (featured, status)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_domain_listings_created ON domain_listings (created_at)
                    """);
            log.info("Ensured domain_listings table");
        } catch (Exception ex) {
            log.warn("Domain listing schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
