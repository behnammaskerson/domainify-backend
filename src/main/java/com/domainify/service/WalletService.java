package com.domainify.service;

import com.domainify.dto.AdminWalletAdjustRequest;
import com.domainify.dto.WalletDto;
import com.domainify.entity.PaymentIntent;
import com.domainify.entity.User;
import com.domainify.entity.Wallet;
import com.domainify.entity.WalletLedgerDirection;
import com.domainify.entity.WalletLedgerEntry;
import com.domainify.entity.WalletLedgerEntryType;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.PaymentIntentRepository;
import com.domainify.repository.UserRepository;
import com.domainify.repository.WalletLedgerEntryRepository;
import com.domainify.repository.WalletRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerEntryRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentSettingsService paymentSettingsService;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final NotificationService notificationService;

    public WalletService(
            WalletRepository walletRepository,
            WalletLedgerEntryRepository ledgerEntryRepository,
            PaymentIntentRepository paymentIntentRepository,
            PaymentSettingsService paymentSettingsService,
            UserRepository userRepository,
            EntityManager entityManager,
            NotificationService notificationService) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentSettingsService = paymentSettingsService;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.notificationService = notificationService;
    }

    @Transactional
    public Wallet getOrCreate(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUser(entityManager.getReference(User.class, user.getId()));
                    wallet.setAvailableBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    wallet.setHeldBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    return walletRepository.save(wallet);
                });
    }

    @Transactional(readOnly = true)
    public WalletDto getWalletDto(User user, int ledgerLimit) {
        return buildWalletDto(user.getId(), ledgerLimit);
    }

    @Transactional(readOnly = true)
    public WalletDto getWalletDtoForUserId(Long userId, int ledgerLimit) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return buildWalletDto(userId, ledgerLimit);
    }

    @Transactional
    public WalletDto adminAdjust(Long userId, AdminWalletAdjustRequest request, User actor) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        BigDecimal amount = BigDecimal.valueOf(request.getAmountIrt());
        String note = request.getNote().trim();
        String direction = request.getDirection() == AdminWalletAdjustRequest.Direction.CREDIT
                ? "credit"
                : "debit";
        if (request.getDirection() == AdminWalletAdjustRequest.Direction.CREDIT) {
            creditAvailable(target, amount, WalletLedgerEntryType.ADJUSTMENT, null, actor.getId(), note);
        } else {
            debitAvailable(target, amount, WalletLedgerEntryType.ADJUSTMENT, null, actor.getId(), note);
        }
        notificationService.notifyWalletAdjusted(target, actor, direction, amount, note);
        return buildWalletDto(userId, 20);
    }

    /**
     * Credits available balance and appends a ledger row. Idempotent when {@code paymentIntentId}
     * is set and a matching entry already exists.
     */
    @Transactional
    public Wallet creditAvailable(
            User user,
            BigDecimal amount,
            WalletLedgerEntryType entryType,
            Long paymentIntentId,
            Long actorId,
            String note) {
        return creditAvailable(user, amount, entryType, paymentIntentId, actorId, note, null, null);
    }

    @Transactional
    public Wallet creditAvailable(
            User user,
            BigDecimal amount,
            WalletLedgerEntryType entryType,
            Long paymentIntentId,
            Long actorId,
            String note,
            String refType,
            Long refId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.WALLET_AMOUNT_INVALID);
        }
        BigDecimal credit = amount.setScale(2, RoundingMode.HALF_UP);

        if (paymentIntentId != null
                && ledgerEntryRepository.existsByPaymentIntentIdAndEntryType(paymentIntentId, entryType)) {
            return getOrCreate(user);
        }

        Wallet wallet = getOrCreate(user);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(credit));
        walletRepository.save(wallet);
        appendLedger(wallet, WalletLedgerDirection.CREDIT, credit, entryType, paymentIntentId, actorId, note,
                refType, refId);
        return wallet;
    }

    /**
     * Credits held balance only (available unchanged). Idempotent when {@code paymentIntentId}
     * is set and a matching entry already exists.
     */
    @Transactional
    public Wallet creditHeld(
            User user,
            BigDecimal amount,
            WalletLedgerEntryType type,
            Long paymentIntentId,
            Long actorId,
            String note,
            String refType,
            Long refId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.WALLET_AMOUNT_INVALID);
        }
        BigDecimal credit = amount.setScale(2, RoundingMode.HALF_UP);

        if (paymentIntentId != null
                && ledgerEntryRepository.existsByPaymentIntentIdAndEntryType(paymentIntentId, type)) {
            return getOrCreate(user);
        }

        Wallet wallet = getOrCreate(user);
        wallet.setHeldBalance(wallet.getHeldBalance().add(credit));
        walletRepository.save(wallet);
        appendLedger(wallet, WalletLedgerDirection.CREDIT, credit, type, paymentIntentId, actorId, note,
                refType, refId);
        return wallet;
    }

    @Transactional
    public Wallet debitAvailable(
            User user,
            BigDecimal amount,
            WalletLedgerEntryType entryType,
            Long paymentIntentId,
            Long actorId,
            String note) {
        return debitAvailable(user, amount, entryType, paymentIntentId, actorId, note, null, null);
    }

    @Transactional
    public Wallet debitAvailable(
            User user,
            BigDecimal amount,
            WalletLedgerEntryType entryType,
            Long paymentIntentId,
            Long actorId,
            String note,
            String refType,
            Long refId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorCode.WALLET_AMOUNT_INVALID);
        }
        BigDecimal debit = amount.setScale(2, RoundingMode.HALF_UP);

        if (paymentIntentId != null
                && ledgerEntryRepository.existsByPaymentIntentIdAndEntryType(paymentIntentId, entryType)) {
            return getOrCreate(user);
        }

        Wallet wallet = getOrCreate(user);
        if (wallet.getAvailableBalance().compareTo(debit) < 0) {
            throw new ApiException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(debit));
        walletRepository.save(wallet);
        appendLedger(wallet, WalletLedgerDirection.DEBIT, debit, entryType, paymentIntentId, actorId, note,
                refType, refId);
        return wallet;
    }

    private WalletDto buildWalletDto(Long userId, int ledgerLimit) {
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        var settings = paymentSettingsService.getDto();
        int limit = Math.min(Math.max(ledgerLimit, 1), 100);

        WalletDto dto = new WalletDto();
        dto.setMinTopUpIrt(settings.getMinTopUpIrt());
        dto.setPaymentsEnabled(settings.isEnabled() && StringUtils.hasText(settings.getMerchantId()));
        dto.setRecentPayments(
                paymentIntentRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                        .stream()
                        .map(this::toPaymentDto)
                        .toList());

        if (wallet == null) {
            dto.setWalletId(null);
            dto.setAvailableBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            dto.setHeldBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            dto.setRecentLedger(List.of());
            return dto;
        }

        dto.setWalletId(wallet.getId());
        dto.setAvailableBalance(wallet.getAvailableBalance());
        dto.setHeldBalance(wallet.getHeldBalance());
        dto.setRecentLedger(
                ledgerEntryRepository
                        .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(0, limit))
                        .map(this::toLedgerDto)
                        .getContent());
        return dto;
    }

    private void appendLedger(
            Wallet wallet,
            WalletLedgerDirection direction,
            BigDecimal amount,
            WalletLedgerEntryType entryType,
            Long paymentIntentId,
            Long actorId,
            String note) {
        appendLedger(wallet, direction, amount, entryType, paymentIntentId, actorId, note, null, null);
    }

    private void appendLedger(
            Wallet wallet,
            WalletLedgerDirection direction,
            BigDecimal amount,
            WalletLedgerEntryType entryType,
            Long paymentIntentId,
            Long actorId,
            String note,
            String refType,
            Long refId) {
        WalletLedgerEntry entry = new WalletLedgerEntry();
        entry.setWallet(wallet);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setAvailableAfter(wallet.getAvailableBalance());
        entry.setHeldAfter(wallet.getHeldBalance());
        entry.setEntryType(entryType);
        if (StringUtils.hasText(refType)) {
            entry.setRefType(refType.trim());
            entry.setRefId(refId);
        } else if (paymentIntentId != null) {
            entry.setRefType("PAYMENT_INTENT");
            entry.setRefId(paymentIntentId);
        } else {
            entry.setRefType("ADMIN_ADJUSTMENT");
            entry.setRefId(actorId);
        }
        entry.setPaymentIntentId(paymentIntentId);
        entry.setActorId(actorId);
        entry.setNote(note == null ? "" : note);
        ledgerEntryRepository.save(entry);
    }

    private WalletDto.WalletLedgerEntryDto toLedgerDto(WalletLedgerEntry entry) {
        WalletDto.WalletLedgerEntryDto dto = new WalletDto.WalletLedgerEntryDto();
        dto.setId(entry.getId());
        dto.setDirection(entry.getDirection());
        dto.setAmount(entry.getAmount());
        dto.setAvailableAfter(entry.getAvailableAfter());
        dto.setHeldAfter(entry.getHeldAfter());
        dto.setEntryType(entry.getEntryType());
        dto.setNote(entry.getNote());
        dto.setPaymentIntentId(entry.getPaymentIntentId());
        dto.setCreatedAt(entry.getCreatedAt());
        return dto;
    }

    private WalletDto.PaymentIntentSummaryDto toPaymentDto(PaymentIntent intent) {
        WalletDto.PaymentIntentSummaryDto dto = new WalletDto.PaymentIntentSummaryDto();
        dto.setId(intent.getId());
        dto.setPurpose(intent.getPurpose());
        dto.setAmountIrt(intent.getAmountIrt());
        dto.setStatus(intent.getStatus());
        dto.setAuthority(intent.getAuthority());
        dto.setRefId(intent.getRefId());
        dto.setGatewayCode(intent.getGatewayCode());
        dto.setFailureReason(intent.getFailureReason());
        dto.setFailedAt(intent.getFailedAt());
        dto.setVerifiedAt(intent.getVerifiedAt());
        dto.setCreatedAt(intent.getCreatedAt());
        return dto;
    }
}
