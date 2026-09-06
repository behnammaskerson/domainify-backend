package com.domainify.service;

import com.domainify.dto.DomainCategoryDto;
import com.domainify.dto.DomainCategoryRequest;
import com.domainify.entity.DomainCategory;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.DomainCategoryRepository;
import com.domainify.repository.DomainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DomainCategoryService {

    private static final int NAME_MAX = 100;
    private static final int CODE_MAX = 64;

    private final DomainCategoryRepository domainCategoryRepository;
    private final DomainRepository domainRepository;

    public DomainCategoryService(
            DomainCategoryRepository domainCategoryRepository,
            DomainRepository domainRepository) {
        this.domainCategoryRepository = domainCategoryRepository;
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public List<DomainCategoryDto> listTree(boolean activeOnly) {
        List<DomainCategory> all = activeOnly
                ? domainCategoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                : domainCategoryRepository.findAllByOrderBySortOrderAscNameAsc();
        return buildTree(all);
    }

    @Transactional(readOnly = true)
    public List<DomainCategoryDto> listFlat(boolean activeOnly) {
        List<DomainCategoryDto> tree = listTree(activeOnly);
        List<DomainCategoryDto> flat = new ArrayList<>();
        flatten(tree, flat);
        return flat;
    }

    @Transactional(readOnly = true)
    public DomainCategory requireActiveCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_REQUIRED);
        }
        DomainCategory category = domainCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOMAIN_CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_INACTIVE);
        }
        return category;
    }

    @Transactional(readOnly = true)
    public DomainCategory requireCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_REQUIRED);
        }
        return domainCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOMAIN_CATEGORY_NOT_FOUND));
    }

    @Transactional
    public DomainCategoryDto create(DomainCategoryRequest request) {
        String name = normalizeName(request.getName());
        boolean codeProvided = StringUtils.hasText(request.getCode());
        String code = normalizeCode(codeProvided ? request.getCode() : name);
        // Non-Latin names (fa/ar/…) slugify to empty — auto-allocate a code when omitted.
        if (!StringUtils.hasText(code)) {
            if (codeProvided) {
                throw new ApiException(ErrorCode.DOMAIN_CATEGORY_CODE_INVALID);
            }
            code = allocateUniqueCode();
        } else if (domainCategoryRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_CODE_EXISTS);
        }

        DomainCategory category = new DomainCategory();
        category.setName(name);
        category.setCode(code);
        category.setActive(request.getActive() == null || request.getActive());
        category.setSortOrder(request.getSortOrder() == null ? nextSortOrder(request.getParentId()) : request.getSortOrder());
        category.setParent(resolveParent(request.getParentId(), null));
        return toDto(domainCategoryRepository.save(category), 0);
    }

    @Transactional
    public DomainCategoryDto update(Long id, DomainCategoryRequest request) {
        DomainCategory category = domainCategoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.DOMAIN_CATEGORY_NOT_FOUND));

        String name = normalizeName(request.getName());
        category.setName(name);

        if (StringUtils.hasText(request.getCode())) {
            String code = normalizeCode(request.getCode());
            if (!StringUtils.hasText(code)) {
                throw new ApiException(ErrorCode.DOMAIN_CATEGORY_CODE_INVALID);
            }
            if (domainCategoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw new ApiException(ErrorCode.DOMAIN_CATEGORY_CODE_EXISTS);
            }
            category.setCode(code);
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        // parentId may be explicitly null (move to root) when JSON includes "parentId": null.
        // Distinguish omitted vs null by checking if request carried the key via a sentinel:
        // DomainCategoryRequest always has parentId field; treat updates as applying parent when
        // the client sends the field. For simplicity, always apply parentId from request body.
        category.setParent(resolveParent(request.getParentId(), id));

        return toDto(domainCategoryRepository.save(category), depthOf(category));
    }

    @Transactional
    public void delete(Long id) {
        DomainCategory category = domainCategoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.DOMAIN_CATEGORY_NOT_FOUND));
        if (domainCategoryRepository.countByParentId(id) > 0) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_HAS_CHILDREN);
        }
        if (domainRepository.countByCategoryId(id) > 0) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_IN_USE);
        }
        domainCategoryRepository.delete(category);
    }

    private DomainCategory resolveParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return null;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_PARENT_INVALID);
        }
        DomainCategory parent = domainCategoryRepository.findById(parentId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOMAIN_CATEGORY_NOT_FOUND));
        if (selfId != null && isDescendant(parent, selfId)) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_PARENT_INVALID);
        }
        return parent;
    }

    private boolean isDescendant(DomainCategory node, Long ancestorId) {
        Set<Long> seen = new HashSet<>();
        DomainCategory current = node;
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

    private List<DomainCategoryDto> buildTree(List<DomainCategory> all) {
        Map<Long, DomainCategoryDto> byId = new LinkedHashMap<>();
        for (DomainCategory category : all) {
            byId.put(category.getId(), toDto(category, 0));
        }
        List<DomainCategoryDto> roots = new ArrayList<>();
        for (DomainCategory category : all) {
            DomainCategoryDto dto = byId.get(category.getId());
            Long parentId = category.getParent() != null ? category.getParent().getId() : null;
            if (parentId != null && byId.containsKey(parentId)) {
                DomainCategoryDto parent = byId.get(parentId);
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

    private void sortTree(List<DomainCategoryDto> nodes) {
        nodes.sort(Comparator
                .comparingInt(DomainCategoryDto::getSortOrder)
                .thenComparing(d -> d.getName() == null ? "" : d.getName(), String.CASE_INSENSITIVE_ORDER));
        for (DomainCategoryDto node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private void flatten(List<DomainCategoryDto> nodes, List<DomainCategoryDto> out) {
        for (DomainCategoryDto node : nodes) {
            DomainCategoryDto copy = shallowCopy(node);
            out.add(copy);
            flatten(node.getChildren(), out);
        }
    }

    private DomainCategoryDto shallowCopy(DomainCategoryDto src) {
        DomainCategoryDto dto = new DomainCategoryDto();
        dto.setId(src.getId());
        dto.setCode(src.getCode());
        dto.setName(src.getName());
        dto.setParentId(src.getParentId());
        dto.setParentName(src.getParentName());
        dto.setActive(src.isActive());
        dto.setSortOrder(src.getSortOrder());
        dto.setDepth(src.getDepth());
        return dto;
    }

    private DomainCategoryDto toDto(DomainCategory category, int depth) {
        DomainCategoryDto dto = new DomainCategoryDto();
        dto.setId(category.getId());
        dto.setCode(category.getCode());
        dto.setName(category.getName());
        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getId());
            dto.setParentName(category.getParent().getName());
        }
        dto.setActive(category.isActive());
        dto.setSortOrder(category.getSortOrder());
        dto.setDepth(depth);
        return dto;
    }

    private int depthOf(DomainCategory category) {
        int depth = 0;
        DomainCategory current = category.getParent();
        Set<Long> seen = new HashSet<>();
        while (current != null && current.getId() != null && seen.add(current.getId())) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private int nextSortOrder(Long parentId) {
        List<DomainCategory> siblings = parentId == null
                ? domainCategoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                    .filter(c -> c.getParent() == null)
                    .collect(Collectors.toList())
                : domainCategoryRepository.findByParentIdOrderBySortOrderAscNameAsc(parentId);
        return siblings.stream().mapToInt(DomainCategory::getSortOrder).max().orElse(-1) + 1;
    }

    private String normalizeName(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_NAME_REQUIRED);
        }
        String name = raw.trim().replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > NAME_MAX) {
            throw new ApiException(ErrorCode.DOMAIN_CATEGORY_NAME_INVALID);
        }
        return name;
    }

    private String normalizeCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String code = raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (code.length() > CODE_MAX) {
            code = code.substring(0, CODE_MAX).replaceAll("-+$", "");
        }
        return code;
    }

    private String allocateUniqueCode() {
        for (int attempt = 0; attempt < 12; attempt++) {
            String candidate = "cat-" + Long.toString(System.nanoTime() + attempt, 36);
            if (candidate.length() > CODE_MAX) {
                candidate = candidate.substring(0, CODE_MAX).replaceAll("-+$", "");
            }
            if (StringUtils.hasText(candidate) && !domainCategoryRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(ErrorCode.DOMAIN_CATEGORY_CODE_INVALID);
    }
}
