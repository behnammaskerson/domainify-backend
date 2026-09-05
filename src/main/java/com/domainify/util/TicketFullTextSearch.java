package com.domainify.util;

import com.domainify.entity.Ticket;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.util.StringUtils;

/**
 * Criteria helper for Postgres {@code ticket_text_search_match}.
 */
public final class TicketFullTextSearch {

    private static final int MAX_QUERY_LENGTH = 200;

    private TicketFullTextSearch() {
    }

    public static Predicate matches(
            Root<Ticket> root,
            CriteriaBuilder cb,
            String rawQuery,
            boolean includeInternalNotes) {
        String term = normalize(rawQuery);
        if (term == null) {
            return cb.conjunction();
        }
        return cb.isTrue(cb.function(
                "ticket_text_search_match",
                Boolean.class,
                root.get("id"),
                cb.literal(term),
                cb.literal(includeInternalNotes)
        ));
    }

    public static String normalize(String rawQuery) {
        if (!StringUtils.hasText(rawQuery)) {
            return null;
        }
        String term = rawQuery.trim();
        if (term.isEmpty()) {
            return null;
        }
        if (term.length() > MAX_QUERY_LENGTH) {
            return term.substring(0, MAX_QUERY_LENGTH);
        }
        return term;
    }
}
