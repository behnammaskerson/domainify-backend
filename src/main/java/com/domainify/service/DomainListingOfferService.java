package com.domainify.service;

import com.domainify.dto.AcceptOfferResponse;
import com.domainify.dto.CreateOrCounterOfferRequest;
import com.domainify.dto.ListingOfferDto;
import com.domainify.dto.MarketplaceOrderDto;
import com.domainify.dto.PagedResponse;
import com.domainify.entity.DomainListing;
import com.domainify.entity.DomainListingOffer;
import com.domainify.entity.DomainListingOfferEvent;
import com.domainify.entity.DomainListingOfferEventAction;
import com.domainify.entity.DomainListingOfferStatus;
import com.domainify.entity.DomainListingStatus;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.DomainListingOfferEventRepository;
import com.domainify.repository.DomainListingOfferRepository;
import com.domainify.repository.DomainListingRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class DomainListingOfferService {

    private static final int MESSAGE_MAX = 1000;
    private static final Set<DomainListingOfferStatus> OPEN_STATUSES = EnumSet.of(
            DomainListingOfferStatus.PENDING,
            DomainListingOfferStatus.COUNTERED
    );
    private static final Set<String> SORT_FIELDS = Set.of("amount", "status", "createdAt", "updatedAt");

    private final DomainListingOfferRepository offerRepository;
    private final DomainListingOfferEventRepository eventRepository;
    private final DomainListingRepository listingRepository;
    private final NotificationService notificationService;
    private final MarketplaceOrderService marketplaceOrderService;

    @Value("${app.marketplace.offer.expiry-enabled:true}")
    private boolean expiryEnabled;

    @Value("${app.marketplace.offer.expiry-days:7}")
    private int expiryDays;

    public DomainListingOfferService(
            DomainListingOfferRepository offerRepository,
            DomainListingOfferEventRepository eventRepository,
            DomainListingRepository listingRepository,
            NotificationService notificationService,
            MarketplaceOrderService marketplaceOrderService) {
        this.offerRepository = offerRepository;
        this.eventRepository = eventRepository;
        this.listingRepository = listingRepository;
        this.notificationService = notificationService;
        this.marketplaceOrderService = marketplaceOrderService;
    }

    @Transactional
    public ListingOfferDto create(User buyer, Long listingId, CreateOrCounterOfferRequest request) {
        requireUser(buyer);
        DomainListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));
        if (listing.getStatus() != DomainListingStatus.ACTIVE) {
            throw new ApiException(ErrorCode.OFFER_LISTING_NOT_ACTIVE);
        }
        if (listing.getSeller() != null && buyer.getId().equals(listing.getSeller().getId())) {
            throw new ApiException(ErrorCode.OFFER_SELF);
        }
        if (offerRepository.existsByListingIdAndBuyerIdAndStatusIn(listing.getId(), buyer.getId(), OPEN_STATUSES)) {
            throw new ApiException(ErrorCode.OFFER_ALREADY_OPEN);
        }

        BigDecimal amount = requireAmount(request);
        String message = normalizeMessage(request != null ? request.getMessage() : null);

        DomainListingOffer offer = new DomainListingOffer();
        offer.setListing(listing);
        offer.setBuyer(buyer);
        offer.setSeller(listing.getSeller());
        offer.setAmount(amount);
        offer.setMessage(message);
        offer.setStatus(DomainListingOfferStatus.PENDING);
        offer = offerRepository.save(offer);
        appendEvent(offer, buyer, DomainListingOfferEventAction.OFFER, amount, message);
        notificationService.notifyOfferReceived(offer, buyer);
        return ListingOfferDto.from(offer, eventsFor(offer.getId()));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ListingOfferDto> listMine(User buyer, DomainListingOfferStatus status, Pageable pageable) {
        requireUser(buyer);
        Specification<DomainListingOffer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("buyer").get("id"), buyer.getId()));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("listing", JoinType.LEFT).fetch("domain", JoinType.LEFT);
                root.fetch("seller", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<ListingOfferDto> page = offerRepository
                .findAll(spec, sanitizePageable(pageable))
                .map(ListingOfferDto::from);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ListingOfferDto> listForListing(
            User seller,
            Long listingId,
            DomainListingOfferStatus status,
            Pageable pageable) {
        requireUser(seller);
        DomainListing listing = listingRepository.findByIdAndSellerId(listingId, seller.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));
        Specification<DomainListingOffer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("listing").get("id"), listing.getId()));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("buyer", JoinType.LEFT);
                root.fetch("listing", JoinType.LEFT).fetch("domain", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<ListingOfferDto> page = offerRepository
                .findAll(spec, sanitizePageable(pageable))
                .map(ListingOfferDto::from);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ListingOfferDto get(User user, Long id) {
        DomainListingOffer offer = requireParty(user, id);
        return ListingOfferDto.from(offer, eventsFor(offer.getId()));
    }

    @Transactional
    public ListingOfferDto counter(User user, Long id, CreateOrCounterOfferRequest request) {
        DomainListingOffer offer = requireParty(user, id);
        requireOpenListing(offer);
        BigDecimal amount = requireAmount(request);
        String message = normalizeMessage(request != null ? request.getMessage() : null);

        if (offer.getStatus() == DomainListingOfferStatus.PENDING) {
            requireSeller(user, offer);
            offer.setStatus(DomainListingOfferStatus.COUNTERED);
        } else if (offer.getStatus() == DomainListingOfferStatus.COUNTERED) {
            requireBuyer(user, offer);
            offer.setStatus(DomainListingOfferStatus.PENDING);
        } else {
            throw new ApiException(ErrorCode.OFFER_INVALID_STATE);
        }

        offer.setAmount(amount);
        offer.setMessage(message);
        offer.setRespondedAt(Instant.now());
        offer = offerRepository.save(offer);
        appendEvent(offer, user, DomainListingOfferEventAction.COUNTER, amount, message);
        notificationService.notifyOfferCountered(offer, user);
        return ListingOfferDto.from(offer, eventsFor(offer.getId()));
    }

    @Transactional
    public AcceptOfferResponse accept(User user, Long id) {
        DomainListingOffer offer = requireParty(user, id);
        requireOpenListing(offer);
        requireResponder(user, offer);

        Instant now = Instant.now();
        offer.setStatus(DomainListingOfferStatus.ACCEPTED);
        offer.setRespondedAt(now);
        offer = offerRepository.save(offer);
        appendEvent(offer, user, DomainListingOfferEventAction.ACCEPT, offer.getAmount(), "");

        // Do NOT mark listing SOLD until payment settles (Phase 2 escrow).
        DomainListing listing = offer.getListing();

        List<DomainListingOffer> siblings = offerRepository.findByListingIdAndStatusInAndIdNot(
                listing.getId(), OPEN_STATUSES, offer.getId());
        for (DomainListingOffer sibling : siblings) {
            sibling.setStatus(DomainListingOfferStatus.REJECTED);
            sibling.setRespondedAt(now);
            offerRepository.save(sibling);
            appendEvent(sibling, null, DomainListingOfferEventAction.SYSTEM_REJECT, sibling.getAmount(), "");
            notificationService.notifyOfferRejected(sibling, user);
        }

        MarketplaceOrderDto order = marketplaceOrderService.createFromAcceptedOffer(offer);
        notificationService.notifyOfferAccepted(offer, user);
        return new AcceptOfferResponse(ListingOfferDto.from(offer, eventsFor(offer.getId())), order);
    }

    @Transactional
    public ListingOfferDto reject(User user, Long id) {
        DomainListingOffer offer = requireParty(user, id);
        requireOpenListing(offer);
        requireResponder(user, offer);

        offer.setStatus(DomainListingOfferStatus.REJECTED);
        offer.setRespondedAt(Instant.now());
        offer = offerRepository.save(offer);
        appendEvent(offer, user, DomainListingOfferEventAction.REJECT, offer.getAmount(), "");
        notificationService.notifyOfferRejected(offer, user);
        return ListingOfferDto.from(offer, eventsFor(offer.getId()));
    }

    @Transactional
    public ListingOfferDto withdraw(User buyer, Long id) {
        DomainListingOffer offer = requireParty(buyer, id);
        requireBuyer(buyer, offer);
        if (offer.getStatus() != DomainListingOfferStatus.PENDING) {
            throw new ApiException(ErrorCode.OFFER_NOT_PENDING);
        }
        offer.setStatus(DomainListingOfferStatus.WITHDRAWN);
        offer.setRespondedAt(Instant.now());
        offer = offerRepository.save(offer);
        appendEvent(offer, buyer, DomainListingOfferEventAction.WITHDRAW, offer.getAmount(), "");
        notificationService.notifyOfferWithdrawn(offer, buyer);
        return ListingOfferDto.from(offer, eventsFor(offer.getId()));
    }

    /**
     * Expires idle PENDING/COUNTERED offers whose last update is older than configured days.
     * @return number of offers expired
     */
    @Transactional
    public int expireStaleOffers() {
        if (!expiryEnabled || expiryDays < 1) {
            return 0;
        }
        Instant cutoff = Instant.now().minus(expiryDays, ChronoUnit.DAYS);
        List<DomainListingOffer> stale = offerRepository.findStaleOpenOffers(OPEN_STATUSES, cutoff);
        Instant now = Instant.now();
        int count = 0;
        for (DomainListingOffer offer : stale) {
            offer.setStatus(DomainListingOfferStatus.EXPIRED);
            offer.setRespondedAt(now);
            offerRepository.save(offer);
            appendEvent(offer, null, DomainListingOfferEventAction.EXPIRE, offer.getAmount(), "");
            notificationService.notifyOfferExpired(offer);
            count++;
        }
        return count;
    }

    private void requireResponder(User user, DomainListingOffer offer) {
        if (offer.getStatus() == DomainListingOfferStatus.PENDING) {
            requireSeller(user, offer);
        } else if (offer.getStatus() == DomainListingOfferStatus.COUNTERED) {
            requireBuyer(user, offer);
        } else {
            throw new ApiException(ErrorCode.OFFER_INVALID_STATE);
        }
    }

    private void requireOpenListing(DomainListingOffer offer) {
        DomainListing listing = offer.getListing();
        if (listing == null || listing.getStatus() != DomainListingStatus.ACTIVE) {
            throw new ApiException(ErrorCode.OFFER_LISTING_NOT_ACTIVE);
        }
    }

    private DomainListingOffer requireParty(User user, Long id) {
        requireUser(user);
        DomainListingOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.OFFER_NOT_FOUND));
        Long userId = user.getId();
        boolean buyer = offer.getBuyer() != null && userId.equals(offer.getBuyer().getId());
        boolean seller = offer.getSeller() != null && userId.equals(offer.getSeller().getId());
        if (!buyer && !seller) {
            throw new ApiException(ErrorCode.OFFER_WRONG_PARTY);
        }
        return offer;
    }

    private void requireBuyer(User user, DomainListingOffer offer) {
        if (offer.getBuyer() == null || !user.getId().equals(offer.getBuyer().getId())) {
            throw new ApiException(ErrorCode.OFFER_WRONG_PARTY);
        }
    }

    private void requireSeller(User user, DomainListingOffer offer) {
        if (offer.getSeller() == null || !user.getId().equals(offer.getSeller().getId())) {
            throw new ApiException(ErrorCode.OFFER_WRONG_PARTY);
        }
    }

    private BigDecimal requireAmount(CreateOrCounterOfferRequest request) {
        if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorCode.OFFER_AMOUNT_INVALID);
        }
        return request.getAmount();
    }

    private String normalizeMessage(String message) {
        String normalized = message != null ? message.trim() : "";
        if (normalized.length() > MESSAGE_MAX) {
            throw new ApiException(ErrorCode.OFFER_MESSAGE_TOO_LONG);
        }
        return normalized;
    }

    private void appendEvent(
            DomainListingOffer offer,
            User actor,
            DomainListingOfferEventAction action,
            BigDecimal amount,
            String message) {
        DomainListingOfferEvent event = new DomainListingOfferEvent();
        event.setOffer(offer);
        event.setActor(actor);
        event.setAction(action);
        event.setAmount(amount);
        event.setMessage(message != null ? message : "");
        eventRepository.save(event);
    }

    private List<DomainListingOfferEvent> eventsFor(Long offerId) {
        return eventRepository.findByOfferIdOrderByCreatedAtAscIdAsc(offerId);
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (SORT_FIELDS.contains(order.getProperty())) {
                orders.add(order);
            }
        }
        if (orders.isEmpty()) {
            orders.add(new Sort.Order(Sort.Direction.DESC, "createdAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }
}
