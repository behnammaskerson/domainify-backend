package com.domainify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SmsDefaultLineRequest {

    @NotBlank(message = "{validation.sms.defaultLine.required}")
    @Size(max = 32, message = "{validation.sms.defaultLine.size}")
    private String defaultLine;

    public String getDefaultLine() {
        return defaultLine;
    }

    public void setDefaultLine(String defaultLine) {
        this.defaultLine = defaultLine;
    }
}
