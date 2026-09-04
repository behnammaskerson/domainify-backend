package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps in_app_notifications.type check constraint in sync with {@link com.domainify.entity.NotificationType}.
 * Hibernate creates the CHECK from enum values at first create, but does not widen it when new values are added.
 */
@Component
@Order(40)
public class NotificationTypeSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationTypeSchemaRepair.class);

    private static final String TYPE_CHECK =
            "type IN ("
                    + "'TICKET_CREATED',"
                    + "'TICKET_CUSTOMER_REPLY',"
                    + "'TICKET_STAFF_REPLY',"
                    + "'TICKET_MENTION',"
                    + "'TICKET_STATUS_CHANGED',"
                    + "'TICKET_ASSIGNED',"
                    + "'TICKET_UNASSIGNED',"
                    + "'TICKET_CLOSED',"
                    + "'TICKET_REOPENED',"
                    + "'TICKET_WATCHER_ADDED',"
                    + "'TICKET_TRANSFERRED'"
                    + ")";

    private final JdbcTemplate jdbcTemplate;

    public NotificationTypeSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE in_app_notifications DROP CONSTRAINT IF EXISTS in_app_notifications_type_check"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE in_app_notifications "
                            + "ADD CONSTRAINT in_app_notifications_type_check CHECK (" + TYPE_CHECK + ")"
            );
            log.info("Updated in_app_notifications.type check constraint");
        } catch (Exception ex) {
            log.warn("Notification type schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }
}
