package com.domainify.repository;

import com.domainify.entity.TicketSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketSettingsRepository extends JpaRepository<TicketSettings, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TicketSettings s WHERE s.id = :id")
    Optional<TicketSettings> findByIdForUpdate(@Param("id") Long id);
}
