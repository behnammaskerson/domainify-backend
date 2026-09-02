package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsIrPackReportEnvelope {

    private Integer status;
    private String message;
    private List<SmsDeliveryStatusDataDto> data = new ArrayList<>();

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

    public List<SmsDeliveryStatusDataDto> getData() {
        return data;
    }

    public void setData(List<SmsDeliveryStatusDataDto> data) {
        this.data = data != null ? data : new ArrayList<>();
    }
}
