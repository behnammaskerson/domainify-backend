package com.domainify.dto;

import jakarta.validation.constraints.NotBlank;

public class TotpDisableRequest {

    @NotBlank(message = "{validation.password.required}")
    private String password;

    @NotBlank(message = "{validation.totpCode.required}")
    private String code;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
