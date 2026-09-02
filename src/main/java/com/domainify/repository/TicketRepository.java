package com.domainify.repository;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    long countByCategory(TicketCategory category);

    java.util.Optional<Ticket> findByIdAndRequesterId(Long id, Long requesterId);
}
