package com.domainify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class SmsBulkSendRequest {

    @Size(max = 32)
    private String lineNumber;

    @NotBlank(message = "{validation.sms.messageText.required}")
    @Size(max = 2000, message = "{validation.sms.messageText.size}")
    private String messageText;

    @Size(min = 1, max = 100, message = "{validation.sms.mobiles.size}")
    private List<@NotBlank @Size(max = 20) String> mobiles = new ArrayList<>();

    private Long sendDateTime;

    @Size(max = 16)
    private String sendSource;

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
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

    public String getSendSource() {
        return sendSource;
    }

    public void setSendSource(String sendSource) {
        this.sendSource = sendSource;
    }
}
