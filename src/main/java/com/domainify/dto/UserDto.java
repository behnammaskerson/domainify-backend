package com.domainify.dto;

import com.domainify.entity.User;

import java.time.Instant;

public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private User.Role role;
    private boolean enabled;
    private String avatarUrl;
    private String phoneCountryCode;
    private String phoneNumber;
    private boolean totpEnabled;
    private boolean emailVerified;
    private Instant emailVerifiedAt;
    private boolean phoneVerified;
    private Instant phoneVerifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String creatorUsername;
    private User.CreateMethod createMethod;

    public UserDto() {
    }

    public UserDto(Long id, String firstName, String lastName, String email, User.Role role, boolean enabled,
                   String avatarUrl, String phoneCountryCode, String phoneNumber, boolean totpEnabled,
                   boolean emailVerified, Instant emailVerifiedAt,
                   boolean phoneVerified, Instant phoneVerifiedAt,
                   Instant createdAt, Instant updatedAt, String creatorUsername, User.CreateMethod createMethod) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.avatarUrl = avatarUrl;
        this.phoneCountryCode = phoneCountryCode;
        this.phoneNumber = phoneNumber;
        this.totpEnabled = totpEnabled;
        this.emailVerified = emailVerified;
        this.emailVerifiedAt = emailVerifiedAt;
        this.phoneVerified = phoneVerified;
        this.phoneVerifiedAt = phoneVerifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.creatorUsername = creatorUsername;
        this.createMethod = createMethod;
    }

    public static UserDto fromUser(User user) {
        return new UserDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole(),
            user.isEnabled(),
            user.getAvatarUrl(),
            user.getPhoneCountryCode(),
            user.getPhoneNumber(),
            user.isTotpEnabled(),
            user.isEmailVerified(),
            user.getEmailVerifiedAt(),
            user.isPhoneVerified(),
            user.getPhoneVerifiedAt(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getCreatorUsername(),
            user.getCreateMethod()
        );
    }

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

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }

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

    public boolean isEmailVerified() {
        return emailVerified;
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
        return phoneVerified;
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

    public User.CreateMethod getCreateMethod() {
        return createMethod;
    }

    public void setCreateMethod(User.CreateMethod createMethod) {
        this.createMethod = createMethod;
    }
}
