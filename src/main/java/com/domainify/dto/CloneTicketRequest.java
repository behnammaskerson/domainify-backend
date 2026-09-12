package com.domainify.dto;

import jakarta.validation.constraints.Size;

public class CloneTicketRequest {

    @Size(max = 200)
    private String subject;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
