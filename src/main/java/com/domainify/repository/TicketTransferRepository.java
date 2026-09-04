package com.domainify.repository;

import com.domainify.entity.TicketTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketTransferRepository extends JpaRepository<TicketTransfer, Long> {

    List<TicketTransfer> findByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);
}
