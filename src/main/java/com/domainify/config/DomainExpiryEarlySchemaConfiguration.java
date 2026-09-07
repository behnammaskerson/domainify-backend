package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Applies expiry columns before Hibernate builds the EntityManagerFactory
 * (via EMF builder customizer dependency), so ddl-auto never tries to ADD a
 * NOT NULL column onto an already-populated table without a default.
 */
@Configuration
public class DomainExpiryEarlySchemaConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DomainExpiryEarlySchemaConfiguration.class);

    @Bean
    DomainExpirySchemaBootstrap domainExpirySchemaBootstrap(DataSource dataSource) {
        DomainExpirySchemaBootstrap bootstrap = new DomainExpirySchemaBootstrap(new JdbcTemplate(dataSource));
        bootstrap.ensureColumns();
        return bootstrap;
    }

    /** Depends on bootstrap so columns exist before Hibernate schema update. */
    @Bean
    EntityManagerFactoryBuilderCustomizer domainExpirySchemaBeforeHibernate(
            DomainExpirySchemaBootstrap domainExpirySchemaBootstrap) {
        return builder -> {
            // no-op; bean dependency is the ordering mechanism
        };
    }

    static final class DomainExpirySchemaBootstrap {
        private final JdbcTemplate jdbcTemplate;

        DomainExpirySchemaBootstrap(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        void ensureColumns() {
            try {
                if (!tableExists("domains")) {
                    return;
                }
                jdbcTemplate.execute("""
                        ALTER TABLE domains
                          ADD COLUMN IF NOT EXISTS expires_source VARCHAR(20)
                        """);
                jdbcTemplate.execute("""
                        UPDATE domains
                        SET expires_source = 'MANUAL'
                        WHERE expires_source IS NULL
                        """);
                jdbcTemplate.execute("ALTER TABLE domains ALTER COLUMN expires_source SET DEFAULT 'MANUAL'");
                jdbcTemplate.execute("ALTER TABLE domains ALTER COLUMN expires_source SET NOT NULL");

                jdbcTemplate.execute("""
                        ALTER TABLE domains
                          ADD COLUMN IF NOT EXISTS expires_checked_at TIMESTAMPTZ
                        """);
                jdbcTemplate.execute("""
                        ALTER TABLE domains
                          ADD COLUMN IF NOT EXISTS expires_registrar VARCHAR(255)
                        """);
                log.info("Ensured domain expiry source columns (pre-Hibernate)");
            } catch (Exception ex) {
                log.warn("Domain expiry early schema repair skipped or partially applied: {}", ex.getMessage());
            }
        }

        private boolean tableExists(String table) {
            Integer count = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(*) FROM information_schema.tables
                            WHERE table_schema = current_schema() AND table_name = ?
                            """,
                    Integer.class,
                    table
            );
            return count != null && count > 0;
        }
    }
}
