package com.domainify.service;

import com.domainify.dto.KbArticleDto;
import com.domainify.dto.KbArticleRequest;
import com.domainify.dto.KbCategoryDto;
import com.domainify.dto.KbCategoryRequest;
import com.domainify.entity.KbArticle;
import com.domainify.entity.KbCategory;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.KbArticleRepository;
import com.domainify.repository.KbCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class KnowledgeBaseService {

    private static final Set<String> ALLOWED_LOCALES = Set.of("en", "fa", "ar", "tr", "all");

    private final KbCategoryRepository categoryRepository;
    private final KbArticleRepository articleRepository;

    public KnowledgeBaseService(KbCategoryRepository categoryRepository,
                                KbArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<KbCategoryDto> listCategoriesAdmin() {
        return listCategoriesFlat(false);
    }

    @Transactional(readOnly = true)
    public List<KbCategoryDto> listCategoriesTree(boolean activeOnly) {
        List<KbCategory> all = activeOnly
                ? categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                : categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        return buildTree(all);
    }

    @Transactional(readOnly = true)
    public List<KbCategoryDto> listCategoriesFlat(boolean activeOnly) {
        List<KbCategoryDto> tree = listCategoriesTree(activeOnly);
        List<KbCategoryDto> flat = new ArrayList<>();
        flatten(tree, flat);
        return flat;
    }

    @Transactional(readOnly = true)
    public List<KbCategoryDto> listActiveCategories(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        List<KbCategoryDto> flat = listCategoriesFlat(true);
        Set<Long> visibleIds = new HashSet<>();
        for (KbCategoryDto dto : flat) {
            long count = articleRepository.findPublished(dto.getId(), normalizedLocale).size();
            dto.setArticleCount(count);
            if (count > 0) {
                visibleIds.add(dto.getId());
                Long parentId = dto.getParentId();
                while (parentId != null && visibleIds.add(parentId)) {
                    Long currentParent = parentId;
                    parentId = flat.stream()
                            .filter(c -> Objects.equals(c.getId(), currentParent))
                            .map(KbCategoryDto::getParentId)
                            .findFirst()
                            .orElse(null);
                }
            }
        }
        return flat.stream().filter(dto -> visibleIds.contains(dto.getId())).toList();
    }

    public KbCategoryDto createCategory(KbCategoryRequest request) {
        validateCategoryRequest(request);
        String code = normalizeCode(request.getCode());
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(ErrorCode.KB_CATEGORY_CODE_EXISTS);
        }
        KbCategory category = new KbCategory();
        applyCategory(category, request, code, null);
        KbCategory saved = categoryRepository.save(category);
        return toCategoryDto(saved, depthOf(saved));
    }

    public KbCategoryDto updateCategory(Long id, KbCategoryRequest request) {
        validateCategoryRequest(request);
        KbCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.KB_CATEGORY_NOT_FOUND));
        String code = normalizeCode(request.getCode());
        categoryRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ApiException(ErrorCode.KB_CATEGORY_CODE_EXISTS);
            }
        });
        applyCategory(category, request, code, id);
        KbCategory saved = categoryRepository.save(category);
        return toCategoryDto(saved, depthOf(saved));
    }

    public void deleteCategory(Long id) {
        KbCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.KB_CATEGORY_NOT_FOUND));
        if (categoryRepository.countByParentId(id) > 0) {
            throw new ApiException(ErrorCode.KB_CATEGORY_HAS_CHILDREN);
        }
        if (articleRepository.countByCategoryId(id) > 0) {
            throw new ApiException(ErrorCode.KB_CATEGORY_IN_USE);
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Page<KbArticleDto> searchArticlesAdmin(String q, Long categoryId, String locale,
                                                  Boolean published, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return articleRepository.searchAdmin(
                normalizeQuery(q),
                categoryId,
                blankToNull(locale),
                published,
                pageable
        ).map(this::toArticleDto);
    }

    @Transactional(readOnly = true)
    public KbArticleDto getArticleAdmin(Long id) {
        return toArticleDto(requireArticle(id));
    }

    public KbArticleDto createArticle(KbArticleRequest request) {
        KbCategory category = requireActiveOrAnyCategory(request.getCategoryId());
        KbArticle article = new KbArticle();
        applyArticle(article, request, category, null);
        return toArticleDto(articleRepository.save(article));
    }

    public KbArticleDto updateArticle(Long id, KbArticleRequest request) {
        KbArticle article = requireArticle(id);
        KbCategory category = requireActiveOrAnyCategory(request.getCategoryId());
        applyArticle(article, request, category, id);
        return toArticleDto(articleRepository.save(article));
    }

    public void deleteArticle(Long id) {
        articleRepository.delete(requireArticle(id));
    }

    @Transactional(readOnly = true)
    public Page<KbArticleDto> searchPublished(String q, Long categoryId, String locale, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return articleRepository.searchPublished(
                normalizeQuery(q),
                categoryId,
                normalizeLocale(locale),
                pageable
        ).map(this::toArticleSummaryDto);
    }

    @Transactional(readOnly = true)
    public List<KbArticleDto> listPublished(Long categoryId, String locale) {
        return articleRepository.findPublished(categoryId, normalizeLocale(locale)).stream()
                .map(this::toArticleSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public KbArticleDto getPublishedBySlug(String slug, String locale) {
        if (!StringUtils.hasText(slug)) {
            throw new ApiException(ErrorCode.KB_ARTICLE_NOT_FOUND);
        }
        KbArticle article = articleRepository.findPublishedBySlug(slug.trim(), normalizeLocale(locale))
                .orElseThrow(() -> new ApiException(ErrorCode.KB_ARTICLE_NOT_FOUND));
        return toArticleDto(article);
    }

    @Transactional(readOnly = true)
    public List<KbArticleDto> suggest(String q, String locale, int limit) {
        String query = normalizeQuery(q);
        if (query.length() < 2) {
            return List.of();
        }
        int size = Math.min(Math.max(limit, 1), 10);
        return articleRepository.suggestPublished(query, normalizeLocale(locale), PageRequest.of(0, size))
                .stream()
                .map(this::toArticleSummaryDto)
                .toList();
    }

    private void applyCategory(KbCategory category, KbCategoryRequest request, String code, Long selfId) {
        category.setCode(code);
        category.setName(request.getName().trim());
        category.setDescription(blankToNull(request.getDescription()));
        category.setActive(Boolean.TRUE.equals(request.getActive()));
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setParent(resolveParent(request.getParentId(), selfId));
    }

    private KbCategory resolveParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return null;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new ApiException(ErrorCode.KB_CATEGORY_PARENT_INVALID);
        }
        KbCategory parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(ErrorCode.KB_CATEGORY_NOT_FOUND));
        if (selfId != null && isDescendant(parent, selfId)) {
            throw new ApiException(ErrorCode.KB_CATEGORY_PARENT_INVALID);
        }
        return parent;
    }

    private boolean isDescendant(KbCategory node, Long ancestorId) {
        Set<Long> seen = new HashSet<>();
        KbCategory current = node;
        while (current != null) {
            if (current.getId() == null || !seen.add(current.getId())) {
                break;
            }
            if (Objects.equals(current.getId(), ancestorId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private List<KbCategoryDto> buildTree(List<KbCategory> all) {
        Map<Long, KbCategoryDto> byId = new LinkedHashMap<>();
        for (KbCategory category : all) {
            byId.put(category.getId(), toCategoryDto(category, 0));
        }
        List<KbCategoryDto> roots = new ArrayList<>();
        for (KbCategory category : all) {
            KbCategoryDto dto = byId.get(category.getId());
            Long parentId = category.getParent() != null ? category.getParent().getId() : null;
            if (parentId != null && byId.containsKey(parentId)) {
                KbCategoryDto parent = byId.get(parentId);
                dto.setDepth(parent.getDepth() + 1);
                parent.getChildren().add(dto);
            } else {
                dto.setDepth(0);
                roots.add(dto);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<KbCategoryDto> nodes) {
        nodes.sort(Comparator
                .comparingInt(KbCategoryDto::getSortOrder)
                .thenComparing(d -> d.getName() == null ? "" : d.getName(), String.CASE_INSENSITIVE_ORDER));
        for (KbCategoryDto node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private void flatten(List<KbCategoryDto> nodes, List<KbCategoryDto> out) {
        for (KbCategoryDto node : nodes) {
            out.add(shallowCopy(node));
            flatten(node.getChildren(), out);
        }
    }

    private KbCategoryDto shallowCopy(KbCategoryDto src) {
        KbCategoryDto dto = new KbCategoryDto();
        dto.setId(src.getId());
        dto.setCode(src.getCode());
        dto.setName(src.getName());
        dto.setDescription(src.getDescription());
        dto.setParentId(src.getParentId());
        dto.setParentName(src.getParentName());
        dto.setActive(src.isActive());
        dto.setSortOrder(src.getSortOrder());
        dto.setDepth(src.getDepth());
        dto.setArticleCount(src.getArticleCount());
        dto.setCreatedAt(src.getCreatedAt());
        dto.setUpdatedAt(src.getUpdatedAt());
        return dto;
    }

    private void applyArticle(KbArticle article, KbArticleRequest request, KbCategory category, Long currentId) {
        if (request == null
                || !StringUtils.hasText(request.getTitle())
                || !StringUtils.hasText(request.getBody())
                || request.getCategoryId() == null) {
            throw new ApiException(ErrorCode.KB_ARTICLE_INVALID);
        }
        String locale = normalizeLocaleRequired(request.getLocale());
        String slug = StringUtils.hasText(request.getSlug())
                ? normalizeSlug(request.getSlug())
                : slugify(request.getTitle());
        if (!StringUtils.hasText(slug)) {
            throw new ApiException(ErrorCode.KB_ARTICLE_SLUG_INVALID);
        }
        boolean slugTaken = currentId == null
                ? articleRepository.existsBySlugIgnoreCase(slug)
                : articleRepository.existsBySlugIgnoreCaseAndIdNot(slug, currentId);
        if (slugTaken) {
            throw new ApiException(ErrorCode.KB_ARTICLE_SLUG_EXISTS);
        }

        article.setSlug(slug);
        article.setTitle(request.getTitle().trim());
        article.setSummary(blankToNull(request.getSummary()));
        article.setBody(request.getBody().trim());
        article.setLocale(locale);
        article.setCategory(category);
        article.setPublished(Boolean.TRUE.equals(request.getPublished()));
        article.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
    }

    private void validateCategoryRequest(KbCategoryRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getCode())
                || !StringUtils.hasText(request.getName())) {
            throw new ApiException(ErrorCode.KB_CATEGORY_INVALID);
        }
    }

    private KbCategory requireActiveOrAnyCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ApiException(ErrorCode.KB_CATEGORY_NOT_FOUND);
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.KB_CATEGORY_NOT_FOUND));
    }

    private KbArticle requireArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.KB_ARTICLE_NOT_FOUND));
    }

    private KbCategoryDto toCategoryDto(KbCategory category, int depth) {
        KbCategoryDto dto = new KbCategoryDto();
        dto.setId(category.getId());
        dto.setCode(category.getCode());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getId());
            dto.setParentName(category.getParent().getName());
        }
        dto.setActive(category.isActive());
        dto.setSortOrder(category.getSortOrder());
        dto.setDepth(depth);
        dto.setArticleCount(category.getId() != null ? articleRepository.countByCategoryId(category.getId()) : 0);
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }

    private int depthOf(KbCategory category) {
        int depth = 0;
        KbCategory current = category.getParent();
        Set<Long> seen = new HashSet<>();
        while (current != null && current.getId() != null && seen.add(current.getId())) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private KbArticleDto toArticleDto(KbArticle article) {
        KbArticleDto dto = toArticleSummaryDto(article);
        dto.setBody(article.getBody());
        return dto;
    }

    private KbArticleDto toArticleSummaryDto(KbArticle article) {
        KbArticleDto dto = new KbArticleDto();
        dto.setId(article.getId());
        dto.setSlug(article.getSlug());
        dto.setTitle(article.getTitle());
        dto.setSummary(article.getSummary());
        dto.setLocale(article.getLocale());
        if (article.getCategory() != null) {
            dto.setCategoryId(article.getCategory().getId());
            dto.setCategoryCode(article.getCategory().getCode());
            dto.setCategoryName(article.getCategory().getName());
        }
        dto.setPublished(article.isPublished());
        dto.setSortOrder(article.getSortOrder());
        dto.setPublishedAt(article.getPublishedAt());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        return dto;
    }

    private String normalizeCode(String code) {
        return code.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }

    private String normalizeSlug(String slug) {
        return slugify(slug);
    }

    private String slugify(String input) {
        return Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String normalizeLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            return null;
        }
        String value = locale.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_LOCALES.contains(value) ? value : null;
    }

    private String normalizeLocaleRequired(String locale) {
        if (!StringUtils.hasText(locale)) {
            return "en";
        }
        String value = locale.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_LOCALES.contains(value)) {
            throw new ApiException(ErrorCode.KB_ARTICLE_INVALID);
        }
        return value;
    }

    private String normalizeQuery(String q) {
        if (!StringUtils.hasText(q)) {
            return "";
        }
        return q.trim();
    }

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
