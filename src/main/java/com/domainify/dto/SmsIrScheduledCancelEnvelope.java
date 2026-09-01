package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsIrScheduledCancelEnvelope {

    private Integer status;
    private String message;
    private SmsScheduledCancelDataDto data;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SmsScheduledCancelDataDto getData() {
        return data;
    }

    public void setData(SmsScheduledCancelDataDto data) {
        this.data = data;
    }
}
