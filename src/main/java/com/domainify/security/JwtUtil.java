package com.domainify.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_PRE_AUTH = "pre_auth";
    public static final String TOKEN_TYPE_EMAIL_VERIFY = "email_verify";
    public static final String CLAIM_VERIFY_EMAIL = "email";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${jwt.pre-auth-expiration:300000}")
    private Long preAuthExpiration;

    @Value("${jwt.email-verify-expiration:86400000}")
    private Long emailVerifyExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.putIfAbsent(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        return buildToken(userDetails, claims, expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, new HashMap<>(), refreshExpiration);
    }

    public String generatePreAuthToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_PRE_AUTH);
        return buildToken(userDetails, claims, preAuthExpiration);
    }

    public String generateEmailVerificationToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_EMAIL_VERIFY);
        claims.put(CLAIM_VERIFY_EMAIL, email);
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + emailVerifyExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isEmailVerifyToken(String token) {
        return TOKEN_TYPE_EMAIL_VERIFY.equals(extractTokenType(token));
    }

    public Long extractUserId(String token) {
        String subject = extractUsername(token);
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(subject.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public String extractVerifyEmail(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_VERIFY_EMAIL, String.class));
    }

    private String buildToken(UserDetails userDetails, Map<String, Object> extraClaims, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isPreAuthToken(String token) {
        return TOKEN_TYPE_PRE_AUTH.equals(extractTokenType(token));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token) && !isPreAuthToken(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
