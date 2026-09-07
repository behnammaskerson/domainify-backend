package com.domainify.service;

import com.domainify.dto.DomainListingDto;
import com.domainify.dto.PagedResponse;
import com.domainify.dto.UpsertDomainListingRequest;
import com.domainify.entity.Domain;
import com.domainify.entity.DomainListing;
import com.domainify.entity.DomainListingStatus;
import com.domainify.entity.DomainOwnershipStatus;
import com.domainify.entity.DomainStatus;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.DomainListingRepository;
import com.domainify.repository.DomainRepository;
import jakarta.persistence.criteria.Join;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class DomainListingService {

    private static final int DESCRIPTION_MAX = 2000;
    private static final Set<String> SELLER_SORT_FIELDS = Set.of(
            "askingPrice", "status", "featured", "createdAt", "updatedAt", "domain.name"
    );
    private static final Set<String> PUBLIC_SORT_FIELDS = Set.of(
            "askingPrice", "featured", "createdAt", "domain.name"
    );

    private final DomainListingRepository listingRepository;
    private final DomainRepository domainRepository;

    public DomainListingService(DomainListingRepository listingRepository, DomainRepository domainRepository) {
        this.listingRepository = listingRepository;
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<DomainListingDto> listMine(
            User seller,
            String q,
            DomainListingStatus status,
            Pageable pageable) {
        requireUser(seller);
        Specification<DomainListing> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("seller").get("id"), seller.getId()));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(q)) {
                Join<DomainListing, Domain> domainJoin = root.join("domain", JoinType.INNER);
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(domainJoin.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<DomainListingDto> page = listingRepository
                .findAll(spec, sanitizePageable(pageable, SELLER_SORT_FIELDS, "createdAt"))
                .map(DomainListingDto::from);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public DomainListingDto getMine(User seller, Long id) {
        return DomainListingDto.from(requireOwned(seller, id));
    }

    @Transactional(readOnly = true)
    public Optional<DomainListingDto> findMineByDomainId(User seller, Long domainId) {
        requireUser(seller);
        return listingRepository.findByDomainId(domainId)
                .filter(l -> l.getSeller() != null && seller.getId().equals(l.getSeller().getId()))
                .map(DomainListingDto::from);
    }

    @Transactional
    public DomainListingDto create(User seller, UpsertDomainListingRequest request) {
        requireUser(seller);
        if (request == null || request.getDomainId() == null) {
            throw new ApiException(ErrorCode.LISTING_DOMAIN_REQUIRED);
        }
        Domain domain = domainRepository.findByIdAndOwnerId(request.getDomainId(), seller.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.DOMAIN_NOT_FOUND));
        validateDomainListable(domain);
        if (listingRepository.existsByDomainId(domain.getId())) {
            throw new ApiException(ErrorCode.LISTING_ALREADY_EXISTS);
        }

        DomainListing listing = new DomainListing();
        listing.setSeller(seller);
        listing.setDomain(domain);
        applyWritable(listing, request, true);
        listing.setStatus(DomainListingStatus.ACTIVE);
        return DomainListingDto.from(listingRepository.save(listing));
    }

    @Transactional
    public DomainListingDto update(User seller, Long id, UpsertDomainListingRequest request) {
        DomainListing listing = requireOwned(seller, id);
        if (listing.getStatus() == DomainListingStatus.SOLD) {
            throw new ApiException(ErrorCode.LISTING_SOLD);
        }
        applyWritable(listing, request, false);
        return DomainListingDto.from(listingRepository.save(listing));
    }

    @Transactional
    public DomainListingDto deactivate(User seller, Long id) {
        DomainListing listing = requireOwned(seller, id);
        if (listing.getStatus() == DomainListingStatus.SOLD) {
            throw new ApiException(ErrorCode.LISTING_SOLD);
        }
        listing.setStatus(DomainListingStatus.INACTIVE);
        return DomainListingDto.from(listingRepository.save(listing));
    }

    @Transactional
    public DomainListingDto activate(User seller, Long id) {
        DomainListing listing = requireOwned(seller, id);
        if (listing.getStatus() == DomainListingStatus.SOLD) {
            throw new ApiException(ErrorCode.LISTING_SOLD);
        }
        validateDomainListable(listing.getDomain());
        listing.setStatus(DomainListingStatus.ACTIVE);
        return DomainListingDto.from(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public PagedResponse<DomainListingDto> browseMarketplace(
            String q,
            Long categoryId,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Boolean featured,
            Pageable pageable) {
        Specification<DomainListing> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), DomainListingStatus.ACTIVE));
            Join<DomainListing, Domain> domainJoin = root.join("domain", JoinType.INNER);
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(domainJoin.get("category").get("id"), categoryId));
            }
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("askingPrice"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("askingPrice"), priceMax));
            }
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(domainJoin.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("domain", JoinType.LEFT);
                root.fetch("seller", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<DomainListingDto> page = listingRepository
                .findAll(spec, sanitizePageable(pageable, PUBLIC_SORT_FIELDS, "createdAt"))
                .map(DomainListingDto::from);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public DomainListingDto getPublic(Long id) {
        DomainListing listing = listingRepository.findPublicById(id, DomainListingStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));
        return DomainListingDto.from(listing);
    }

    private void applyWritable(DomainListing listing, UpsertDomainListingRequest request, boolean creating) {
        if (request == null) {
            throw new ApiException(ErrorCode.LISTING_PRICE_INVALID);
        }
        if (request.getAskingPrice() == null || request.getAskingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.LISTING_PRICE_INVALID);
        }
        listing.setAskingPrice(request.getAskingPrice());
        String description = request.getDescription() != null ? request.getDescription().trim() : "";
        if (description.length() > DESCRIPTION_MAX) {
            throw new ApiException(ErrorCode.LISTING_DESCRIPTION_TOO_LONG);
        }
        listing.setDescription(description);
        if (request.getFeatured() != null) {
            listing.setFeatured(request.getFeatured());
        } else if (creating) {
            listing.setFeatured(false);
        }
    }

    private void validateDomainListable(Domain domain) {
        if (domain == null) {
            throw new ApiException(ErrorCode.DOMAIN_NOT_FOUND);
        }
        if (domain.getStatus() != DomainStatus.ACTIVE && domain.getStatus() != DomainStatus.PENDING) {
            throw new ApiException(ErrorCode.LISTING_DOMAIN_NOT_LISTABLE);
        }
        if (domain.getOwnershipStatus() != DomainOwnershipStatus.VERIFIED) {
            throw new ApiException(ErrorCode.LISTING_DOMAIN_NOT_VERIFIED);
        }
    }

    private DomainListing requireOwned(User seller, Long id) {
        requireUser(seller);
        return listingRepository.findByIdAndSellerId(id, seller.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Pageable sanitizePageable(Pageable pageable, Set<String> allowed, String defaultSort) {
        if (pageable == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, defaultSort));
        }
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, defaultSort));
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (allowed.contains(order.getProperty())) {
                orders.add(order);
            }
        }
        if (orders.isEmpty()) {
            orders.add(new Sort.Order(Sort.Direction.DESC, defaultSort));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }
}
