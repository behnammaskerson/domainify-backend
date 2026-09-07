package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class DomainRenewalTestResultDto {

    private boolean success;
    private String errorMessage;
    private List<String> channelsSent = new ArrayList<>();

    public DomainRenewalTestResultDto() {
    }

    public DomainRenewalTestResultDto(boolean success, String errorMessage, List<String> channelsSent) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.channelsSent = channelsSent != null ? channelsSent : new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getChannelsSent() {
        return channelsSent;
    }

    public void setChannelsSent(List<String> channelsSent) {
        this.channelsSent = channelsSent;
    }
}
