package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Ensures payment notification boolean columns exist and are NOT NULL before Hibernate
 * ddl-auto runs. Adding {@code boolean not null} without a default fails on existing rows.
 */
@Configuration
public class PaymentNotificationsEarlySchemaConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationsEarlySchemaConfiguration.class);

    @Bean
    PaymentNotificationsSchemaBootstrap paymentNotificationsSchemaBootstrap(DataSource dataSource) {
        PaymentNotificationsSchemaBootstrap bootstrap =
                new PaymentNotificationsSchemaBootstrap(new JdbcTemplate(dataSource));
        bootstrap.ensureColumns();
        return bootstrap;
    }

    /** Depends on bootstrap so columns exist before Hibernate schema update. */
    @Bean
    EntityManagerFactoryBuilderCustomizer paymentNotificationsSchemaBeforeHibernate(
            PaymentNotificationsSchemaBootstrap paymentNotificationsSchemaBootstrap) {
        return builder -> {
            // no-op; bean dependency is the ordering mechanism
        };
    }

    static final class PaymentNotificationsSchemaBootstrap {
        private final JdbcTemplate jdbcTemplate;

        PaymentNotificationsSchemaBootstrap(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        void ensureColumns() {
            try {
                if (tableExists("payment_settings")) {
                    repairBooleanColumn("payment_settings", "payment_notifications_enabled", true);
                    repairBooleanColumn("payment_settings", "payment_in_app_notifications_enabled", true);
                    repairBooleanColumn("payment_settings", "payment_email_notifications_enabled", true);
                    repairBooleanColumn("payment_settings", "payment_sms_notifications_enabled", true);
                }
                if (tableExists("users")) {
                    repairBooleanColumn("users", "payment_notifications_enabled", true);
                }
                log.info("Ensured payment notification columns (pre-Hibernate)");
            } catch (Exception ex) {
                log.warn("Payment notification early schema repair skipped or partially applied: {}",
                        ex.getMessage());
            }
        }

        private void repairBooleanColumn(String table, String column, boolean defaultValue) {
            String sqlDefault = defaultValue ? "true" : "false";
            String javaDefault = defaultValue ? "TRUE" : "FALSE";
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column
                            + " BOOLEAN DEFAULT " + sqlDefault);
            jdbcTemplate.update(
                    "UPDATE " + table + " SET " + column + " = " + javaDefault
                            + " WHERE " + column + " IS NULL");
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET DEFAULT " + sqlDefault);
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ALTER COLUMN " + column + " SET NOT NULL");
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
