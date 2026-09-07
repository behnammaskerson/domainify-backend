package com.domainify.repository;

import com.domainify.entity.DomainListingOffer;
import com.domainify.entity.DomainListingOfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DomainListingOfferRepository
        extends JpaRepository<DomainListingOffer, Long>, JpaSpecificationExecutor<DomainListingOffer> {

    Optional<DomainListingOffer> findByIdAndBuyerId(Long id, Long buyerId);

    Optional<DomainListingOffer> findByIdAndSellerId(Long id, Long sellerId);

    boolean existsByListingIdAndBuyerIdAndStatusIn(
            Long listingId,
            Long buyerId,
            Collection<DomainListingOfferStatus> statuses);

    List<DomainListingOffer> findByListingIdAndStatusInAndIdNot(
            Long listingId,
            Collection<DomainListingOfferStatus> statuses,
            Long excludeId);

    long countByListingIdAndStatusIn(Long listingId, Collection<DomainListingOfferStatus> statuses);

    @Query("""
            select distinct o from DomainListingOffer o
            join fetch o.listing l
            join fetch l.domain
            join fetch o.buyer
            join fetch o.seller
            where o.status in :statuses and o.updatedAt < :before
            """)
    List<DomainListingOffer> findStaleOpenOffers(
            @Param("statuses") Collection<DomainListingOfferStatus> statuses,
            @Param("before") Instant before);
}
