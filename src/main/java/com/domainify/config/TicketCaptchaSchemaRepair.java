package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds CAPTCHA settings column to ticket_settings table.
 */
@Component
@Order(93)
public class TicketCaptchaSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketCaptchaSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketCaptchaSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS captcha_settings_json TEXT
                    """);

            log.info("Ticket CAPTCHA schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket CAPTCHA schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}