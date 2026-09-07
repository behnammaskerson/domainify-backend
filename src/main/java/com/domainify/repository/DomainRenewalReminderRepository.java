package com.domainify.repository;

import com.domainify.entity.DomainRenewalReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DomainRenewalReminderRepository extends JpaRepository<DomainRenewalReminder, Long> {

    boolean existsByDomainIdAndWindowDaysAndExpiresAt(Long domainId, int windowDays, LocalDate expiresAt);
}
