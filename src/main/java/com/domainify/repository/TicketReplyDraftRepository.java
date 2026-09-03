package com.domainify.repository;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketReplyDraft;
import com.domainify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketReplyDraftRepository extends JpaRepository<TicketReplyDraft, Long> {

    Optional<TicketReplyDraft> findByTicketAndAuthor(Ticket ticket, User author);

    void deleteByTicketAndAuthor(Ticket ticket, User author);
}
