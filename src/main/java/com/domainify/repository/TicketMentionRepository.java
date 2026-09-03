package com.domainify.repository;

import com.domainify.entity.TicketMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketMentionRepository extends JpaRepository<TicketMention, Long> {

    boolean existsByTicketIdAndMentionedUserIdAndMessageId(Long ticketId, Long mentionedUserId, Long messageId);
}
