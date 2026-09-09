package com.domainify.service;

import com.domainify.dto.ImpersonationResponse;
import com.domainify.dto.StartImpersonationRequest;
import com.domainify.dto.UserDto;
import com.domainify.entity.ImpersonationAudit;
import com.domainify.entity.User;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.ImpersonationAuditRepository;
import com.domainify.repository.UserRepository;
import com.domainify.security.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class ImpersonationService {

    private static final int NOTE_MAX = 500;

    private final ImpersonationAuditRepository impersonationAuditRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public ImpersonationService(
            ImpersonationAuditRepository impersonationAuditRepository,
            UserRepository userRepository,
            JwtUtil jwtUtil) {
        this.impersonationAuditRepository = impersonationAuditRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public ImpersonationResponse start(User admin, Long targetUserId, StartImpersonationRequest request) {
        requireAdmin(admin);
        if (targetUserId == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (admin.getId().equals(targetUserId)) {
            throw new ApiException(ErrorCode.IMPERSONATION_SELF);
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (target.getRole() == User.Role.ADMIN) {
            throw new ApiException(ErrorCode.IMPERSONATION_ADMIN_FORBIDDEN);
        }
        if (!target.isEnabled()) {
            throw new ApiException(ErrorCode.IMPERSONATION_TARGET_DISABLED);
        }

        String note = request != null ? request.getNote() : null;
        if (StringUtils.hasText(note)) {
            note = note.trim();
            if (note.length() > NOTE_MAX) {
                throw new ApiException(ErrorCode.IMPERSONATION_NOTE_TOO_LONG);
            }
        } else {
            note = null;
        }

        ImpersonationAudit audit = new ImpersonationAudit();
        audit.setAdmin(admin);
        audit.setTarget(target);
        audit.setNote(note);
        audit = impersonationAuditRepository.save(audit);

        String accessToken = jwtUtil.generateImpersonationToken(target, admin.getId(), audit.getId());
        ImpersonationResponse response = new ImpersonationResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setAuditId(audit.getId());
        response.setExpiresAt(Instant.now().plusMillis(jwtUtil.getImpersonationExpirationMs()));
        response.setUser(UserDto.fromUser(target));
        response.setImpersonator(UserDto.fromUser(admin));
        return response;
    }

    @Transactional
    public void endByAdmin(User admin, Long auditId) {
        requireAdmin(admin);
        if (auditId == null) {
            throw new ApiException(ErrorCode.IMPERSONATION_NOT_ACTIVE);
        }
        ImpersonationAudit audit = impersonationAuditRepository.findById(auditId)
                .orElseThrow(() -> new ApiException(ErrorCode.IMPERSONATION_NOT_ACTIVE));
        if (audit.getAdmin() == null || audit.getAdmin().getId() == null
                || !audit.getAdmin().getId().equals(admin.getId())) {
            throw new ApiException(ErrorCode.IMPERSONATION_NOT_ACTIVE);
        }
        if (audit.getEndedAt() == null) {
            audit.setEndedAt(Instant.now());
            impersonationAuditRepository.save(audit);
        }
    }

    @Transactional
    public void endFromImpersonationToken(String bearerToken) {
        if (!StringUtils.hasText(bearerToken)) {
            throw new ApiException(ErrorCode.IMPERSONATION_NOT_ACTIVE);
        }
        String jwt = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7).trim() : bearerToken.trim();
        if (!jwtUtil.isImpersonationToken(jwt) || jwtUtil.isTokenExpired(jwt)) {
            throw new ApiException(ErrorCode.IMPERSONATION_NOT_ACTIVE);
        }
        Long auditId = jwtUtil.extractImpersonationAuditId(jwt);
        if (auditId == null) {
            throw new ApiException(ErrorCode.IMPERSONATION_NOT_ACTIVE);
        }
        ImpersonationAudit audit = impersonationAuditRepository.findById(auditId)
                .orElseThrow(() -> new ApiException(ErrorCode.IMPERSONATION_NOT_ACTIVE));
        if (audit.getEndedAt() == null) {
            audit.setEndedAt(Instant.now());
            impersonationAuditRepository.save(audit);
        }
    }

    private void requireAdmin(User admin) {
        if (admin == null || admin.getId() == null || admin.getRole() != User.Role.ADMIN || !admin.isEnabled()) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
