package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Creates domain_categories tree table and migrates legacy domains.category_key → category_id.
 * Categories are managed via admin UI — no hardcoded niche seed list.
 */
@Component
@Order(58)
public class DomainCategorySchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DomainCategorySchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public DomainCategorySchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS domain_categories (
                        id BIGSERIAL PRIMARY KEY,
                        code VARCHAR(64) NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        parent_id BIGINT REFERENCES domain_categories(id),
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        sort_order INT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        CONSTRAINT uk_domain_categories_code UNIQUE (code)
                    )
                    """);
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_domain_categories_parent ON domain_categories (parent_id)");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_domain_categories_active ON domain_categories (active)");

            ensureCategoryIdColumn();
            migrateCategoryKeys();
            dropLegacyCategoryKey();

            log.info("Ensured domain_categories tree schema and domain category migration");
        } catch (Exception ex) {
            log.warn("Domain category schema repair skipped or partially applied: {}", ex.getMessage());
        }
    }

    private void ensureCategoryIdColumn() {
        Integer hasCategoryId = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'domains' AND column_name = 'category_id'
                """, Integer.class);
        if (hasCategoryId != null && hasCategoryId > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE domains ADD COLUMN IF NOT EXISTS category_id BIGINT");
    }

    private void migrateCategoryKeys() {
        Integer hasCategoryKey = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'domains' AND column_name = 'category_key'
                """, Integer.class);
        if (hasCategoryKey == null || hasCategoryKey == 0) {
            return;
        }

        List<String> keys = jdbcTemplate.query("""
                SELECT DISTINCT lower(trim(category_key)) AS k
                FROM domains
                WHERE category_key IS NOT NULL AND trim(category_key) <> ''
                ORDER BY k
                """, (rs, rowNum) -> rs.getString("k"));

        int order = nextRootSortOrder();
        for (String key : keys) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            String code = normalizeCode(key);
            if (!StringUtils.hasText(code)) {
                continue;
            }
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM domain_categories WHERE lower(code) = lower(?)",
                    Integer.class,
                    code);
            if (exists == null || exists == 0) {
                String name = humanize(code);
                jdbcTemplate.update("""
                        INSERT INTO domain_categories (code, name, parent_id, active, sort_order, created_at, updated_at)
                        VALUES (?, ?, NULL, TRUE, ?, NOW(), NOW())
                        """, code, name, order++);
            }
        }

        jdbcTemplate.update("""
                UPDATE domains d
                SET category_id = c.id
                FROM domain_categories c
                WHERE d.category_id IS NULL
                  AND d.category_key IS NOT NULL
                  AND lower(trim(d.category_key)) = lower(c.code)
                """);

        // Any leftover rows: ensure a fallback category exists from first available, or create from key.
        Integer nullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domains WHERE category_id IS NULL",
                Integer.class);
        if (nullCount != null && nullCount > 0) {
            Long fallbackId = jdbcTemplate.query("""
                    SELECT id FROM domain_categories ORDER BY sort_order ASC, id ASC LIMIT 1
                    """, rs -> rs.next() ? rs.getLong(1) : null);
            if (fallbackId == null) {
                jdbcTemplate.update("""
                        INSERT INTO domain_categories (code, name, parent_id, active, sort_order, created_at, updated_at)
                        VALUES ('general', 'General', NULL, TRUE, 0, NOW(), NOW())
                        """);
                fallbackId = jdbcTemplate.query("""
                        SELECT id FROM domain_categories WHERE lower(code) = 'general' LIMIT 1
                        """, rs -> rs.next() ? rs.getLong(1) : null);
            }
            if (fallbackId != null) {
                jdbcTemplate.update(
                        "UPDATE domains SET category_id = ? WHERE category_id IS NULL",
                        fallbackId);
            }
        }
    }

    private int nextRootSortOrder() {
        Integer max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(sort_order), -1) FROM domain_categories WHERE parent_id IS NULL",
                Integer.class);
        return max == null ? 0 : max + 1;
    }

    private static String normalizeCode(String raw) {
        String code = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (code.length() > 64) {
            code = code.substring(0, 64).replaceAll("-+$", "");
        }
        return code;
    }

    private static String humanize(String code) {
        String[] parts = code.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        String name = sb.toString();
        return StringUtils.hasText(name) ? name : code;
    }

    private void dropLegacyCategoryKey() {
        Integer nullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domains WHERE category_id IS NULL",
                Integer.class);
        if (nullCount != null && nullCount > 0) {
            log.warn("Skipping drop of domains.category_key; {} rows still missing category_id", nullCount);
            return;
        }

        Integer hasCategoryId = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'domains' AND column_name = 'category_id'
                """, Integer.class);
        if (hasCategoryId == null || hasCategoryId == 0) {
            return;
        }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE domains ALTER COLUMN category_id SET NOT NULL");
        } catch (Exception ignored) {
            // may already be NOT NULL
        }
        try {
            jdbcTemplate.execute("""
                    DO $$ BEGIN
                      IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'fk_domains_category'
                      ) THEN
                        ALTER TABLE domains
                          ADD CONSTRAINT fk_domains_category
                          FOREIGN KEY (category_id) REFERENCES domain_categories(id);
                      END IF;
                    END $$;
                    """);
        } catch (Exception ex) {
            log.warn("Could not add fk_domains_category: {}", ex.getMessage());
        }
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_domains_category ON domains (category_id)");

        Integer hasCategoryKey = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = 'domains' AND column_name = 'category_key'
                """, Integer.class);
        if (hasCategoryKey != null && hasCategoryKey > 0) {
            jdbcTemplate.execute("ALTER TABLE domains DROP COLUMN category_key");
        }
    }
}
