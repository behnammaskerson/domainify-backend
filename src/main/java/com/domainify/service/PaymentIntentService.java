package com.domainify.service;

import com.domainify.dto.PaymentVerifyResultDto;
import com.domainify.dto.WalletTopUpResponse;
import com.domainify.entity.MarketplaceOrderPaymentMethod;
import com.domainify.entity.PaymentIntent;
import com.domainify.entity.PaymentIntentPurpose;
import com.domainify.entity.PaymentIntentStatus;
import com.domainify.entity.PaymentSettings;
import com.domainify.entity.User;
import com.domainify.entity.Wallet;
import com.domainify.entity.WalletLedgerEntryType;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.exception.PaymentGatewayException;
import com.domainify.repository.PaymentIntentRepository;
import com.domainify.service.zarinpal.ZarinPalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class PaymentIntentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentIntentService.class);

    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentIntentPersistence paymentIntentPersistence;
    private final PaymentSettingsService paymentSettingsService;
    private final WalletService walletService;
    private final ZarinPalClient zarinPalClient;
    private final NotificationService notificationService;
    private final MarketplaceOrderService marketplaceOrderService;

    public PaymentIntentService(
            PaymentIntentRepository paymentIntentRepository,
            PaymentIntentPersistence paymentIntentPersistence,
            PaymentSettingsService paymentSettingsService,
            WalletService walletService,
            ZarinPalClient zarinPalClient,
            NotificationService notificationService,
            @Lazy MarketplaceOrderService marketplaceOrderService) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentIntentPersistence = paymentIntentPersistence;
        this.paymentSettingsService = paymentSettingsService;
        this.walletService = walletService;
        this.zarinPalClient = zarinPalClient;
        this.notificationService = notificationService;
        this.marketplaceOrderService = marketplaceOrderService;
    }

    public WalletTopUpResponse startWalletTopUp(User user, long amountIrt) {
        paymentSettingsService.requireEnabledConfigured();
        PaymentSettings settings = paymentSettingsService.getOrCreate();

        if (amountIrt < settings.getMinTopUpIrt()) {
            throw new ApiException(ErrorCode.PAYMENT_AMOUNT_TOO_LOW);
        }

        PaymentIntent intent = paymentIntentPersistence.createTopUpIntent(user, amountIrt);

        String callbackUrl = paymentSettingsService.resolveCallbackUrl();
        String mobile = buildMobile(user);
        try {
            ZarinPalClient.RequestResult zp = zarinPalClient.requestPayment(
                    settings.isSandbox(),
                    settings.getMerchantId(),
                    amountIrt,
                    "Domainify wallet top-up #" + intent.getId(),
                    callbackUrl,
                    user.getEmail(),
                    mobile,
                    String.valueOf(intent.getId()));

            intent = paymentIntentPersistence.markRedirected(intent.getId(), zp);

            WalletTopUpResponse response = new WalletTopUpResponse();
            response.setPaymentIntentId(intent.getId());
            response.setAmountIrt(amountIrt);
            response.setAuthority(zp.authority());
            response.setStartPayUrl(zarinPalClient.startPayUrl(settings.isSandbox(), zp.authority()));
            return response;
        } catch (PaymentGatewayException ex) {
            paymentIntentPersistence.markFailed(intent.getId(), ex.getGatewayCode(), ex.getGatewayMessage());
            notifyTopUpFailedSafely(user, intent.getId(), ex.getGatewayMessage());
            throw ex;
        } catch (RuntimeException ex) {
            paymentIntentPersistence.markFailed(intent.getId(), null, ex.getMessage());
            notifyTopUpFailedSafely(user, intent.getId(), ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public PaymentVerifyResultDto verifyForUser(User user, String authority) {
        if (!StringUtils.hasText(authority)) {
            throw new ApiException(ErrorCode.PAYMENT_AUTHORITY_REQUIRED);
        }
        String normalized = authority.trim();

        PaymentIntent intent = paymentIntentRepository.findByAuthorityAndUserId(normalized, user.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_INTENT_NOT_FOUND));

        if (intent.getStatus() == PaymentIntentStatus.VERIFIED) {
            PaymentIntentPurpose purpose = intent.getPurpose();
            if ((purpose == PaymentIntentPurpose.BUY_NOW || purpose == PaymentIntentPurpose.OFFER_SETTLEMENT)
                    && intent.getOrderId() != null) {
                marketplaceOrderService.completePaidOrder(
                        intent.getOrderId(),
                        intent.getId(),
                        MarketplaceOrderPaymentMethod.ZARINPAL);
            }
            return toVerifyResult(intent, walletService.getOrCreate(user), true);
        }

        PaymentIntentPurpose purpose = intent.getPurpose();
        boolean walletTopUp = purpose == PaymentIntentPurpose.WALLET_TOP_UP;
        boolean orderPay = purpose == PaymentIntentPurpose.BUY_NOW
                || purpose == PaymentIntentPurpose.OFFER_SETTLEMENT;
        if (!walletTopUp && !orderPay) {
            throw new ApiException(ErrorCode.PAYMENT_PURPOSE_UNSUPPORTED);
        }

        paymentSettingsService.requireEnabledConfigured();
        PaymentSettings settings = paymentSettingsService.getOrCreate();

        PaymentIntent verified;
        ZarinPalClient.VerifyResult zp;
        try {
            zp = zarinPalClient.verifyPayment(
                    settings.isSandbox(),
                    settings.getMerchantId(),
                    intent.getAmountIrt(),
                    normalized);

            verified = paymentIntentPersistence.markVerified(intent.getId(), zp);
        } catch (PaymentGatewayException ex) {
            paymentIntentPersistence.markFailed(intent.getId(), ex.getGatewayCode(), ex.getGatewayMessage());
            if (walletTopUp) {
                notifyTopUpFailedSafely(user, intent.getId(), ex.getGatewayMessage());
            }
            throw ex;
        } catch (RuntimeException ex) {
            paymentIntentPersistence.markFailed(intent.getId(), null, ex.getMessage());
            if (walletTopUp) {
                notifyTopUpFailedSafely(user, intent.getId(), ex.getMessage());
            }
            throw ex;
        }

        Wallet wallet;
        if (walletTopUp) {
            wallet = walletService.creditAvailable(
                    user,
                    BigDecimal.valueOf(verified.getAmountIrt()),
                    WalletLedgerEntryType.TOP_UP,
                    verified.getId(),
                    user.getId(),
                    "Wallet top-up");
            try {
                notificationService.notifyPaymentTopUpSuccess(user, verified);
            } catch (Exception ex) {
                log.warn("Payment top-up success notification failed for intent {}: {}",
                        verified.getId(), ex.getMessage());
            }
        } else {
            if (verified.getOrderId() == null) {
                throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
            }
            marketplaceOrderService.completePaidOrder(
                    verified.getOrderId(),
                    verified.getId(),
                    MarketplaceOrderPaymentMethod.ZARINPAL);
            wallet = walletService.getOrCreate(user);
        }

        return toVerifyResult(verified, wallet, zp.alreadyVerifiedAtGateway());
    }

    @Transactional
    public void markCancelledForUser(User user, String authority) {
        if (!StringUtils.hasText(authority)) {
            return;
        }
        paymentIntentRepository.findByAuthorityAndUserId(authority.trim(), user.getId())
                .ifPresent(intent -> {
                    PaymentIntentStatus prior = intent.getStatus();
                    boolean wasCancellable = prior == PaymentIntentStatus.CREATED
                            || prior == PaymentIntentStatus.REDIRECTED;
                    paymentIntentPersistence.markCancelled(
                            intent.getId(),
                            "Cancelled or rejected at gateway (Status=NOK)");
                    if (wasCancellable && intent.getPurpose() == PaymentIntentPurpose.WALLET_TOP_UP) {
                        try {
                            PaymentIntent cancelled = paymentIntentRepository.findById(intent.getId())
                                    .orElse(intent);
                            notificationService.notifyPaymentTopUpCancelled(user, cancelled);
                        } catch (Exception ex) {
                            log.warn("Payment top-up cancelled notification failed for intent {}: {}",
                                    intent.getId(), ex.getMessage());
                        }
                    }
                });
    }

    private void notifyTopUpFailedSafely(User user, Long intentId, String reason) {
        try {
            PaymentIntent failed = paymentIntentRepository.findById(intentId).orElse(null);
            if (failed != null) {
                if (StringUtils.hasText(reason) && !StringUtils.hasText(failed.getFailureReason())) {
                    failed.setFailureReason(reason);
                }
                notificationService.notifyPaymentTopUpFailed(user, failed);
            }
        } catch (Exception ex) {
            log.warn("Payment top-up failed notification skipped for intent {}: {}", intentId, ex.getMessage());
        }
    }

    private PaymentVerifyResultDto toVerifyResult(PaymentIntent intent, Wallet wallet, boolean alreadyVerified) {
        PaymentVerifyResultDto dto = new PaymentVerifyResultDto();
        dto.setPaymentIntentId(intent.getId());
        dto.setPurpose(intent.getPurpose());
        dto.setStatus(intent.getStatus());
        dto.setAmountIrt(intent.getAmountIrt());
        dto.setRefId(intent.getRefId());
        dto.setAvailableBalance(wallet.getAvailableBalance());
        dto.setHeldBalance(wallet.getHeldBalance());
        dto.setAlreadyVerified(alreadyVerified);
        dto.setVerifiedAt(intent.getVerifiedAt());
        return dto;
    }

    private String buildMobile(User user) {
        if (!StringUtils.hasText(user.getPhoneCountryCode()) || !StringUtils.hasText(user.getPhoneNumber())) {
            return null;
        }
        return (user.getPhoneCountryCode().trim() + user.getPhoneNumber().trim()).replaceAll("[^0-9+]", "");
    }
}
