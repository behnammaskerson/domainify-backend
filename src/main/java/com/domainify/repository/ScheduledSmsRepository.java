package com.domainify.repository;

import com.domainify.entity.ScheduledSms;
import com.domainify.entity.ScheduledSmsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduledSmsRepository extends JpaRepository<ScheduledSms, Long>, JpaSpecificationExecutor<ScheduledSms> {

    Optional<ScheduledSms> findByPackId(String packId);

    List<ScheduledSms> findByStatusAndScheduledAtBefore(ScheduledSmsStatus status, Instant scheduledAt);
}
