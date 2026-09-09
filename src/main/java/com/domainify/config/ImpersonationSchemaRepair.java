package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates impersonation_audits for admin "view as customer" sessions. */
@Component
@Order(53)
public class ImpersonationSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImpersonationSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public ImpersonationSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS impersonation_audits (
                        id BIGSERIAL PRIMARY KEY,
                        admin_id BIGINT NOT NULL REFERENCES users(id),
                        target_id BIGINT NOT NULL REFERENCES users(id),
                        note VARCHAR(500),
                        started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        ended_at TIMESTAMPTZ
                    )
                    """);
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_impersonation_audits_admin ON impersonation_audits (admin_id)");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_impersonation_audits_target ON impersonation_audits (target_id)");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_impersonation_audits_started ON impersonation_audits (started_at)");
            log.info("Ensured impersonation_audits table exists");
        } catch (Exception ex) {
            log.warn("Impersonation schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
