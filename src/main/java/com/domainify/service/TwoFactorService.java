package com.domainify.service;

import com.domainify.dto.*;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.UserRepository;
import com.domainify.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TwoFactorService {

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;

    public TwoFactorService(UserRepository userRepository,
                            TotpService totpService,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TotpSetupResponse beginSetup(User principal) {
        User user = requireUser(principal.getId());
        if (user.isTotpEnabled()) {
            throw new ApiException(ErrorCode.TOTP_ALREADY_ENABLED);
        }

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        user.setTotpEnabled(false);
        user.setTotpBackupCodes(null);
        userRepository.save(user);

        TotpService.TotpQrPayload qr = totpService.buildQrPayload(user.getEmail(), secret);
        return new TotpSetupResponse(secret, qr.otpauthUri(), qr.qrCodeDataUri());
    }

    @Transactional
    public TotpEnableResponse enable(User principal, TotpCodeRequest request) {
        User user = requireUser(principal.getId());
        if (user.isTotpEnabled()) {
            throw new ApiException(ErrorCode.TOTP_ALREADY_ENABLED);
        }
        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            throw new ApiException(ErrorCode.TOTP_NOT_SETUP);
        }
        if (!totpService.verifyCode(user.getTotpSecret(), request.getCode())) {
            throw new ApiException(ErrorCode.INVALID_TOTP);
        }

        List<String> backupCodes = totpService.generateBackupCodes();
        user.setTotpEnabled(true);
        user.setTotpBackupCodes(totpService.hashBackupCodes(backupCodes));
        userRepository.save(user);

        return new TotpEnableResponse(UserDto.fromUser(user), backupCodes);
    }

    @Transactional
    public UserDto disable(User principal, TotpDisableRequest request) {
        User user = requireUser(principal.getId());
        if (!user.isTotpEnabled()) {
            throw new ApiException(ErrorCode.TOTP_NOT_ENABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (!verifyTotpOrBackup(user, request.getCode())) {
            throw new ApiException(ErrorCode.INVALID_TOTP);
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.setTotpBackupCodes(null);
        userRepository.save(user);
        return UserDto.fromUser(user);
    }

    @Transactional
    public TotpEnableResponse regenerateBackupCodes(User principal, TotpDisableRequest request) {
        User user = requireUser(principal.getId());
        if (!user.isTotpEnabled()) {
            throw new ApiException(ErrorCode.TOTP_NOT_ENABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (!totpService.verifyCode(user.getTotpSecret(), request.getCode())) {
            throw new ApiException(ErrorCode.INVALID_TOTP);
        }

        List<String> backupCodes = totpService.generateBackupCodes();
        user.setTotpBackupCodes(totpService.hashBackupCodes(backupCodes));
        userRepository.save(user);
        return new TotpEnableResponse(UserDto.fromUser(user), backupCodes);
    }

    @Transactional
    public boolean verifyTotpOrBackup(User user, String code) {
        if (totpService.verifyCode(user.getTotpSecret(), code)) {
            return true;
        }
        if (user.getTotpBackupCodes() == null) {
            return false;
        }
        String updated = totpService.consumeBackupCode(user.getTotpBackupCodes(), code);
        if (updated == null) {
            return false;
        }
        user.setTotpBackupCodes(updated);
        userRepository.save(user);
        return true;
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
