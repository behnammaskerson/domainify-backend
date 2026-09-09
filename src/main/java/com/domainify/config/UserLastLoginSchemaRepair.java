package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds users.last_login_at for ticket customer recent-activity context. */
@Component
@Order(50)
public class UserLastLoginSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserLastLoginSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public UserLastLoginSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE");
        } catch (Exception ex) {
            log.warn("User last_login_at schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
