package com.domainify.repository;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicketAndInternalNoteFalseOrderByCreatedAtAscIdAsc(Ticket ticket);

    List<TicketMessage> findByTicketOrderByCreatedAtAscIdAsc(Ticket ticket);

    List<TicketMessage> findByTicketIdAndIdInOrderByCreatedAtAscIdAsc(Long ticketId, List<Long> ids);

    Optional<TicketMessage> findByIdAndTicketId(Long id, Long ticketId);

    boolean existsByAuthorId(Long authorId);
}
