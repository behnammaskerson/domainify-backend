package com.domainify.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateEmailNotificationsRequest {

    @NotNull(message = "{validation.enabled.required}")
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
