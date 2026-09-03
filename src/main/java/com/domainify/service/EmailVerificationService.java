package com.domainify.service;

import com.domainify.entity.EmailConfig;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.UserRepository;
import com.domainify.security.JwtUtil;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final UserRepository userRepository;
    private final EmailConfigService emailConfigService;
    private final MailService mailService;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailConfigService emailConfigService,
            MailService mailService,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.emailConfigService = emailConfigService;
        this.mailService = mailService;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        if (user == null || user.getId() == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.isEmailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        if (!emailConfigService.isEnabled()) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_DISABLED);
        }

        String email = normalizeEmail(user.getEmail());
        String token = jwtUtil.generateEmailVerificationToken(user.getId(), email);
        String link = buildVerificationLink(token);
        EmailConfig config = emailConfigService.getOrCreate();

        String subject = "Verify your Domainify email";
        String body = """
                Hello %s,

                Please verify your email address by opening this link:

                %s

                If you did not request this, you can ignore this email.

                — Domainify
                """.formatted(displayName(user), link);

        try {
            mailService.sendEmail(config, email, subject, body);
        } catch (MessagingException ex) {
            log.warn("Failed to send verification email to {}: {}", email, ex.getMessage());
            throw new ApiException(ErrorCode.EMAIL_CONFIG_INVALID);
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }
        if (!jwtUtil.isEmailVerifyToken(token) || jwtUtil.isTokenExpired(token)) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }

        Long userId = jwtUtil.extractUserId(token);
        String email = normalizeEmail(jwtUtil.extractVerifyEmail(token));
        if (userId == null || !StringUtils.hasText(email)) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.EMAIL_VERIFICATION_INVALID));
        if (!email.equalsIgnoreCase(user.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);
    }

    public void sendVerificationEmailSilently(User user) {
        try {
            sendVerificationEmail(user);
        } catch (ApiException ex) {
            log.debug("Skipped verification email for user {}: {}", user.getId(), ex.getCode());
        } catch (Exception ex) {
            log.debug("Skipped verification email for user {}: {}", user.getId(), ex.getMessage());
        }
    }

    /**
     * Resends a verification email for self-registered accounts that are not yet verified.
     * Does nothing when the address is unknown, already verified, or not a registration account.
     */
    @Transactional(readOnly = true)
    public void resendVerificationEmailPublic(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String normalized = normalizeEmail(email);
        userRepository.findByEmail(normalized).ifPresent(user -> {
            if (user.getCreateMethod() != User.CreateMethod.REGISTER || user.isEmailVerified()) {
                return;
            }
            sendVerificationEmail(user);
        });
    }

    private String buildVerificationLink(String token) {
        String base = frontendUrl == null ? "http://localhost:4200" : frontendUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/verify-email?token=" + token;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return StringUtils.hasText(full) ? full : user.getEmail();
    }
}
