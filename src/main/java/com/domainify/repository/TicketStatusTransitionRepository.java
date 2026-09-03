package com.domainify.repository;

import com.domainify.entity.TicketStatus;
import com.domainify.entity.TicketStatusTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketStatusTransitionRepository extends JpaRepository<TicketStatusTransition, Long> {

    List<TicketStatusTransition> findAllByOrderByFromStatusAscToStatusAsc();

    List<TicketStatusTransition> findByFromStatusOrderByToStatusAsc(TicketStatus fromStatus);

    boolean existsByFromStatusAndToStatus(TicketStatus fromStatus, TicketStatus toStatus);

    void deleteAllInBatch();
}
