package com.domainify.repository;

import com.domainify.entity.PaymentSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {
}
