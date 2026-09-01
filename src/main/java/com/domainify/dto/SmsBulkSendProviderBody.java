package com.domainify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** Request body sent to the SMS.ir bulk send API. */
public class SmsBulkSendProviderBody {

    @JsonProperty("lineNumber")
    private Long lineNumber;

    @JsonProperty("messageText")
    private String messageText;

    @JsonProperty("mobiles")
    private List<String> mobiles = new ArrayList<>();

    @JsonProperty("sendDateTime")
    private Long sendDateTime;

    public Long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public List<String> getMobiles() {
        return mobiles;
    }

    public void setMobiles(List<String> mobiles) {
        this.mobiles = mobiles != null ? mobiles : new ArrayList<>();
    }

    public Long getSendDateTime() {
        return sendDateTime;
    }

    public void setSendDateTime(Long sendDateTime) {
        this.sendDateTime = sendDateTime;
    }
}
