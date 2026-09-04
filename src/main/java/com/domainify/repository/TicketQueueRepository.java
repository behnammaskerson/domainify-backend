package com.domainify.repository;

import com.domainify.entity.TicketQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketQueueRepository extends JpaRepository<TicketQueue, Long> {

    List<TicketQueue> findAllByOrderBySortOrderAscNameAsc();

    List<TicketQueue> findByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<TicketQueue> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
