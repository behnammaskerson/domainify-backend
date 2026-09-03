package com.domainify.dto;

import jakarta.validation.constraints.Size;

public class SaveTicketReplyDraftRequest {

    @Size(max = 10000)
    private String body;

    private Boolean internalNote;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Boolean getInternalNote() {
        return internalNote;
    }

    public void setInternalNote(Boolean internalNote) {
        this.internalNote = internalNote;
    }
}
