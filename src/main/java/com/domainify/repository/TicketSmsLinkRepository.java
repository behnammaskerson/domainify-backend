package com.domainify.repository;

import com.domainify.entity.TicketSmsLink;
import com.domainify.entity.TicketSmsLinkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketSmsLinkRepository extends JpaRepository<TicketSmsLink, Long> {

    boolean existsByTicketIdAndLinkTypeAndExternalId(Long ticketId, TicketSmsLinkType linkType, String externalId);

    List<TicketSmsLink> findByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);

    @Query("select l.externalId from TicketSmsLink l where l.ticket.id = :ticketId and l.linkType = :linkType")
    List<String> findExternalIdsByTicketIdAndLinkType(
            @Param("ticketId") Long ticketId,
            @Param("linkType") TicketSmsLinkType linkType);

    @Modifying
    @Query("delete from TicketSmsLink l where l.ticket.id = :ticketId and l.id = :linkId")
    int deleteByTicketIdAndId(@Param("ticketId") Long ticketId, @Param("linkId") Long linkId);
}
