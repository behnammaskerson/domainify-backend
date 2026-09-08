package com.domainify.repository;

import com.domainify.entity.TicketDomainLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketDomainLinkRepository extends JpaRepository<TicketDomainLink, Long> {

    boolean existsByTicketIdAndDomainId(Long ticketId, Long domainId);

    List<TicketDomainLink> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

    @Query("select l.domain.id from TicketDomainLink l where l.ticket.id = :ticketId")
    List<Long> findDomainIdsByTicketId(@Param("ticketId") Long ticketId);

    @Modifying
    @Query("delete from TicketDomainLink l where l.ticket.id = :ticketId and l.domain.id = :domainId")
    int deleteByTicketIdAndDomainId(@Param("ticketId") Long ticketId, @Param("domainId") Long domainId);
}
