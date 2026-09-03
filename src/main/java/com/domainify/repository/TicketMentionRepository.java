package com.domainify.repository;

import com.domainify.entity.TicketMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketMentionRepository extends JpaRepository<TicketMention, Long> {

    boolean existsByTicketIdAndMentionedUserIdAndMessageId(Long ticketId, Long mentionedUserId, Long messageId);

    List<TicketMention> findByTicketId(Long ticketId);

    void deleteByMessageId(Long messageId);
}
