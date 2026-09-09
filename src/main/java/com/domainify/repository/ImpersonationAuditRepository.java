package com.domainify.repository;

import com.domainify.entity.ImpersonationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImpersonationAuditRepository extends JpaRepository<ImpersonationAudit, Long> {

    Optional<ImpersonationAudit> findByIdAndEndedAtIsNull(Long id);
}
