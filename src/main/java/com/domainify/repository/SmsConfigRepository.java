package com.domainify.repository;

import com.domainify.entity.SmsConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsConfigRepository extends JpaRepository<SmsConfig, Long> {
}
