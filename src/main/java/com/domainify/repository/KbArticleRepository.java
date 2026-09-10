package com.domainify.repository;

import com.domainify.entity.KbArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

    Optional<KbArticle> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    long countByCategoryId(Long categoryId);

    List<KbArticle> findByCategoryIdOrderBySortOrderAscTitleAsc(Long categoryId);

    @Query("""
            select a from KbArticle a
            join fetch a.category c
            where a.published = true
              and c.active = true
              and (:categoryId is null or c.id = :categoryId)
              and (:locale is null or :locale = '' or lower(a.locale) = lower(:locale) or a.locale = 'all')
            order by a.sortOrder asc, a.title asc
            """)
    List<KbArticle> findPublished(
            @Param("categoryId") Long categoryId,
            @Param("locale") String locale);

    @Query("""
            select a from KbArticle a
            join fetch a.category c
            where a.published = true
              and c.active = true
              and lower(a.slug) = lower(:slug)
              and (:locale is null or :locale = '' or lower(a.locale) = lower(:locale) or a.locale = 'all')
            """)
    Optional<KbArticle> findPublishedBySlug(
            @Param("slug") String slug,
            @Param("locale") String locale);

    @Query("""
            select a from KbArticle a
            join a.category c
            where a.published = true
              and c.active = true
              and (:categoryId is null or c.id = :categoryId)
              and (:locale is null or :locale = '' or lower(a.locale) = lower(:locale) or a.locale = 'all')
              and (
                :q = ''
                or lower(a.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(a.summary, '')) like lower(concat('%', :q, '%'))
                or lower(a.body) like lower(concat('%', :q, '%'))
              )
            order by a.sortOrder asc, a.title asc
            """)
    Page<KbArticle> searchPublished(
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("locale") String locale,
            Pageable pageable);

    @Query("""
            select a from KbArticle a
            join a.category c
            where (:published is null or a.published = :published)
              and (:categoryId is null or c.id = :categoryId)
              and (:locale is null or :locale = '' or lower(a.locale) = lower(:locale))
              and (
                :q = ''
                or lower(a.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(a.summary, '')) like lower(concat('%', :q, '%'))
                or lower(a.slug) like lower(concat('%', :q, '%'))
              )
            order by a.sortOrder asc, a.updatedAt desc
            """)
    Page<KbArticle> searchAdmin(
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("locale") String locale,
            @Param("published") Boolean published,
            Pageable pageable);

    @Query("""
            select a from KbArticle a
            join fetch a.category c
            where a.published = true
              and c.active = true
              and (:locale is null or :locale = '' or lower(a.locale) = lower(:locale) or a.locale = 'all')
              and (
                lower(a.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(a.summary, '')) like lower(concat('%', :q, '%'))
                or lower(a.body) like lower(concat('%', :q, '%'))
              )
            order by a.sortOrder asc, a.title asc
            """)
    List<KbArticle> suggestPublished(@Param("q") String q, @Param("locale") String locale, Pageable pageable);
}
