package com.domainify.repository;

import com.domainify.entity.TicketReplyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketReplyTemplateRepository extends JpaRepository<TicketReplyTemplate, Long> {

    List<TicketReplyTemplate> findAllByOrderBySortOrderAscTitleAsc();

    List<TicketReplyTemplate> findByActiveTrueOrderBySortOrderAscTitleAsc();

    boolean existsByTitleIgnoreCase(String title);

    Optional<TicketReplyTemplate> findByTitleIgnoreCase(String title);
}
