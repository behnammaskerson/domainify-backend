package com.domainify.repository;

import com.domainify.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    List<TicketCategory> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<TicketCategory> findAllByOrderBySortOrderAscNameAsc();

    Optional<TicketCategory> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
