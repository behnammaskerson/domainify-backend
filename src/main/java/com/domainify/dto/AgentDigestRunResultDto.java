package com.domainify.dto;

public class AgentDigestRunResultDto {

    private int emailsSent;

    public AgentDigestRunResultDto() {
    }

    public AgentDigestRunResultDto(int emailsSent) {
        this.emailsSent = emailsSent;
    }

    public int getEmailsSent() {
        return emailsSent;
    }

    public void setEmailsSent(int emailsSent) {
        this.emailsSent = emailsSent;
    }
}
