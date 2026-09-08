package com.domainify.repository;

import com.domainify.entity.PaymentIntent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {

    Optional<PaymentIntent> findByAuthority(String authority);

    Optional<PaymentIntent> findByAuthorityAndUserId(String authority, Long userId);

    List<PaymentIntent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
