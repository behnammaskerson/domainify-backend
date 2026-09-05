package com.domainify.repository;

import com.domainify.entity.Domain;
import com.domainify.entity.DomainStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByOwnerId(Long ownerId);

    long countByCategoryId(Long categoryId);
}
