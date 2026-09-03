package com.domainify.util;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

/** Formats stored profile phone fields for the SMS provider API. */
public final class PhoneSmsUtil {

    private static final Map<String, String> CALLING_CODES = Map.ofEntries(
            Map.entry("IR", "98"),
            Map.entry("US", "1"),
            Map.entry("CA", "1"),
            Map.entry("GB", "44"),
            Map.entry("TR", "90"),
            Map.entry("AE", "971"),
            Map.entry("SA", "966"),
            Map.entry("DE", "49"),
            Map.entry("FR", "33"),
            Map.entry("IT", "39"),
            Map.entry("ES", "34"),
            Map.entry("NL", "31"),
            Map.entry("IN", "91"),
            Map.entry("PK", "92"),
            Map.entry("IQ", "964"),
            Map.entry("AF", "93")
    );

    private PhoneSmsUtil() {
    }

    public static String toSmsMobile(String countryIso, String nationalDigits) {
        if (!StringUtils.hasText(countryIso) || !StringUtils.hasText(nationalDigits)) {
            return null;
        }

        String country = countryIso.trim().toUpperCase(Locale.ROOT);
        String digits = nationalDigits.replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }

        if ("IR".equals(country)) {
            if (digits.startsWith("98") && digits.length() == 12) {
                digits = digits.substring(2);
            }
            if (digits.startsWith("0") && digits.length() == 11) {
                digits = digits.substring(1);
            }
            return digits.matches("9\\d{9}") ? digits : null;
        }

        String callingCode = CALLING_CODES.get(country);
        if (!StringUtils.hasText(callingCode)) {
            return null;
        }

        while (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.startsWith(callingCode)) {
            return digits;
        }
        return callingCode + digits;
    }
}
