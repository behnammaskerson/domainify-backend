package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds users.preferred_language for email/SMS/in-app notification locale (default en).
 */
@Component
@Order(49)
public class PreferredLanguageSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PreferredLanguageSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public PreferredLanguageSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(5) DEFAULT 'en'");
            jdbcTemplate.execute("""
                    UPDATE users
                    SET preferred_language = 'en'
                    WHERE preferred_language IS NULL OR TRIM(preferred_language) = ''
                    """);
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN preferred_language SET DEFAULT 'en'");
        } catch (Exception ex) {
            log.warn("Preferred language schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
