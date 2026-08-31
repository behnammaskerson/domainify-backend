package com.domainify.dto;

import jakarta.validation.constraints.NotBlank;

public class TotpCodeRequest {

    @NotBlank(message = "{validation.totpCode.required}")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
