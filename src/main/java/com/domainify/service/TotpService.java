package com.domainify.service;

import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TotpService {

    private static final int BACKUP_CODE_COUNT = 8;
    private static final int BACKUP_CODE_LENGTH = 8;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final String issuer;

    public TotpService(ObjectMapper objectMapper,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.totp.issuer:Domainify}") String issuer) {
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.issuer = issuer;
        ((DefaultCodeVerifier) this.codeVerifier).setTimePeriod(30);
        ((DefaultCodeVerifier) this.codeVerifier).setAllowedTimePeriodDiscrepancy(1);
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public TotpQrPayload buildQrPayload(String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            byte[] image = qrGenerator.generate(data);
            String dataUri = getDataUriForImage(image, qrGenerator.getImageMimeType());
            return new TotpQrPayload(data.getUri(), dataUri);
        } catch (QrGenerationException e) {
            throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
        }
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }
        String normalized = normalizeCode(code);
        if (!normalized.matches("\\d{6}")) {
            return false;
        }
        return codeVerifier.isValidCode(secret, normalized);
    }

    public List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            codes.add(randomNumeric(random, BACKUP_CODE_LENGTH));
        }
        return codes;
    }

    public String hashBackupCodes(List<String> plainCodes) {
        try {
            List<String> hashed = plainCodes.stream()
                    .map(passwordEncoder::encode)
                    .toList();
            return objectMapper.writeValueAsString(hashed);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
        }
    }

    /**
     * If {@code submittedCode} matches a stored backup hash, returns updated JSON with that hash removed.
     * Returns null when no match.
     */
    public String consumeBackupCode(String storedJson, String submittedCode) {
        if (storedJson == null || submittedCode == null) {
            return null;
        }
        String normalized = normalizeCode(submittedCode).replace("-", "");
        try {
            List<String> hashes = objectMapper.readValue(storedJson, new TypeReference<>() {});
            for (int i = 0; i < hashes.size(); i++) {
                if (passwordEncoder.matches(normalized, hashes.get(i))) {
                    hashes.remove(i);
                    return objectMapper.writeValueAsString(hashes);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String normalizeCode(String code) {
        return code == null ? "" : code.trim().replace(" ", "");
    }

    private String randomNumeric(SecureRandom random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public record TotpQrPayload(String otpauthUri, String qrCodeDataUri) {}
}
