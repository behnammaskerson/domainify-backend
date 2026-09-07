package com.domainify.repository;

import com.domainify.entity.Domain;
import com.domainify.entity.DomainStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DomainRepository extends JpaRepository<Domain, Long>, JpaSpecificationExecutor<Domain> {

    Optional<Domain> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(Long ownerId, String name, Long id);

    @Query("""
            select d.status, count(d)
            from Domain d
            where d.owner.id = :ownerId
            group by d.status
            """)
    List<Object[]> countByStatusForOwner(@Param("ownerId") Long ownerId);

    @Query("""
            select d from Domain d
            join fetch d.owner
            where d.expiresAt = :expiresAt
              and d.status in :statuses
            """)
    List<Domain> findByExpiresAtAndStatusIn(
            @Param("expiresAt") LocalDate expiresAt,
            @Param("statuses") Collection<DomainStatus> statuses);

    @Query("""
            select d from Domain d
            join fetch d.owner
            where d.expiresAt is not null
              and d.expiresAt > :today
              and d.expiresAt <= :horizon
              and d.status in :statuses
            """)
    List<Domain> findExpiringBetweenAndStatusIn(
            @Param("today") LocalDate today,
            @Param("horizon") LocalDate horizon,
            @Param("statuses") Collection<DomainStatus> statuses);

    @Query("""
            select d from Domain d
            where d.renewalWindows is not null
              and d.renewalWindows <> ''
              and d.expiresAt is not null
              and d.status in :statuses
            """)
    List<Domain> findWithCustomRenewalWindows(@Param("statuses") Collection<DomainStatus> statuses);

    long countByOwnerId(Long ownerId);

    long countByCategoryId(Long categoryId);
}
