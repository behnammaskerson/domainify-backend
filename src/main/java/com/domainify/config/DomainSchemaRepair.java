package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(57)
public class DomainSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public DomainSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS domains (
                        id BIGSERIAL PRIMARY KEY,
                        owner_id BIGINT NOT NULL REFERENCES users(id),
                        name VARCHAR(253) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        category_key VARCHAR(40) NOT NULL,
                        price NUMERIC(14, 2) NOT NULL DEFAULT 0,
                        expires_at DATE,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        CONSTRAINT uk_domains_owner_name UNIQUE (owner_id, name)
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_domains_owner ON domains (owner_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_domains_status ON domains (status)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_domains_expires_at ON domains (expires_at)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_domains_name ON domains (name)");
            log.info("Ensured domains schema exists");
        } catch (Exception ex) {
            log.warn("Domain schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
