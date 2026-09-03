package com.domainify.repository;

import com.domainify.entity.TicketStatus;
import com.domainify.entity.TicketStatusDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketStatusDefinitionRepository extends JpaRepository<TicketStatusDefinition, TicketStatus> {

    List<TicketStatusDefinition> findAllByOrderBySortOrderAscStatusAsc();

    List<TicketStatusDefinition> findByActiveTrueOrderBySortOrderAscStatusAsc();
}
