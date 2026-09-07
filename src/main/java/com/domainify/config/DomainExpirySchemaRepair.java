package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Safety net: backfill expires_source after startup if Hibernate left nulls.
 */
@Component
@Order(60)
public class DomainExpirySchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainExpirySchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public DomainExpirySchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS expires_source VARCHAR(20)
                    """);
            jdbcTemplate.execute("""
                    UPDATE domains
                    SET expires_source = 'MANUAL'
                    WHERE expires_source IS NULL
                    """);
            jdbcTemplate.execute("ALTER TABLE domains ALTER COLUMN expires_source SET DEFAULT 'MANUAL'");
            jdbcTemplate.execute("ALTER TABLE domains ALTER COLUMN expires_source SET NOT NULL");

            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS expires_checked_at TIMESTAMPTZ
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS expires_registrar VARCHAR(255)
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS renewal_windows VARCHAR(64)
                    """);
            log.info("Ensured domain expiry source columns");
        } catch (Exception ex) {
            log.warn("Domain expiry schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
