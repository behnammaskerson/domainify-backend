package com.domainify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DomainRenewalTestRequest {

    @NotBlank
    @Size(max = 253)
    private String domainName = "example.com";

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer windowDays = 30;

    @Email
    @Size(max = 255)
    private String toEmail;

    @NotNull
    private Boolean sendInApp = true;

    @NotNull
    private Boolean sendEmail = true;

    @NotNull
    private Boolean sendSms = false;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Integer getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(Integer windowDays) {
        this.windowDays = windowDays;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public Boolean getSendInApp() {
        return sendInApp;
    }

    public void setSendInApp(Boolean sendInApp) {
        this.sendInApp = sendInApp;
    }

    public Boolean getSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(Boolean sendEmail) {
        this.sendEmail = sendEmail;
    }

    public Boolean getSendSms() {
        return sendSms;
    }

    public void setSendSms(Boolean sendSms) {
        this.sendSms = sendSms;
    }
}
