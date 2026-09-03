package com.domainify.repository;

import com.domainify.entity.Ticket;
import com.domainify.entity.TicketCategory;
import com.domainify.entity.TicketTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    long countByCategory(TicketCategory category);

    Optional<Ticket> findByIdAndRequesterId(Long id, Long requesterId);

    @Query("select count(t) from Ticket t join t.tags tag where tag = :tag")
    long countByTag(@Param("tag") TicketTag tag);
}
