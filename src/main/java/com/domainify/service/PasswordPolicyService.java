package com.domainify.service;

import com.domainify.dto.PasswordPolicyDto;
import com.domainify.entity.PasswordHistory;
import com.domainify.entity.PasswordPolicy;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.PasswordHistoryRepository;
import com.domainify.repository.PasswordPolicyRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PasswordPolicyService {

    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:'\",.<>?/\\`~";

    private final PasswordPolicyRepository policyRepository;
    private final PasswordHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyService(PasswordPolicyRepository policyRepository,
                                 PasswordHistoryRepository historyRepository,
                                 PasswordEncoder passwordEncoder) {
        this.policyRepository = policyRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PasswordPolicy getOrCreate() {
        return policyRepository.findById(PasswordPolicy.SINGLETON_ID)
                .orElseGet(() -> policyRepository.save(PasswordPolicy.defaults()));
    }

    @Transactional
    public PasswordPolicyDto getDto() {
        return toDto(getOrCreate());
    }

    @Transactional
    public PasswordPolicyDto update(PasswordPolicyDto request) {
        if (request.getMinLength() > request.getMaxLength()) {
            throw new ApiException(ErrorCode.PASSWORD_POLICY_INVALID);
        }

        PasswordPolicy policy = getOrCreate();
        policy.setMinLength(request.getMinLength());
        policy.setMaxLength(request.getMaxLength());
        policy.setRequireUppercase(Boolean.TRUE.equals(request.getRequireUppercase()));
        policy.setRequireLowercase(Boolean.TRUE.equals(request.getRequireLowercase()));
        policy.setRequireDigit(Boolean.TRUE.equals(request.getRequireDigit()));
        policy.setRequireSpecial(Boolean.TRUE.equals(request.getRequireSpecial()));
        policy.setExpiryDays(request.getExpiryDays());
        policy.setHistoryCount(request.getHistoryCount());
        return toDto(policyRepository.save(policy));
    }

    public boolean isPasswordExpired(User user) {
        PasswordPolicy policy = getOrCreate();
        if (policy.getExpiryDays() <= 0) {
            return false;
        }
        Instant changedAt = user.getEffectivePasswordChangedAt();
        Instant expiresAt = changedAt.plus(policy.getExpiryDays(), ChronoUnit.DAYS);
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Validates the raw password against the active policy and (when applicable) history,
     * then encodes and assigns it on the user. Callers must persist the user afterwards.
     */
    @Transactional
    public void applyNewPassword(User user, String rawPassword) {
        PasswordPolicy policy = getOrCreate();
        validateComplexity(rawPassword, policy);
        validateNotReused(user, rawPassword, policy);

        String previousHash = user.getPassword();
        if (user.getId() != null && StringUtils.hasText(previousHash) && policy.getHistoryCount() > 0) {
            PasswordHistory entry = new PasswordHistory();
            entry.setUser(user);
            entry.setPasswordHash(previousHash);
            historyRepository.save(entry);
            trimHistory(user.getId(), policy.getHistoryCount());
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPasswordChangedAt(Instant.now());
    }

    public void validateComplexity(String rawPassword) {
        validateComplexity(rawPassword, getOrCreate());
    }

    private void validateComplexity(String rawPassword, PasswordPolicy policy) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new ApiException(ErrorCode.PASSWORD_TOO_SHORT, policy.getMinLength());
        }
        if (rawPassword.length() < policy.getMinLength()) {
            throw new ApiException(ErrorCode.PASSWORD_TOO_SHORT, policy.getMinLength());
        }
        if (rawPassword.length() > policy.getMaxLength()) {
            throw new ApiException(ErrorCode.PASSWORD_TOO_LONG, policy.getMaxLength());
        }
        if (policy.isRequireUppercase() && !rawPassword.chars().anyMatch(Character::isUpperCase)) {
            throw new ApiException(ErrorCode.PASSWORD_MISSING_UPPERCASE);
        }
        if (policy.isRequireLowercase() && !rawPassword.chars().anyMatch(Character::isLowerCase)) {
            throw new ApiException(ErrorCode.PASSWORD_MISSING_LOWERCASE);
        }
        if (policy.isRequireDigit() && !rawPassword.chars().anyMatch(Character::isDigit)) {
            throw new ApiException(ErrorCode.PASSWORD_MISSING_DIGIT);
        }
        if (policy.isRequireSpecial() && rawPassword.chars().noneMatch(c -> SPECIAL_CHARS.indexOf(c) >= 0)) {
            throw new ApiException(ErrorCode.PASSWORD_MISSING_SPECIAL);
        }
    }

    private void validateNotReused(User user, String rawPassword, PasswordPolicy policy) {
        if (policy.getHistoryCount() <= 0) {
            return;
        }
        if (StringUtils.hasText(user.getPassword()) && passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ApiException(ErrorCode.PASSWORD_REUSED);
        }
        if (user.getId() == null) {
            return;
        }
        List<PasswordHistory> history = historyRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        int limit = Math.min(policy.getHistoryCount(), history.size());
        for (int i = 0; i < limit; i++) {
            if (passwordEncoder.matches(rawPassword, history.get(i).getPasswordHash())) {
                throw new ApiException(ErrorCode.PASSWORD_REUSED);
            }
        }
    }

    private void trimHistory(Long userId, int historyCount) {
        List<PasswordHistory> history = historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (history.size() <= historyCount) {
            return;
        }
        for (int i = historyCount; i < history.size(); i++) {
            historyRepository.delete(history.get(i));
        }
    }

    private PasswordPolicyDto toDto(PasswordPolicy policy) {
        PasswordPolicyDto dto = new PasswordPolicyDto();
        dto.setMinLength(policy.getMinLength());
        dto.setMaxLength(policy.getMaxLength());
        dto.setRequireUppercase(policy.isRequireUppercase());
        dto.setRequireLowercase(policy.isRequireLowercase());
        dto.setRequireDigit(policy.isRequireDigit());
        dto.setRequireSpecial(policy.isRequireSpecial());
        dto.setExpiryDays(policy.getExpiryDays());
        dto.setHistoryCount(policy.getHistoryCount());
        dto.setUpdatedAt(policy.getUpdatedAt());
        return dto;
    }
}
