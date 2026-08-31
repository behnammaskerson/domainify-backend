package com.domainify.dto;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserDto user;
    private boolean requiresTotp;
    private String preAuthToken;
    private boolean requiresPasswordChange;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, String tokenType, UserDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.user = user;
        this.requiresTotp = false;
        this.requiresPasswordChange = false;
    }

    public static AuthResponse totpRequired(String preAuthToken) {
        AuthResponse response = new AuthResponse();
        response.requiresTotp = true;
        response.preAuthToken = preAuthToken;
        response.tokenType = "Bearer";
        return response;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public boolean isRequiresTotp() {
        return requiresTotp;
    }

    public void setRequiresTotp(boolean requiresTotp) {
        this.requiresTotp = requiresTotp;
    }

    public String getPreAuthToken() {
        return preAuthToken;
    }

    public void setPreAuthToken(String preAuthToken) {
        this.preAuthToken = preAuthToken;
    }

    public boolean isRequiresPasswordChange() {
        return requiresPasswordChange;
    }

    public void setRequiresPasswordChange(boolean requiresPasswordChange) {
        this.requiresPasswordChange = requiresPasswordChange;
    }
}
