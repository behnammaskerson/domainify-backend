package com.domainify.service;

import com.domainify.entity.PaymentIntent;
import com.domainify.entity.PaymentIntentPurpose;
import com.domainify.entity.PaymentIntentStatus;
import com.domainify.entity.User;
import com.domainify.repository.PaymentIntentRepository;
import com.domainify.service.zarinpal.ZarinPalClient;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Short independent transactions so failed payment rows survive after the caller rethrows.
 */
@Service
public class PaymentIntentPersistence {

    private static final Logger log = LoggerFactory.getLogger(PaymentIntentPersistence.class);

    private final PaymentIntentRepository paymentIntentRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    public PaymentIntentPersistence(
            PaymentIntentRepository paymentIntentRepository,
            EntityManager entityManager,
            JdbcTemplate jdbcTemplate) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentIntent createTopUpIntent(User user, long amountIrt) {
        PaymentIntent intent = new PaymentIntent();
        intent.setUser(entityManager.getReference(User.class, user.getId()));
        intent.setPurpose(PaymentIntentPurpose.WALLET_TOP_UP);
        intent.setAmountIrt(amountIrt);
        intent.setStatus(PaymentIntentStatus.CREATED);
        intent.setDescription("Wallet top-up");
        PaymentIntent saved = paymentIntentRepository.saveAndFlush(intent);
        log.info("Created payment_intent id={} userId={} amount={}", saved.getId(), user.getId(), amountIrt);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentIntent createOrderIntent(
            User user,
            PaymentIntentPurpose purpose,
            long amountIrt,
            Long orderId,
            Long listingId,
            Long offerId) {
        PaymentIntent intent = new PaymentIntent();
        intent.setUser(entityManager.getReference(User.class, user.getId()));
        intent.setPurpose(purpose);
        intent.setAmountIrt(amountIrt);
        intent.setStatus(PaymentIntentStatus.CREATED);
        intent.setOrderId(orderId);
        intent.setListingId(listingId);
        intent.setOfferId(offerId);
        String label = purpose == PaymentIntentPurpose.OFFER_SETTLEMENT
                ? "Offer settlement"
                : "Buy now";
        intent.setDescription(label + " order #" + orderId);
        PaymentIntent saved = paymentIntentRepository.saveAndFlush(intent);
        log.info("Created order payment_intent id={} purpose={} orderId={} userId={} amount={}",
                saved.getId(), purpose, orderId, user.getId(), amountIrt);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentIntent markRedirected(Long intentId, ZarinPalClient.RequestResult result) {
        PaymentIntent intent = paymentIntentRepository.findById(intentId).orElseThrow();
        intent.setAuthority(result.authority());
        intent.setFee(result.fee());
        intent.setGatewayCode(result.code());
        intent.setFailureReason(null);
        intent.setFailedAt(null);
        intent.setStatus(PaymentIntentStatus.REDIRECTED);
        return paymentIntentRepository.saveAndFlush(intent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long intentId, Integer gatewayCode, String reason) {
        if (intentId == null) {
            return;
        }
        Instant now = Instant.now();
        int updated = jdbcTemplate.update("""
                        UPDATE payment_intents
                           SET status = 'FAILED',
                               gateway_code = ?,
                               failure_reason = ?,
                               failed_at = ?,
                               updated_at = ?
                         WHERE id = ?
                           AND status <> 'VERIFIED'
                        """,
                gatewayCode,
                truncate(reason),
                Timestamp.from(now),
                Timestamp.from(now),
                intentId);
        log.info("Marked payment_intent id={} FAILED rowsUpdated={} code={} reason={}",
                intentId, updated, gatewayCode, truncate(reason));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelled(Long intentId, String reason) {
        if (intentId == null) {
            return;
        }
        Instant now = Instant.now();
        int updated = jdbcTemplate.update("""
                        UPDATE payment_intents
                           SET status = 'CANCELLED',
                               failure_reason = ?,
                               failed_at = ?,
                               updated_at = ?
                         WHERE id = ?
                           AND status IN ('CREATED', 'REDIRECTED')
                        """,
                truncate(reason),
                Timestamp.from(now),
                Timestamp.from(now),
                intentId);
        log.info("Marked payment_intent id={} CANCELLED rowsUpdated={}", intentId, updated);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentIntent markVerified(
            Long intentId,
            ZarinPalClient.VerifyResult result) {
        PaymentIntent intent = paymentIntentRepository.findById(intentId).orElseThrow();
        intent.setRefId(result.refId());
        intent.setFee(result.fee());
        intent.setCardPan(result.cardPan());
        intent.setGatewayCode(result.code());
        intent.setFailureReason(null);
        intent.setFailedAt(null);
        intent.setVerifiedAt(Instant.now());
        intent.setStatus(PaymentIntentStatus.VERIFIED);
        return paymentIntentRepository.saveAndFlush(intent);
    }

    private String truncate(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.length() <= 1000 ? trimmed : trimmed.substring(0, 1000);
    }
}
