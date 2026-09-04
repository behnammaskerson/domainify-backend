package com.domainify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdatePreferredLanguageRequest {

    @NotBlank(message = "{validation.language.required}")
    @Size(min = 2, max = 5)
    private String language;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
