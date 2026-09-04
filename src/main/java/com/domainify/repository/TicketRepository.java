package com.domainify.repository;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketQueue;
import com.domainify.entity.TicketTag;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    long countByCategory(TicketCategory category);

    long countByQueue(TicketQueue queue);

    Optional<Ticket> findByIdAndRequesterId(Long id, Long requesterId);

    Optional<Ticket> findByPublicNumberIgnoreCase(String publicNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") Long id);

    List<Ticket> findByMergedIntoIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long mergedIntoId);

    List<Ticket> findBySplitFromIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long splitFromId);

    long countBySplitFromIdAndDeletedAtIsNull(Long splitFromId);

    boolean existsByRequesterId(Long requesterId);

    boolean existsByAssigneeId(Long assigneeId);

    @Query("select count(t) from Ticket t join t.tags tag where tag = :tag")
    long countByTag(@Param("tag") TicketTag tag);

    @Query("""
            select t from Ticket t
            where t.status = com.domainify.entity.TicketStatus.CLOSED
              and t.deletedAt is null
              and t.archivedAt is null
              and t.closedAt is not null
              and t.closedAt < :cutoff
            """)
    List<Ticket> findClosedEligibleForAutoArchive(@Param("cutoff") Instant cutoff);

    @Query("""
            select t from Ticket t
            where t.deletedAt is null
              and t.archivedAt is null
              and t.escalatedAt is null
              and t.dueAt is not null
              and t.dueAt < :now
              and t.status in (
                com.domainify.entity.TicketStatus.NEW,
                com.domainify.entity.TicketStatus.OPEN,
                com.domainify.entity.TicketStatus.PENDING,
                com.domainify.entity.TicketStatus.ON_HOLD
              )
            """)
    List<Ticket> findEligibleForSlaEscalation(@Param("now") Instant now);
}
