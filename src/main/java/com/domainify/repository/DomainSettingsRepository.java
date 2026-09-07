package com.domainify.repository;

import com.domainify.entity.DomainSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainSettingsRepository extends JpaRepository<DomainSettings, Long> {
}
