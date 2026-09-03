package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds phone_verified columns safely on existing databases before enforcing NOT NULL.
 */
@Component
public class PhoneVerifiedSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PhoneVerifiedSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public PhoneVerifiedSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT true");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMPTZ");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verification_otp_hash VARCHAR(72)");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verification_otp_expires_at TIMESTAMPTZ");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verification_otp_sent_at TIMESTAMPTZ");
            jdbcTemplate.execute("""
                    ALTER TABLE users
                    ADD COLUMN IF NOT EXISTS phone_verification_otp_attempts INTEGER DEFAULT 0
                    """);

            jdbcTemplate.execute("""
                    UPDATE users
                    SET phone_verified = true
                    WHERE phone_verified IS NULL
                    """);

            jdbcTemplate.execute("""
                    UPDATE users
                    SET phone_verified = false
                    WHERE phone_country_code IS NOT NULL
                      AND phone_number IS NOT NULL
                      AND phone_verified_at IS NULL
                      AND create_method = 'REGISTER'
                    """);

            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN phone_verified SET DEFAULT true");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN phone_verified SET NOT NULL");
        } catch (Exception ex) {
            log.warn("Phone verification schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
