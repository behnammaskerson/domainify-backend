package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Postgres full-text / trigram support for ticket search (subject, description,
 * message body, public number, customer email).
 */
@Component
@Order(56)
public class TicketFullTextSearchSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketFullTextSearchSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public TicketFullTextSearchSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean trgm = ensurePgTrgm();
            jdbcTemplate.execute("""
                    CREATE OR REPLACE FUNCTION ticket_text_search_match(
                        p_ticket_id bigint,
                        p_query text,
                        p_include_internal boolean DEFAULT true
                    )
                    RETURNS boolean
                    LANGUAGE plpgsql
                    STABLE
                    AS $fn$
                    DECLARE
                        q text := trim(both FROM coalesce(p_query, ''));
                        like_pat text;
                        ts_query tsquery;
                    BEGIN
                        IF q = '' THEN
                            RETURN false;
                        END IF;
                        IF char_length(q) > 200 THEN
                            q := left(q, 200);
                        END IF;
                        like_pat := '%' || replace(replace(replace(q, '\\', '\\\\'), '%', '\\%'), '_', '\\_') || '%';
                        BEGIN
                            ts_query := plainto_tsquery('simple', q);
                        EXCEPTION WHEN others THEN
                            ts_query := NULL;
                        END;

                        RETURN EXISTS (
                            SELECT 1
                            FROM tickets t
                            LEFT JOIN users u ON u.id = t.requester_id
                            WHERE t.id = p_ticket_id
                              AND (
                                    cast(t.id as text) = q
                                 OR t.public_number ILIKE like_pat ESCAPE '\\'
                                 OR coalesce(u.email, '') ILIKE like_pat ESCAPE '\\'
                                 OR coalesce(u.first_name, '') ILIKE like_pat ESCAPE '\\'
                                 OR coalesce(u.last_name, '') ILIKE like_pat ESCAPE '\\'
                                 OR t.subject ILIKE like_pat ESCAPE '\\'
                                 OR t.description ILIKE like_pat ESCAPE '\\'
                                 OR (
                                        ts_query IS NOT NULL
                                    AND ts_query <> ''::tsquery
                                    AND to_tsvector(
                                            'simple',
                                            coalesce(t.subject, '') || ' ' || coalesce(t.description, '')
                                        ) @@ ts_query
                                 )
                                 OR EXISTS (
                                        SELECT 1
                                        FROM ticket_messages m
                                        WHERE m.ticket_id = t.id
                                          AND m.deleted_at IS NULL
                                          AND (p_include_internal OR m.internal_note = false)
                                          AND (
                                                m.body ILIKE like_pat ESCAPE '\\'
                                             OR (
                                                    ts_query IS NOT NULL
                                                AND ts_query <> ''::tsquery
                                                AND to_tsvector('simple', coalesce(m.body, '')) @@ ts_query
                                             )
                                          )
                                 )
                              )
                        );
                    END;
                    $fn$
                    """);

            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tickets_subject_desc_fts
                    ON tickets
                    USING gin (
                        to_tsvector(
                            'simple',
                            coalesce(subject, '') || ' ' || coalesce(description, '')
                        )
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_ticket_messages_body_fts
                    ON ticket_messages
                    USING gin (to_tsvector('simple', coalesce(body, '')))
                    """);

            if (trgm) {
                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_tickets_public_number_trgm
                        ON tickets USING gin (public_number gin_trgm_ops)
                        """);
                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_tickets_subject_trgm
                        ON tickets USING gin (subject gin_trgm_ops)
                        """);
                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_users_email_trgm
                        ON users USING gin (email gin_trgm_ops)
                        """);
                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_ticket_messages_body_trgm
                        ON ticket_messages USING gin (body gin_trgm_ops)
                        """);
            }

            log.info("Ensured ticket full-text search function and indexes");
        } catch (Exception ex) {
            log.warn("Ticket full-text search schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }

    private boolean ensurePgTrgm() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            return true;
        } catch (Exception ex) {
            log.warn("pg_trgm extension unavailable (ILIKE indexes skipped): {}", ex.getMessage());
            return false;
        }
    }
}
