package com.domainify.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class KnowledgeBaseSchemaRepair {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseSchemaRepair.class);

    @Bean
    @Order(110)
    public ApplicationRunner knowledgeBaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                log.info("Applying knowledge base schema repair...");

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS kb_categories (
                          id BIGSERIAL PRIMARY KEY,
                          code VARCHAR(64) NOT NULL UNIQUE,
                          name VARCHAR(120) NOT NULL,
                          description VARCHAR(500),
                          parent_id BIGINT REFERENCES kb_categories(id),
                          active BOOLEAN NOT NULL DEFAULT true,
                          sort_order INTEGER NOT NULL DEFAULT 0,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                jdbcTemplate.execute("ALTER TABLE kb_categories ADD COLUMN IF NOT EXISTS parent_id BIGINT");
                try {
                    jdbcTemplate.execute("""
                            ALTER TABLE kb_categories
                              ADD CONSTRAINT fk_kb_categories_parent
                              FOREIGN KEY (parent_id) REFERENCES kb_categories(id)
                            """);
                } catch (Exception ignored) {
                    // Constraint may already exist
                }

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS kb_articles (
                          id BIGSERIAL PRIMARY KEY,
                          slug VARCHAR(160) NOT NULL UNIQUE,
                          title VARCHAR(200) NOT NULL,
                          summary VARCHAR(500),
                          body TEXT NOT NULL,
                          locale VARCHAR(8) NOT NULL DEFAULT 'en',
                          category_id BIGINT NOT NULL REFERENCES kb_categories(id),
                          published BOOLEAN NOT NULL DEFAULT false,
                          sort_order INTEGER NOT NULL DEFAULT 0,
                          published_at TIMESTAMPTZ,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_kb_categories_active
                          ON kb_categories (active, sort_order)
                        """);
                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_kb_categories_parent
                          ON kb_categories (parent_id)
                        """);
                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_kb_articles_published
                          ON kb_articles (published, sort_order)
                        """);
                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_kb_articles_category
                          ON kb_articles (category_id)
                        """);
                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_kb_articles_locale
                          ON kb_articles (locale)
                        """);

                log.info("Knowledge base schema repair applied");
            } catch (Exception ex) {
                log.error("Knowledge base schema repair failed: {}", ex.getMessage(), ex);
                throw ex;
            }
        };
    }
}
