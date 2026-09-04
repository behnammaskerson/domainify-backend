package com.domainify.dto;

/**
 * Profile update result. When the email changes, new JWT tokens are included so the
 * client can keep the session (access tokens are keyed by email).
 */
public class UpdateProfileResponse {
    private UserDto user;
    private String accessToken;
    private String refreshToken;
    private String tokenType;

    public UpdateProfileResponse() {
    }

    public static UpdateProfileResponse of(UserDto user) {
        UpdateProfileResponse response = new UpdateProfileResponse();
        response.user = user;
        return response;
    }

    public static UpdateProfileResponse withTokens(UserDto user, String accessToken, String refreshToken) {
        UpdateProfileResponse response = new UpdateProfileResponse();
        response.user = user;
        response.accessToken = accessToken;
        response.refreshToken = refreshToken;
        response.tokenType = "Bearer";
        return response;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
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
}
