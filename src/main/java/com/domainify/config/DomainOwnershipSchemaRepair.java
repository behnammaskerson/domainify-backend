package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(59)
public class DomainOwnershipSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainOwnershipSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public DomainOwnershipSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS ownership_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED'
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS ownership_verified_at TIMESTAMPTZ
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS ownership_method VARCHAR(20)
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS ownership_token VARCHAR(64)
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS ownership_token_expires_at TIMESTAMPTZ
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS ownership_last_checked_at TIMESTAMPTZ
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE domains
                      ADD COLUMN IF NOT EXISTS ownership_check_attempts INT NOT NULL DEFAULT 0
                    """);
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_domains_ownership_status ON domains (ownership_status)");
            log.info("Ensured domain ownership verification columns");
        } catch (Exception ex) {
            log.warn("Domain ownership schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
