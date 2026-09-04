package com.domainify.service;

import com.domainify.exception.ErrorCode;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class MessageService {

    private final MessageSource messageSource;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(ErrorCode code) {
        return get(code, null);
    }

    public String get(ErrorCode code, Object[] args) {
        return messageSource.getMessage(
                "error." + code.name().toLowerCase(),
                args,
                LocaleContextHolder.getLocale());
    }

    public String get(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    public String get(String key, Object[] args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    public String get(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale != null ? locale : Locale.ENGLISH);
    }

    public String get(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, key, locale != null ? locale : Locale.ENGLISH);
    }
}
