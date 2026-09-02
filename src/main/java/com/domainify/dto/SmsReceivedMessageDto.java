package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsReceivedMessageDto {

    @JsonProperty("receiveReturnId")
    private Long receiveReturnId;

    @JsonProperty("messageText")
    private String messageText;

    @JsonProperty("number")
    private Long number;

    @JsonProperty("mobile")
    private Long mobile;

    @JsonProperty("receivedDateTime")
    private Long receivedDateTime;

    public Long getReceiveReturnId() {
        return receiveReturnId;
    }

    public void setReceiveReturnId(Long receiveReturnId) {
        this.receiveReturnId = receiveReturnId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public Long getMobile() {
        return mobile;
    }

    public void setMobile(Long mobile) {
        this.mobile = mobile;
    }

    public Long getReceivedDateTime() {
        return receivedDateTime;
    }

    public void setReceivedDateTime(Long receivedDateTime) {
        this.receivedDateTime = receivedDateTime;
    }
}
