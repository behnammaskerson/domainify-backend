package com.domainify.repository;

import com.domainify.entity.TicketMessageRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketMessageRevisionRepository extends JpaRepository<TicketMessageRevision, Long> {

    List<TicketMessageRevision> findByMessageIdOrderByCreatedAtAscIdAsc(Long messageId);

    boolean existsByMessageId(Long messageId);

    boolean existsByTicketIdAndMessageIsNull(Long ticketId);

    List<TicketMessageRevision> findByTicketIdAndMessageIsNullOrderByCreatedAtAscIdAsc(Long ticketId);

    boolean existsByActorId(Long actorId);
}
