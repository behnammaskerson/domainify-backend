package com.domainify.service;

import com.domainify.dto.*;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.UserRepository;
import com.domainify.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TwoFactorService twoFactorService;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       TwoFactorService twoFactorService,
                       PasswordPolicyService passwordPolicyService,
                       EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.twoFactorService = twoFactorService;
        this.passwordPolicyService = passwordPolicyService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(ErrorCode.PASSWORDS_MISMATCH);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        passwordPolicyService.applyNewPassword(user, request.getPassword());
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        user.setCreateMethod(User.CreateMethod.REGISTER);
        user.setCreatorUsername(null);
        user.setPreferredLanguage(com.domainify.util.UserPreferredLanguage.normalize(request.getPreferredLanguage()));

        user = userRepository.save(user);
        emailVerificationService.sendVerificationEmailSilently(user);
        return AuthResponse.emailVerificationRequired(UserDto.fromUser(user));
    }

    @Transactional
    public void resendVerificationEmail(ResendVerificationEmailRequest request) {
        emailVerificationService.resendVerificationEmailPublic(request.getEmail());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        assertEmailVerifiedForLogin(user);

        if (user.isTotpEnabled()) {
            return AuthResponse.totpRequired(jwtUtil.generatePreAuthToken(user));
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse verifyTotpLogin(TotpVerifyRequest request) {
        String preAuthToken = request.getPreAuthToken();
        try {
            if (!jwtUtil.isPreAuthToken(preAuthToken) || jwtUtil.isTokenExpired(preAuthToken)) {
                throw new ApiException(ErrorCode.INVALID_PRE_AUTH_TOKEN);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_PRE_AUTH_TOKEN);
        }

        String email = jwtUtil.extractUsername(preAuthToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }
        assertEmailVerifiedForLogin(user);
        if (!user.isTotpEnabled()) {
            throw new ApiException(ErrorCode.TOTP_NOT_ENABLED);
        }
        if (!twoFactorService.verifyTotpOrBackup(user, request.getCode())) {
            throw new ApiException(ErrorCode.INVALID_TOTP);
        }

        return issueTokens(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtUtil.extractUsername(refreshToken);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // Do not re-check email verification here: login already enforced it, and a mid-session
        // email change may leave the address unverified while the user still has a valid refresh token.

        String newAccessToken = jwtUtil.generateToken(user, new HashMap<>());

        AuthResponse response = new AuthResponse(newAccessToken, refreshToken, "Bearer", UserDto.fromUser(user));
        response.setRequiresPasswordChange(passwordPolicyService.isPasswordExpired(user));
        return response;
    }

    public void logout(String token) {
        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(username).orElse(null);
        if (user != null) {
            user.setRefreshToken(null);
            userRepository.save(user);
        }
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.EMAIL_NOT_FOUND));

        // In production, send email with reset token
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(ErrorCode.PASSWORDS_MISMATCH);
        }

        String username = jwtUtil.extractUsername(request.getToken());
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));

        passwordPolicyService.applyNewPassword(user, request.getPassword());
        user.setRefreshToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        emailVerificationService.verifyEmail(token);
    }

    public PasswordPolicyDto getPasswordPolicy() {
        return passwordPolicyService.getDto();
    }

    private AuthResponse issueTokens(User user) {
        assertEmailVerifiedForLogin(user);
        return buildTokenResponse(user);
    }

    /**
     * Re-issues access/refresh tokens after an identity change (e.g. profile email update).
     * Skips the login-time email-verification gate so an already-authenticated session can continue.
     */
    @Transactional
    public UpdateProfileResponse reissueTokensAfterEmailChange(User user) {
        AuthResponse tokens = buildTokenResponse(user);
        return UpdateProfileResponse.withTokens(
                tokens.getUser(),
                tokens.getAccessToken(),
                tokens.getRefreshToken());
    }

    private AuthResponse buildTokenResponse(User user) {
        String accessToken = jwtUtil.generateToken(user, new HashMap<>());
        String refreshToken = jwtUtil.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        AuthResponse response = new AuthResponse(accessToken, refreshToken, "Bearer", UserDto.fromUser(user));
        response.setRequiresPasswordChange(passwordPolicyService.isPasswordExpired(user));
        return response;
    }

    private void assertEmailVerifiedForLogin(User user) {
        if (user.requiresEmailVerificationForLogin()) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }
}
