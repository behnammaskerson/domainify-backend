package com.domainify.repository;

import com.domainify.entity.TicketAgentQueueMembership;
import com.domainify.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketAgentQueueMembershipRepository extends JpaRepository<TicketAgentQueueMembership, Long> {

    List<TicketAgentQueueMembership> findByQueueId(Long queueId);

    @Query("""
            SELECT m.user FROM TicketAgentQueueMembership m
            WHERE m.queue.id = :queueId
              AND m.user.role = :role
              AND m.user.enabled = true
            ORDER BY m.user.firstName ASC, m.user.lastName ASC, m.user.id ASC
            """)
    List<User> findEnabledAgentsByQueueId(
            @Param("queueId") Long queueId,
            @Param("role") User.Role role);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TicketAgentQueueMembership m WHERE m.queue.id = :queueId")
    void deleteByQueueId(@Param("queueId") Long queueId);

    @Query("SELECT m.user.id FROM TicketAgentQueueMembership m WHERE m.queue.id = :queueId")
    List<Long> findUserIdsByQueueId(@Param("queueId") Long queueId);

    @Query("SELECT m.queue.id FROM TicketAgentQueueMembership m WHERE m.user.id = :userId")
    List<Long> findQueueIdsByUserId(@Param("userId") Long userId);
}
