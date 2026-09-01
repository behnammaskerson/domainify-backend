package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsIrBulkSendEnvelope {

    private Integer status;
    private String message;
    private SmsBulkSendDataDto data;

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

    public SmsBulkSendDataDto getData() {
        return data;
    }

    public void setData(SmsBulkSendDataDto data) {
        this.data = data;
    }
}
