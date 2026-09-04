package com.domainify.repository;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketQueue;
import com.domainify.entity.TicketTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
