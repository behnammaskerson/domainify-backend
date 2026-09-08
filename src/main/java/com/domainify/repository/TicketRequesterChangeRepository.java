package com.domainify.repository;

import com.domainify.entity.TicketRequesterChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRequesterChangeRepository extends JpaRepository<TicketRequesterChange, Long> {

    List<TicketRequesterChange> findByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);
}
