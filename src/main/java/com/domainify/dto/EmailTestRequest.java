package com.domainify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailTestRequest {

    @NotBlank(message = "{validation.email.testTo.required}")
    @Email(message = "{validation.email.testTo.invalid}")
    @Size(max = 255, message = "{validation.email.testTo.size}")
    private String to;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
