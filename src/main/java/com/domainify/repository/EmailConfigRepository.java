package com.domainify.repository;

import com.domainify.entity.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {
}
