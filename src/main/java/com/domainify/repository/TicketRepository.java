package com.domainify.repository;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    long countByCategory(TicketCategory category);
}
