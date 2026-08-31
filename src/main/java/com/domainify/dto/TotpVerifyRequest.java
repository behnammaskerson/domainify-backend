package com.domainify.dto;

import jakarta.validation.constraints.NotBlank;

public class TotpVerifyRequest {

    @NotBlank(message = "{validation.token.required}")
    private String preAuthToken;

    @NotBlank(message = "{validation.totpCode.required}")
    private String code;

    public String getPreAuthToken() {
        return preAuthToken;
    }

    public void setPreAuthToken(String preAuthToken) {
        this.preAuthToken = preAuthToken;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
