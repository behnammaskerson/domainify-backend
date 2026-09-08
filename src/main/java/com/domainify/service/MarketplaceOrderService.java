package com.domainify.service;

import com.domainify.dto.MarketplaceOrderDto;
import com.domainify.dto.OrderPayResponse;
import com.domainify.dto.PagedResponse;
import com.domainify.entity.DomainListing;
import com.domainify.entity.DomainListingOffer;
import com.domainify.entity.DomainListingOfferEvent;
import com.domainify.entity.DomainListingOfferEventAction;
import com.domainify.entity.DomainListingOfferStatus;
import com.domainify.entity.DomainListingStatus;
import com.domainify.entity.MarketplaceOrder;
import com.domainify.entity.MarketplaceOrderPaymentMethod;
import com.domainify.entity.MarketplaceOrderStatus;
import com.domainify.entity.PaymentIntent;
import com.domainify.entity.PaymentIntentPurpose;
import com.domainify.entity.PaymentSettings;
import com.domainify.entity.User;
import com.domainify.entity.WalletLedgerEntryType;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.exception.PaymentGatewayException;
import com.domainify.repository.DomainListingOfferEventRepository;
import com.domainify.repository.DomainListingOfferRepository;
import com.domainify.repository.DomainListingRepository;
import com.domainify.repository.MarketplaceOrderRepository;
import com.domainify.repository.UserRepository;
import com.domainify.service.zarinpal.ZarinPalClient;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MarketplaceOrderService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceOrderService.class);
    private static final String REF_MARKETPLACE_ORDER = "MARKETPLACE_ORDER";
    private static final Set<String> SORT_FIELDS = Set.of(
            "grossAmount", "status", "createdAt", "updatedAt", "paidAt", "paymentDeadline");

    private final MarketplaceOrderRepository orderRepository;
    private final DomainListingRepository listingRepository;
    private final DomainListingOfferRepository offerRepository;
    private final DomainListingOfferEventRepository offerEventRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PaymentSettingsService paymentSettingsService;
    private final PaymentIntentPersistence paymentIntentPersistence;
    private final ZarinPalClient zarinPalClient;

    @Value("${app.marketplace.order.payment-timeout-hours:24}")
    private int paymentTimeoutHours;

    public MarketplaceOrderService(
            MarketplaceOrderRepository orderRepository,
            DomainListingRepository listingRepository,
            DomainListingOfferRepository offerRepository,
            DomainListingOfferEventRepository offerEventRepository,
            UserRepository userRepository,
            WalletService walletService,
            PaymentSettingsService paymentSettingsService,
            PaymentIntentPersistence paymentIntentPersistence,
            ZarinPalClient zarinPalClient) {
        this.orderRepository = orderRepository;
        this.listingRepository = listingRepository;
        this.offerRepository = offerRepository;
        this.offerEventRepository = offerEventRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.paymentSettingsService = paymentSettingsService;
        this.paymentIntentPersistence = paymentIntentPersistence;
        this.zarinPalClient = zarinPalClient;
    }

    @Transactional
    public MarketplaceOrderDto createBuyNow(User buyer, Long listingId) {
        requireUser(buyer);
        DomainListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ApiException(ErrorCode.LISTING_NOT_FOUND));
        validatePurchaseEligibility(buyer, listing);

        BigDecimal gross = listing.getAskingPrice() == null
                ? BigDecimal.ZERO
                : listing.getAskingPrice().setScale(2, RoundingMode.HALF_UP);
        if (gross.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.LISTING_PRICE_INVALID);
        }

        MarketplaceOrder order = buildPendingOrder(listing, buyer, listing.getSeller(), null, gross);
        order = orderRepository.save(order);
        return toDto(order);
    }

    /**
     * Creates a PENDING_PAYMENT order after an offer is accepted. Listing stays ACTIVE until paid.
     */
    @Transactional
    public MarketplaceOrderDto createFromAcceptedOffer(DomainListingOffer offer) {
        if (offer == null || offer.getId() == null) {
            throw new ApiException(ErrorCode.OFFER_NOT_FOUND);
        }
        DomainListing listing = offer.getListing();
        if (listing == null || listing.getStatus() != DomainListingStatus.ACTIVE) {
            throw new ApiException(ErrorCode.ORDER_LISTING_NOT_ACTIVE);
        }
        if (orderRepository.existsByListingIdAndStatus(listing.getId(), MarketplaceOrderStatus.PENDING_PAYMENT)) {
            throw new ApiException(ErrorCode.ORDER_PENDING_EXISTS);
        }
        User buyer = offer.getBuyer();
        User seller = offer.getSeller();
        if (buyer == null || seller == null) {
            throw new ApiException(ErrorCode.ORDER_FORBIDDEN);
        }
        if (buyer.getId().equals(seller.getId())) {
            throw new ApiException(ErrorCode.ORDER_SELF_PURCHASE);
        }

        BigDecimal gross = offer.getAmount() == null
                ? BigDecimal.ZERO
                : offer.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (gross.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.OFFER_AMOUNT_INVALID);
        }

        MarketplaceOrder order = buildPendingOrder(listing, buyer, seller, offer, gross);
        order = orderRepository.save(order);
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public MarketplaceOrderDto getForUser(User user, Long id) {
        MarketplaceOrder order = requireParty(user, id);
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MarketplaceOrderDto> listMine(User user, String role, Pageable pageable) {
        requireUser(user);
        return listForUserId(user.getId(), role, pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MarketplaceOrderDto> listForUserId(Long userId, String role, Pageable pageable) {
        if (userId == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        String normalized = role == null ? "all" : role.trim().toLowerCase();
        Specification<MarketplaceOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if ("buyer".equals(normalized)) {
                predicates.add(cb.equal(root.get("buyer").get("id"), userId));
            } else if ("seller".equals(normalized)) {
                predicates.add(cb.equal(root.get("seller").get("id"), userId));
            } else {
                predicates.add(cb.or(
                        cb.equal(root.get("buyer").get("id"), userId),
                        cb.equal(root.get("seller").get("id"), userId)));
            }
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("listing", JoinType.LEFT).fetch("domain", JoinType.LEFT);
                root.fetch("buyer", JoinType.LEFT);
                root.fetch("seller", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<MarketplaceOrderDto> page = orderRepository
                .findAll(spec, sanitizePageable(pageable))
                .map(MarketplaceOrderDto::from);
        return PagedResponse.from(page);
    }

    @Transactional
    public MarketplaceOrderDto payWithWallet(User buyer, Long orderId) {
        MarketplaceOrder order = requireBuyerPending(buyer, orderId);
        try {
            walletService.debitAvailable(
                    buyer,
                    order.getGrossAmount(),
                    WalletLedgerEntryType.PURCHASE,
                    null,
                    buyer.getId(),
                    "Marketplace purchase order #" + order.getId(),
                    REF_MARKETPLACE_ORDER,
                    order.getId());
            return completePaidOrder(order.getId(), null, MarketplaceOrderPaymentMethod.WALLET);
        } catch (ApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Wallet pay failed for order {}: {}", orderId, ex.getMessage());
            throw new ApiException(ErrorCode.ORDER_WALLET_PAY_FAILED);
        }
    }

    @Transactional
    public OrderPayResponse startZarinPalPay(User buyer, Long orderId) {
        MarketplaceOrder order = requireBuyerPending(buyer, orderId);
        paymentSettingsService.requireEnabledConfigured();
        PaymentSettings settings = paymentSettingsService.getOrCreate();

        long amountIrt = toAmountIrt(order.getGrossAmount());
        PaymentIntentPurpose purpose = order.getOffer() != null
                ? PaymentIntentPurpose.OFFER_SETTLEMENT
                : PaymentIntentPurpose.BUY_NOW;
        Long listingId = order.getListing() != null ? order.getListing().getId() : null;
        Long offerId = order.getOffer() != null ? order.getOffer().getId() : null;

        PaymentIntent intent = paymentIntentPersistence.createOrderIntent(
                buyer, purpose, amountIrt, order.getId(), listingId, offerId);

        order.setPaymentIntentId(intent.getId());
        orderRepository.save(order);

        String callbackUrl = paymentSettingsService.resolveCallbackUrl();
        String mobile = buildMobile(buyer);
        String description = (purpose == PaymentIntentPurpose.OFFER_SETTLEMENT
                ? "Offer settlement"
                : "Buy now") + " #" + order.getId();
        try {
            ZarinPalClient.RequestResult zp = zarinPalClient.requestPayment(
                    settings.isSandbox(),
                    settings.getMerchantId(),
                    amountIrt,
                    description,
                    callbackUrl,
                    buyer.getEmail(),
                    mobile,
                    String.valueOf(intent.getId()));

            intent = paymentIntentPersistence.markRedirected(intent.getId(), zp);

            OrderPayResponse response = new OrderPayResponse();
            response.setPaymentIntentId(intent.getId());
            response.setAmountIrt(amountIrt);
            response.setAuthority(zp.authority());
            response.setStartPayUrl(zarinPalClient.startPayUrl(settings.isSandbox(), zp.authority()));
            return response;
        } catch (PaymentGatewayException ex) {
            paymentIntentPersistence.markFailed(intent.getId(), ex.getGatewayCode(), ex.getGatewayMessage());
            throw ex;
        } catch (RuntimeException ex) {
            paymentIntentPersistence.markFailed(intent.getId(), null, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Settles a paid order: PAID_HELD + listing SOLD + seller hold + platform commission.
     * Wallet path already debited the buyer; ZarinPal path does not debit buyer.
     * Idempotent when already PAID_HELD.
     */
    @Transactional
    public MarketplaceOrderDto completePaidOrder(
            Long orderId,
            Long paymentIntentId,
            MarketplaceOrderPaymentMethod method) {
        MarketplaceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == MarketplaceOrderStatus.PAID_HELD) {
            return toDto(order);
        }
        if (order.getStatus() != MarketplaceOrderStatus.PENDING_PAYMENT) {
            throw new ApiException(ErrorCode.ORDER_NOT_PENDING_PAYMENT);
        }

        Instant now = Instant.now();
        order.setStatus(MarketplaceOrderStatus.PAID_HELD);
        order.setPaymentMethod(method);
        if (paymentIntentId != null) {
            order.setPaymentIntentId(paymentIntentId);
        }
        order.setPaidAt(now);
        order = orderRepository.save(order);

        DomainListing listing = order.getListing();
        if (listing != null) {
            listing.setStatus(DomainListingStatus.SOLD);
            listingRepository.save(listing);
        }

        User seller = order.getSeller();
        if (seller != null && order.getSellerNet().compareTo(BigDecimal.ZERO) > 0) {
            walletService.creditHeld(
                    seller,
                    order.getSellerNet(),
                    WalletLedgerEntryType.HOLD,
                    paymentIntentId,
                    order.getBuyer() != null ? order.getBuyer().getId() : null,
                    "Escrow hold for order #" + order.getId(),
                    REF_MARKETPLACE_ORDER,
                    order.getId());
        }

        creditPlatformCommission(order, paymentIntentId);

        // TODO: notify buyer+seller on PAID_HELD (Phase 2 minimal — skip heavy notif work)
        return toDto(order);
    }

    /**
     * Cancels stale PENDING_PAYMENT orders past payment deadline. Listing stays ACTIVE;
     * linked ACCEPTED offers become EXPIRED with a system event.
     *
     * @return number of orders cancelled
     */
    @Transactional
    public int cancelExpiredPending() {
        Instant now = Instant.now();
        List<MarketplaceOrder> stale = orderRepository.findStalePendingBefore(
                MarketplaceOrderStatus.PENDING_PAYMENT, now);
        int count = 0;
        for (MarketplaceOrder order : stale) {
            order.setStatus(MarketplaceOrderStatus.CANCELLED);
            order.setCancelledAt(now);
            orderRepository.save(order);

            DomainListingOffer offer = order.getOffer();
            if (offer != null && offer.getStatus() == DomainListingOfferStatus.ACCEPTED) {
                offer.setStatus(DomainListingOfferStatus.EXPIRED);
                offer.setRespondedAt(now);
                offerRepository.save(offer);
                DomainListingOfferEvent event = new DomainListingOfferEvent();
                event.setOffer(offer);
                event.setActor(null);
                event.setAction(DomainListingOfferEventAction.EXPIRE);
                event.setAmount(offer.getAmount());
                event.setMessage("Payment timeout — order cancelled");
                offerEventRepository.save(event);
            }
            count++;
        }
        return count;
    }

    private void creditPlatformCommission(MarketplaceOrder order, Long paymentIntentId) {
        if (order.getCommissionAmount() == null
                || order.getCommissionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<User> admins = userRepository.findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(User.Role.ADMIN);
        if (admins.isEmpty()) {
            log.info("No enabled ADMIN wallet for commission on order {}; skipping", order.getId());
            return;
        }
        User admin = admins.get(0);
        walletService.creditAvailable(
                admin,
                order.getCommissionAmount(),
                WalletLedgerEntryType.COMMISSION,
                paymentIntentId,
                order.getBuyer() != null ? order.getBuyer().getId() : null,
                "Platform commission for order #" + order.getId(),
                REF_MARKETPLACE_ORDER,
                order.getId());
    }

    private MarketplaceOrder buildPendingOrder(
            DomainListing listing,
            User buyer,
            User seller,
            DomainListingOffer offer,
            BigDecimal gross) {
        PaymentSettings settings = paymentSettingsService.getOrCreate();
        BigDecimal commissionPercent = settings.getCommissionPercent() == null
                ? BigDecimal.ZERO
                : settings.getCommissionPercent();
        BigDecimal commission = gross
                .multiply(commissionPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal sellerNet = gross.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        int hours = Math.max(paymentTimeoutHours, 1);
        MarketplaceOrder order = new MarketplaceOrder();
        order.setListing(listing);
        order.setBuyer(buyer);
        order.setSeller(seller);
        order.setOffer(offer);
        order.setGrossAmount(gross);
        order.setCommissionAmount(commission);
        order.setSellerNet(sellerNet);
        order.setStatus(MarketplaceOrderStatus.PENDING_PAYMENT);
        order.setPaymentDeadline(Instant.now().plus(hours, ChronoUnit.HOURS));
        return order;
    }

    private void validatePurchaseEligibility(User buyer, DomainListing listing) {
        if (listing.getStatus() != DomainListingStatus.ACTIVE) {
            throw new ApiException(ErrorCode.ORDER_LISTING_NOT_ACTIVE);
        }
        if (listing.getSeller() != null && buyer.getId().equals(listing.getSeller().getId())) {
            throw new ApiException(ErrorCode.ORDER_SELF_PURCHASE);
        }
        if (orderRepository.existsByListingIdAndStatus(listing.getId(), MarketplaceOrderStatus.PENDING_PAYMENT)) {
            throw new ApiException(ErrorCode.ORDER_PENDING_EXISTS);
        }
    }

    private MarketplaceOrder requireBuyerPending(User buyer, Long orderId) {
        MarketplaceOrder order = requireBuyer(buyer, orderId);
        if (order.getStatus() == MarketplaceOrderStatus.PAID_HELD) {
            throw new ApiException(ErrorCode.ORDER_ALREADY_PAID);
        }
        if (order.getStatus() != MarketplaceOrderStatus.PENDING_PAYMENT) {
            throw new ApiException(ErrorCode.ORDER_NOT_PENDING_PAYMENT);
        }
        if (order.getPaymentDeadline() != null && order.getPaymentDeadline().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.ORDER_NOT_PENDING_PAYMENT);
        }
        return order;
    }

    private MarketplaceOrder requireBuyer(User buyer, Long orderId) {
        requireUser(buyer);
        MarketplaceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getBuyer() == null || !buyer.getId().equals(order.getBuyer().getId())) {
            throw new ApiException(ErrorCode.ORDER_FORBIDDEN);
        }
        return order;
    }

    private MarketplaceOrder requireParty(User user, Long id) {
        requireUser(user);
        MarketplaceOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
        Long userId = user.getId();
        boolean buyer = order.getBuyer() != null && userId.equals(order.getBuyer().getId());
        boolean seller = order.getSeller() != null && userId.equals(order.getSeller().getId());
        if (!buyer && !seller) {
            throw new ApiException(ErrorCode.ORDER_FORBIDDEN);
        }
        return order;
    }

    private MarketplaceOrderDto toDto(MarketplaceOrder order) {
        // Re-load with associations for domain name / display names when needed
        return MarketplaceOrderDto.from(orderRepository.findById(order.getId()).orElse(order));
    }

    private long toAmountIrt(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.ORDER_PAYMENT_REQUIRED);
        }
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String buildMobile(User user) {
        if (!StringUtils.hasText(user.getPhoneCountryCode()) || !StringUtils.hasText(user.getPhoneNumber())) {
            return null;
        }
        return (user.getPhoneCountryCode().trim() + user.getPhoneNumber().trim()).replaceAll("[^0-9+]", "");
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
