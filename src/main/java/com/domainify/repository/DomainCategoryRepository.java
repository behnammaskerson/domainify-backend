package com.domainify.repository;

import com.domainify.entity.DomainCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DomainCategoryRepository extends JpaRepository<DomainCategory, Long> {

    Optional<DomainCategory> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<DomainCategory> findAllByOrderBySortOrderAscNameAsc();

    List<DomainCategory> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<DomainCategory> findByParentIdOrderBySortOrderAscNameAsc(Long parentId);

    long countByParentId(Long parentId);
}
