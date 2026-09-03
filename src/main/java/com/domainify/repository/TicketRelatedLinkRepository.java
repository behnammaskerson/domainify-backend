package com.domainify.repository;

import com.domainify.entity.TicketRelatedLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRelatedLinkRepository extends JpaRepository<TicketRelatedLink, Long> {

    boolean existsByTicketIdAndRelatedTicketId(Long ticketId, Long relatedTicketId);

    List<TicketRelatedLink> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    @Modifying
    @Query("""
            delete from TicketRelatedLink l
            where (l.ticket.id = :ticketId and l.relatedTicket.id = :relatedTicketId)
               or (l.ticket.id = :relatedTicketId and l.relatedTicket.id = :ticketId)
            """)
    void deleteBidirectional(@Param("ticketId") Long ticketId, @Param("relatedTicketId") Long relatedTicketId);
}
