package com.domainify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SmsConfigUpdateRequest {

    @NotBlank(message = "{validation.sms.serverUrl.required}")
    @Size(max = 512, message = "{validation.sms.serverUrl.size}")
    private String serverUrl;

    @Size(max = 512, message = "{validation.sms.apiKey.size}")
    private String apiKey;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
