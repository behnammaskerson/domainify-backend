package com.domainify.util;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * Supported UI / notification languages (aligned with frontend i18n).
 */
public final class UserPreferredLanguage {

    public static final String DEFAULT = "en";
    public static final Set<String> SUPPORTED = Set.of("en", "fa", "ar", "tr");

    private UserPreferredLanguage() {
    }

    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        int dash = value.indexOf('-');
        if (dash > 0) {
            value = value.substring(0, dash);
        }
        return SUPPORTED.contains(value) ? value : DEFAULT;
    }

    public static Locale toLocale(String raw) {
        return Locale.forLanguageTag(normalize(raw));
    }
}
