package com.domainify.repository;

import com.domainify.entity.DomainListing;
import com.domainify.entity.DomainListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DomainListingRepository extends JpaRepository<DomainListing, Long>, JpaSpecificationExecutor<DomainListing> {

    Optional<DomainListing> findByIdAndSellerId(Long id, Long sellerId);

    Optional<DomainListing> findByDomainId(Long domainId);

    boolean existsByDomainId(Long domainId);

    @Query("""
            select l from DomainListing l
            join fetch l.domain d
            join fetch d.category
            join fetch l.seller
            where l.id = :id and l.status = :status
            """)
    Optional<DomainListing> findPublicById(
            @Param("id") Long id,
            @Param("status") DomainListingStatus status);

    long countBySellerIdAndStatus(Long sellerId, DomainListingStatus status);
}
