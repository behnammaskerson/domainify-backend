package com.domainify.repository;

import com.domainify.entity.TicketCsat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketCsatRepository extends JpaRepository<TicketCsat, Long> {

    Optional<TicketCsat> findByTicketId(Long ticketId);

    boolean existsByTicketId(Long ticketId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TicketCsat c WHERE c.ticket.id = :ticketId")
    void deleteByTicketId(@Param("ticketId") Long ticketId);
}
