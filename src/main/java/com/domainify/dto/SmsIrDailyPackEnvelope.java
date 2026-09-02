package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsIrDailyPackEnvelope {

    private Integer status;
    private String message;
    private List<SmsDailyPackItemDto> data = new ArrayList<>();

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

    public List<SmsDailyPackItemDto> getData() {
        return data;
    }

    public void setData(List<SmsDailyPackItemDto> data) {
        this.data = data != null ? data : new ArrayList<>();
    }
}
