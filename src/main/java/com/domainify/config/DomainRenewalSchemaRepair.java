package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures {@code domain_renewal_reminders} exists with unique dedupe key
 * (domain_id, window_days, expires_at).
 */
@Component
@Order(65)
public class DomainRenewalSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainRenewalSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public DomainRenewalSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS domain_renewal_reminders (
                      id BIGSERIAL PRIMARY KEY,
                      domain_id BIGINT NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
                      window_days INTEGER NOT NULL,
                      expires_at DATE NOT NULL,
                      channels_sent VARCHAR(64) NOT NULL,
                      sent_at TIMESTAMPTZ NOT NULL,
                      CONSTRAINT uk_domain_renewal_reminders_domain_window_expires
                        UNIQUE (domain_id, window_days, expires_at)
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_domain_renewal_reminders_domain
                      ON domain_renewal_reminders (domain_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_domain_renewal_reminders_sent_at
                      ON domain_renewal_reminders (sent_at)
                    """);
            log.info("Ensured domain_renewal_reminders table");
        } catch (Exception ex) {
            log.warn("Domain renewal schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
