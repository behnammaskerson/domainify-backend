package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Creates the business_rules table if it doesn't exist.
 */
@Configuration
public class BusinessRulesSchemaRepair {

    private static final Logger log = LoggerFactory.getLogger(BusinessRulesSchemaRepair.class);

    @Bean
    @Order(109)
    public ApplicationRunner businessRulesSchemaInitializer(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Applying business rules schema repair...");

                // Create business_rules table
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS business_rules (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        description VARCHAR(500),
                        enabled BOOLEAN NOT NULL DEFAULT true,
                        trigger_event VARCHAR(20) NOT NULL,
                        priority INTEGER NOT NULL DEFAULT 100,
                        conditions TEXT NOT NULL,
                        actions TEXT NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        last_executed_at TIMESTAMPTZ,
                        execution_count BIGINT NOT NULL DEFAULT 0
                    )
                    """);

                // Create indexes for better performance
                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_business_rules_trigger_enabled 
                    ON business_rules (trigger_event, enabled, priority)
                    """);

                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_business_rules_enabled_priority 
                    ON business_rules (enabled, priority)
                    """);

                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_business_rules_name 
                    ON business_rules (name)
                    """);

                log.info("Business rules schema repair applied successfully");
            } catch (Exception ex) {
                log.error("Business rules schema repair failed: {}", ex.getMessage(), ex);
                throw ex;
            }
        };
    }
}