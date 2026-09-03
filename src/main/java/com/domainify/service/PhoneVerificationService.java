package com.domainify.service;

import com.domainify.dto.SmsBulkSendRequest;
import com.domainify.dto.SmsBulkSendResultDto;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.UserRepository;
import com.domainify.util.PhoneSmsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class PhoneVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PhoneVerificationService.class);
    private static final int OTP_LENGTH = 6;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final SmsConfigService smsConfigService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.phone-verify-otp-expiration-minutes:10}")
    private long otpExpirationMinutes;

    @Value("${app.phone-verify-resend-seconds:60}")
    private long resendCooldownSeconds;

    public PhoneVerificationService(
            UserRepository userRepository,
            SmsService smsService,
            SmsConfigService smsConfigService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.smsService = smsService;
        this.smsConfigService = smsConfigService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendVerificationCode(User user) {
        if (user == null || user.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.hasPhoneNumber()) {
            throw new ApiException(ErrorCode.PHONE_NOT_SET);
        }
        if (user.isPhoneVerified()) {
            throw new ApiException(ErrorCode.PHONE_ALREADY_VERIFIED);
        }
        if (!isSmsConfigured()) {
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_DISABLED);
        }

        Instant now = Instant.now();
        if (user.getPhoneVerificationOtpSentAt() != null) {
            Duration cooldown = Duration.ofSeconds(resendCooldownSeconds);
            Instant nextAllowed = user.getPhoneVerificationOtpSentAt().plus(cooldown);
            if (now.isBefore(nextAllowed)) {
                throw new ApiException(ErrorCode.PHONE_VERIFICATION_COOLDOWN);
            }
        }

        String smsMobile = PhoneSmsUtil.toSmsMobile(user.getPhoneCountryCode(), user.getPhoneNumber());
        if (!StringUtils.hasText(smsMobile)) {
            throw new ApiException(ErrorCode.PHONE_INVALID);
        }

        String otp = generateOtp();
        user.setPhoneVerificationOtpHash(passwordEncoder.encode(otp));
        user.setPhoneVerificationOtpExpiresAt(now.plus(Duration.ofMinutes(otpExpirationMinutes)));
        user.setPhoneVerificationOtpSentAt(now);
        user.setPhoneVerificationOtpAttempts(0);
        userRepository.save(user);

        String message = "Your Domainify verification code is: " + otp;
        SmsBulkSendRequest request = new SmsBulkSendRequest();
        request.setMobiles(List.of(smsMobile));
        request.setMessageText(message);

        try {
            SmsBulkSendResultDto result = smsService.sendBulk(request);
            if (!result.isSuccess()) {
                user.clearPhoneVerificationOtp();
                userRepository.save(user);
                throw new ApiException(ErrorCode.PHONE_VERIFICATION_SEND_FAILED);
            }
        } catch (ApiException ex) {
            user.clearPhoneVerificationOtp();
            userRepository.save(user);
            throw ex;
        } catch (Exception ex) {
            user.clearPhoneVerificationOtp();
            userRepository.save(user);
            log.warn("Failed to send phone verification SMS to user {}: {}", user.getId(), ex.getMessage());
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_SEND_FAILED);
        }
    }

    @Transactional
    public void verifyCode(User user, String code) {
        if (user == null || user.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.hasPhoneNumber()) {
            throw new ApiException(ErrorCode.PHONE_NOT_SET);
        }
        if (user.isPhoneVerified()) {
            throw new ApiException(ErrorCode.PHONE_ALREADY_VERIFIED);
        }

        String normalizedCode = normalizeCode(code);
        if (!StringUtils.hasText(normalizedCode)) {
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_INVALID);
        }

        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (!StringUtils.hasText(fresh.getPhoneVerificationOtpHash())
                || fresh.getPhoneVerificationOtpExpiresAt() == null) {
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_INVALID);
        }
        if (fresh.getPhoneVerificationOtpExpiresAt().isBefore(Instant.now())) {
            fresh.clearPhoneVerificationOtp();
            userRepository.save(fresh);
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_INVALID);
        }
        if (fresh.getPhoneVerificationOtpAttempts() >= MAX_VERIFY_ATTEMPTS) {
            fresh.clearPhoneVerificationOtp();
            userRepository.save(fresh);
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_TOO_MANY_ATTEMPTS);
        }

        if (!passwordEncoder.matches(normalizedCode, fresh.getPhoneVerificationOtpHash())) {
            fresh.setPhoneVerificationOtpAttempts(fresh.getPhoneVerificationOtpAttempts() + 1);
            userRepository.save(fresh);
            throw new ApiException(ErrorCode.PHONE_VERIFICATION_INVALID);
        }

        fresh.setPhoneVerified(true);
        fresh.setPhoneVerifiedAt(Instant.now());
        fresh.clearPhoneVerificationOtp();
        userRepository.save(fresh);
    }

    public void invalidateAfterPhoneChange(User user) {
        if (user == null) {
            return;
        }
        user.clearPhoneVerificationOtp();
        if (user.hasPhoneNumber()) {
            user.setPhoneVerified(false);
            user.setPhoneVerifiedAt(null);
        } else {
            user.setPhoneVerified(true);
            user.setPhoneVerifiedAt(null);
        }
    }

    private boolean isSmsConfigured() {
        return StringUtils.hasText(smsConfigService.getApiKey())
                && StringUtils.hasText(smsConfigService.getDefaultLine());
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int floor = bound / 10;
        int value = secureRandom.nextInt(bound - floor) + floor;
        return String.format(Locale.ROOT, "%0" + OTP_LENGTH + "d", value);
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            return "";
        }
        return code.replaceAll("\\D", "");
    }
}
