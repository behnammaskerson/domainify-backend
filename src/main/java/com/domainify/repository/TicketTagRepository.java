package com.domainify.repository;

import com.domainify.entity.TicketTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketTagRepository extends JpaRepository<TicketTag, Long> {

    List<TicketTag> findAllByOrderByNameAsc();

    Optional<TicketTag> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
