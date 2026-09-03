package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

/** Public attachment limits for create/reply UIs. */
public class TicketAttachmentPolicyDto {

    private int maxAttachments;
    private int maxAttachmentSizeMb;
    private long maxAttachmentBytes;
    private List<String> allowedAttachmentKinds = new ArrayList<>();
    private List<String> allowedContentTypes = new ArrayList<>();
    private List<String> allowedExtensions = new ArrayList<>();

    public int getMaxAttachments() {
        return maxAttachments;
    }

    public void setMaxAttachments(int maxAttachments) {
        this.maxAttachments = maxAttachments;
    }

    public int getMaxAttachmentSizeMb() {
        return maxAttachmentSizeMb;
    }

    public void setMaxAttachmentSizeMb(int maxAttachmentSizeMb) {
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
    }

    public long getMaxAttachmentBytes() {
        return maxAttachmentBytes;
    }

    public void setMaxAttachmentBytes(long maxAttachmentBytes) {
        this.maxAttachmentBytes = maxAttachmentBytes;
    }

    public List<String> getAllowedAttachmentKinds() {
        return allowedAttachmentKinds;
    }

    public void setAllowedAttachmentKinds(List<String> allowedAttachmentKinds) {
        this.allowedAttachmentKinds = allowedAttachmentKinds != null ? allowedAttachmentKinds : new ArrayList<>();
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes != null ? allowedContentTypes : new ArrayList<>();
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions != null ? allowedExtensions : new ArrayList<>();
    }
}
