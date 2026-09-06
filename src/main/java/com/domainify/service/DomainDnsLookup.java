package com.domainify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

@Component
public class DomainDnsLookup {

    private static final Logger log = LoggerFactory.getLogger(DomainDnsLookup.class);

    public List<String> lookupTxt(String fqdn) {
        List<String> values = new ArrayList<>();
        if (!StringUtils.hasText(fqdn)) {
            return values;
        }
        String name = fqdn.trim().toLowerCase(Locale.ROOT);
        if (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(name, new String[]{"TXT"});
            Attribute txt = attrs != null ? attrs.get("TXT") : null;
            if (txt == null) {
                return values;
            }
            NamingEnumeration<?> all = txt.getAll();
            while (all.hasMore()) {
                Object next = all.next();
                if (next == null) {
                    continue;
                }
                String raw = next.toString();
                // JNDI often returns quoted chunks; join/strip quotes.
                String cleaned = raw.replace("\"", "").trim();
                if (StringUtils.hasText(cleaned)) {
                    values.add(cleaned);
                }
            }
        } catch (Exception ex) {
            log.debug("TXT lookup failed for {}: {}", name, ex.getMessage());
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
        return values;
    }
}
