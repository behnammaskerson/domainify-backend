package com.domainify.config;

import com.domainify.entity.TicketSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds business-hours SLA columns to ticket_settings.
 */
@Component
@Order(88)
public class TicketBusinessHoursSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketBusinessHoursSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketBusinessHoursSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS sla_use_business_hours BOOLEAN DEFAULT false
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET sla_use_business_hours = false
                     WHERE sla_use_business_hours IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS sla_timezone VARCHAR(64) DEFAULT 'UTC'
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET sla_timezone = 'UTC'
                     WHERE sla_timezone IS NULL OR TRIM(sla_timezone) = ''
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS business_hours_json TEXT
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET business_hours_json = ?
                     WHERE business_hours_json IS NULL OR TRIM(business_hours_json) = ''
                    """, TicketSettings.DEFAULT_BUSINESS_HOURS_JSON);

            jdbcTemplate.execute("""
                    ALTER TABLE ticket_settings
                      ADD COLUMN IF NOT EXISTS business_holidays_json TEXT
                    """);
            jdbcTemplate.update("""
                    UPDATE ticket_settings
                       SET business_holidays_json = ?
                     WHERE business_holidays_json IS NULL OR TRIM(business_holidays_json) = ''
                    """, TicketSettings.DEFAULT_BUSINESS_HOLIDAYS_JSON);

            log.info("Ticket business hours schema repair applied");
        } catch (Exception ex) {
            log.warn("Ticket business hours schema repair skipped: {}", ex.getMessage());
        }
    }
}
