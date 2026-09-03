package com.domainify.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Column(unique = true)
    private String refreshToken;
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    @Column(nullable = false)
    private boolean accountNonExpired = true;
    
    @Column(nullable = false)
    private boolean accountNonLocked = true;
    
    @Column(nullable = false)
    private boolean credentialsNonExpired = true;

    @Column(length = 512)
    private String avatarUrl;

    /** ISO 3166-1 alpha-2 country code used for dialing / formatting (e.g. US, IR). */
    @Column(length = 2)
    private String phoneCountryCode;

    /** National mobile number digits only (no country code or formatting). */
    @Column(length = 20)
    private String phoneNumber;

    /** Whether TOTP two-factor authentication is enabled. */
    @Column(nullable = false)
    private boolean totpEnabled = false;

    /** Base32 TOTP secret (set during setup; cleared when 2FA is disabled). */
    @Column(length = 64)
    private String totpSecret;

    /** BCrypt-hashed backup codes, JSON array of strings. */
    @Column(columnDefinition = "TEXT")
    private String totpBackupCodes;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    /** Email/username of the admin who created this user; null for self-registration. */
    @Column(length = 255)
    private String creatorUsername;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CreateMethod createMethod;

    /** When the password was last set or changed; used for expiry policy. */
    private Instant passwordChangedAt;

    @ColumnDefault("true")
    @Column(nullable = true)
    private Boolean emailVerified = true;

    private Instant emailVerifiedAt;

    @ColumnDefault("true")
    @Column(nullable = true)
    private Boolean phoneVerified = true;

    private Instant phoneVerifiedAt;

    @Column(length = 72)
    private String phoneVerificationOtpHash;

    private Instant phoneVerificationOtpExpiresAt;

    private Instant phoneVerificationOtpSentAt;

    private int phoneVerificationOtpAttempts;
    
    @PrePersist
    public void prePersist() {
        if (role == null) {
            role = Role.USER;
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (passwordChangedAt == null) {
            passwordChangedAt = now;
        }
        if (createMethod == null) {
            createMethod = CreateMethod.REGISTER;
        }
        if (createMethod == CreateMethod.REGISTER) {
            emailVerified = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
    
    public enum Role {
        USER,
        SELLER,
        ADMIN
    }

    public enum CreateMethod {
        REGISTER,
        ADMIN
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhoneCountryCode() {
        return phoneCountryCode;
    }

    public void setPhoneCountryCode(String phoneCountryCode) {
        this.phoneCountryCode = phoneCountryCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public String getTotpBackupCodes() {
        return totpBackupCodes;
    }

    public void setTotpBackupCodes(String totpBackupCodes) {
        this.totpBackupCodes = totpBackupCodes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public CreateMethod getCreateMethod() {
        return createMethod;
    }

    public void setCreateMethod(CreateMethod createMethod) {
        this.createMethod = createMethod;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(Instant passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public boolean requiresEmailVerificationForLogin() {
        return createMethod == CreateMethod.REGISTER && !isEmailVerified();
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public boolean isPhoneVerified() {
        return Boolean.TRUE.equals(phoneVerified);
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public Instant getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    public void setPhoneVerifiedAt(Instant phoneVerifiedAt) {
        this.phoneVerifiedAt = phoneVerifiedAt;
    }

    public String getPhoneVerificationOtpHash() {
        return phoneVerificationOtpHash;
    }

    public void setPhoneVerificationOtpHash(String phoneVerificationOtpHash) {
        this.phoneVerificationOtpHash = phoneVerificationOtpHash;
    }

    public Instant getPhoneVerificationOtpExpiresAt() {
        return phoneVerificationOtpExpiresAt;
    }

    public void setPhoneVerificationOtpExpiresAt(Instant phoneVerificationOtpExpiresAt) {
        this.phoneVerificationOtpExpiresAt = phoneVerificationOtpExpiresAt;
    }

    public Instant getPhoneVerificationOtpSentAt() {
        return phoneVerificationOtpSentAt;
    }

    public void setPhoneVerificationOtpSentAt(Instant phoneVerificationOtpSentAt) {
        this.phoneVerificationOtpSentAt = phoneVerificationOtpSentAt;
    }

    public int getPhoneVerificationOtpAttempts() {
        return phoneVerificationOtpAttempts;
    }

    public void setPhoneVerificationOtpAttempts(int phoneVerificationOtpAttempts) {
        this.phoneVerificationOtpAttempts = phoneVerificationOtpAttempts;
    }

    public void clearPhoneVerificationOtp() {
        this.phoneVerificationOtpHash = null;
        this.phoneVerificationOtpExpiresAt = null;
        this.phoneVerificationOtpSentAt = null;
        this.phoneVerificationOtpAttempts = 0;
    }

    public boolean hasPhoneNumber() {
        return phoneCountryCode != null && !phoneCountryCode.isBlank()
                && phoneNumber != null && !phoneNumber.isBlank();
    }

    /** Falls back to createdAt for users created before passwordChangedAt existed. */
    public Instant getEffectivePasswordChangedAt() {
        if (passwordChangedAt != null) {
            return passwordChangedAt;
        }
        return createdAt != null ? createdAt : Instant.now();
    }
}
