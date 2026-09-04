package com.domainify.repository;

import com.domainify.entity.TicketWatcher;
import com.domainify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketWatcherRepository extends JpaRepository<TicketWatcher, Long> {

    boolean existsByTicketIdAndUserId(Long ticketId, Long userId);

    Optional<TicketWatcher> findByTicketIdAndUserId(Long ticketId, Long userId);

    List<TicketWatcher> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    void deleteByTicketIdAndUserId(Long ticketId, Long userId);

    @Query("select w.user from TicketWatcher w where w.ticket.id = :ticketId")
    List<User> findUsersByTicketId(@Param("ticketId") Long ticketId);
}
