package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsScheduledCancelDataDto {

    @JsonProperty("returnedCreditCount")
    private BigDecimal returnedCreditCount;

    @JsonProperty("smsCount")
    private Integer smsCount;

    public BigDecimal getReturnedCreditCount() {
        return returnedCreditCount;
    }

    public void setReturnedCreditCount(BigDecimal returnedCreditCount) {
        this.returnedCreditCount = returnedCreditCount;
    }

    public Integer getSmsCount() {
        return smsCount;
    }

    public void setSmsCount(Integer smsCount) {
        this.smsCount = smsCount;
    }
}
