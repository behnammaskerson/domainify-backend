package com.domainify.repository;

import com.domainify.entity.KbCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbCategoryRepository extends JpaRepository<KbCategory, Long> {

    Optional<KbCategory> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<KbCategory> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<KbCategory> findAllByOrderBySortOrderAscNameAsc();

    List<KbCategory> findByParentIdOrderBySortOrderAscNameAsc(Long parentId);

    long countByParentId(Long parentId);
}
