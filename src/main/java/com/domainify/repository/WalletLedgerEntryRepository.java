package com.domainify.repository;

import com.domainify.entity.WalletLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, Long> {

    Page<WalletLedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    boolean existsByPaymentIntentIdAndEntryType(
            Long paymentIntentId,
            com.domainify.entity.WalletLedgerEntryType entryType);

    Optional<WalletLedgerEntry> findFirstByPaymentIntentIdAndEntryType(
            Long paymentIntentId,
            com.domainify.entity.WalletLedgerEntryType entryType);
}
