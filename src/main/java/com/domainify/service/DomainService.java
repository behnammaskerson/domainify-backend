package com.domainify.service;

import com.domainify.dto.DomainDto;
import com.domainify.dto.DomainStatusCountsDto;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.UpsertDomainRequest;
import com.domainify.entity.Domain;
import com.domainify.entity.DomainCategory;
import com.domainify.entity.DomainStatus;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.DomainRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DomainService {

    private static final int NAME_MAX = 253;
    private static final Pattern DOMAIN_NAME_PATTERN = Pattern.compile(
            "^(?=.{1,253}$)(?!-)[a-z0-9-]+(\\.[a-z0-9-]+)+$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "status", "price", "expiresAt", "createdAt", "updatedAt", "category.name", "ownershipStatus"
    );

    private final DomainRepository domainRepository;
    private final DomainCategoryService domainCategoryService;
    private final DomainOwnershipService domainOwnershipService;

    public DomainService(
            DomainRepository domainRepository,
            DomainCategoryService domainCategoryService,
            DomainOwnershipService domainOwnershipService) {
        this.domainRepository = domainRepository;
        this.domainCategoryService = domainCategoryService;
        this.domainOwnershipService = domainOwnershipService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<DomainDto> list(
            User owner,
            String q,
            DomainStatus status,
            Long categoryId,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Pageable pageable) {
        requireUser(owner);
        Specification<Domain> spec = buildListSpec(owner.getId(), q, status, categoryId, priceMin, priceMax);
        Pageable safe = sanitizePageable(pageable);
        Page<DomainDto> page = domainRepository.findAll(spec, safe).map(DomainDto::from);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public DomainStatusCountsDto statusCounts(User owner) {
        requireUser(owner);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DomainStatus value : DomainStatus.values()) {
            byStatus.put(value.name().toLowerCase(Locale.ROOT), 0L);
        }
        for (Object[] row : domainRepository.countByStatusForOwner(owner.getId())) {
            if (row == null || row[0] == null) {
                continue;
            }
            DomainStatus status = (DomainStatus) row[0];
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            byStatus.put(status.name().toLowerCase(Locale.ROOT), count);
        }
        return new DomainStatusCountsDto(domainRepository.countByOwnerId(owner.getId()), byStatus);
    }

    @Transactional(readOnly = true)
    public DomainDto get(User owner, Long id) {
        return DomainDto.from(requireOwned(owner, id));
    }

    @Transactional
    public DomainDto create(User owner, UpsertDomainRequest request) {
        requireUser(owner);
        Domain domain = new Domain();
        domain.setOwner(owner);
        applyRequest(domain, request);
        if (domainRepository.existsByOwnerIdAndNameIgnoreCase(owner.getId(), domain.getName())) {
            throw new ApiException(ErrorCode.DOMAIN_NAME_EXISTS);
        }
        return DomainDto.from(domainRepository.save(domain));
    }

    @Transactional
    public DomainDto update(User owner, Long id, UpsertDomainRequest request) {
        Domain domain = requireOwned(owner, id);
        applyRequest(domain, request);
        if (domainRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(owner.getId(), domain.getName(), id)) {
            throw new ApiException(ErrorCode.DOMAIN_NAME_EXISTS);
        }
        return DomainDto.from(domainRepository.save(domain));
    }

    @Transactional
    public void delete(User owner, Long id) {
        Domain domain = requireOwned(owner, id);
        domainRepository.delete(domain);
    }

    private void applyRequest(Domain domain, UpsertDomainRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.DOMAIN_NAME_REQUIRED);
        }
        String name = normalizeName(request.getName());
        if (!StringUtils.hasText(name)) {
            throw new ApiException(ErrorCode.DOMAIN_NAME_REQUIRED);
        }
        if (name.length() > NAME_MAX || !DOMAIN_NAME_PATTERN.matcher(name).matches()) {
            throw new ApiException(ErrorCode.DOMAIN_NAME_INVALID);
        }
        if (request.getStatus() == null || !EnumSet.allOf(DomainStatus.class).contains(request.getStatus())) {
            throw new ApiException(ErrorCode.DOMAIN_STATUS_INVALID);
        }
        DomainCategory category = domainCategoryService.requireActiveCategory(request.getCategoryId());
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.DOMAIN_PRICE_INVALID);
        }

        String previousName = domain.getName();
        domain.setName(name);
        domain.setStatus(request.getStatus());
        domain.setCategory(category);
        domain.setPrice(request.getPrice());
        domain.setExpiresAt(request.getExpiresAt());
        if (previousName != null && !previousName.equalsIgnoreCase(name)) {
            domainOwnershipService.resetOwnership(domain);
        }
    }

    private Specification<Domain> buildListSpec(
            Long ownerId,
            String q,
            DomainStatus status,
            Long categoryId,
            BigDecimal priceMin,
            BigDecimal priceMax) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("owner").get("id"), ownerId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                var categoryJoin = root.join("category", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(categoryJoin.get("name")), pattern),
                        cb.like(cb.lower(categoryJoin.get("code")), pattern)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? 10 : Math.min(pageable.getPageSize(), 100);
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if ("categoryName".equals(property)) {
                property = "category.name";
            }
            if (!ALLOWED_SORT_FIELDS.contains(property)) {
                continue;
            }
            Sort.Order next = new Sort.Order(order.getDirection(), property);
            if ("name".equals(property) || "category.name".equals(property)) {
                next = next.ignoreCase();
            }
            orders.add(next);
        }
        if (orders.isEmpty()) {
            orders.add(Sort.Order.asc("name"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private Domain requireOwned(User owner, Long id) {
        requireUser(owner);
        if (id == null) {
            throw new ApiException(ErrorCode.DOMAIN_NOT_FOUND);
        }
        return domainRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.DOMAIN_NOT_FOUND));
    }

    private void requireUser(User owner) {
        if (owner == null || owner.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private String normalizeName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
