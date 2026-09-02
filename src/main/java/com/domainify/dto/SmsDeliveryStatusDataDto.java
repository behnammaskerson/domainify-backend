package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsDeliveryStatusDataDto {

    @JsonProperty("messageId")
    private Long messageId;

    @JsonProperty("mobile")
    private Long mobile;

    @JsonProperty("messageText")
    private String messageText;

    @JsonProperty("sendDateTime")
    private Long sendDateTime;

    @JsonProperty("lineNumber")
    private Long lineNumber;

    @JsonProperty("cost")
    private BigDecimal cost;

    @JsonProperty("deliveryState")
    private Byte deliveryState;

    @JsonProperty("deliveryDateTime")
    private Long deliveryDateTime;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getMobile() {
        return mobile;
    }

    public void setMobile(Long mobile) {
        this.mobile = mobile;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Long getSendDateTime() {
        return sendDateTime;
    }

    public void setSendDateTime(Long sendDateTime) {
        this.sendDateTime = sendDateTime;
    }

    public Long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Byte getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(Byte deliveryState) {
        this.deliveryState = deliveryState;
    }

    public Long getDeliveryDateTime() {
        return deliveryDateTime;
    }

    public void setDeliveryDateTime(Long deliveryDateTime) {
        this.deliveryDateTime = deliveryDateTime;
    }
}
