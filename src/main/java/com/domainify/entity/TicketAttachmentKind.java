package com.domainify.entity;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Configurable attachment categories for ticket uploads (images, PDF, logs, documents).
 */
public enum TicketAttachmentKind {
    IMAGE(
            Set.of("image/jpeg", "image/png", "image/webp"),
            Set.of(".jpg", ".jpeg", ".png", ".webp")
    ),
    PDF(
            Set.of("application/pdf"),
            Set.of(".pdf")
    ),
    LOG(
            Set.of("text/plain", "text/x-log"),
            Set.of(".txt", ".log")
    ),
    DOCUMENT(
            Set.of(
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ),
            Set.of(".doc", ".docx")
    );

    private final Set<String> contentTypes;
    private final Set<String> extensions;

    TicketAttachmentKind(Set<String> contentTypes, Set<String> extensions) {
        this.contentTypes = contentTypes;
        this.extensions = extensions;
    }

    public Set<String> getContentTypes() {
        return contentTypes;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public static Optional<TicketAttachmentKind> fromToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(TicketAttachmentKind.valueOf(token.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static Set<TicketAttachmentKind> parseCsv(String csv) {
        Set<TicketAttachmentKind> kinds = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return kinds;
        }
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(TicketAttachmentKind::fromToken)
                .flatMap(Optional::stream)
                .forEach(kinds::add);
        return kinds;
    }

    public static String toCsv(Set<TicketAttachmentKind> kinds) {
        if (kinds == null || kinds.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (TicketAttachmentKind kind : kinds) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(kind.name());
        }
        return sb.toString();
    }
}
