package com.domainify.repository;

import com.domainify.entity.TicketEscalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketEscalationRepository extends JpaRepository<TicketEscalation, Long> {

    List<TicketEscalation> findByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);
}
