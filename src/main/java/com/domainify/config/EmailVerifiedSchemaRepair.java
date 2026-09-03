package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds email_verified columns safely on existing databases before enforcing NOT NULL.
 */
@Component
public class EmailVerifiedSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmailVerifiedSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public EmailVerifiedSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT true");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ");

            jdbcTemplate.execute("""
                    UPDATE users
                    SET email_verified = true
                    WHERE email_verified IS NULL
                    """);

            jdbcTemplate.execute("""
                    UPDATE users
                    SET email_verified = false
                    WHERE create_method = 'REGISTER'
                      AND email_verified_at IS NULL
                    """);

            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT true");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL");
        } catch (Exception ex) {
            log.warn("Email verification schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
