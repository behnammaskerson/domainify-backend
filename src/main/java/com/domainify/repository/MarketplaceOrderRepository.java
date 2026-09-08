package com.domainify.repository;

import com.domainify.entity.MarketplaceOrder;
import com.domainify.entity.MarketplaceOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, Long>,
        JpaSpecificationExecutor<MarketplaceOrder> {

    Page<MarketplaceOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    Page<MarketplaceOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    List<MarketplaceOrder> findByListingIdAndStatus(Long listingId, MarketplaceOrderStatus status);

    Optional<MarketplaceOrder> findById(Long id);

    boolean existsByListingIdAndStatus(Long listingId, MarketplaceOrderStatus status);

    @Query("""
            SELECT o FROM MarketplaceOrder o
            WHERE o.status = :status
              AND o.paymentDeadline IS NOT NULL
              AND o.paymentDeadline < :before
            """)
    List<MarketplaceOrder> findStalePendingBefore(
            @Param("status") MarketplaceOrderStatus status,
            @Param("before") Instant before);
}
