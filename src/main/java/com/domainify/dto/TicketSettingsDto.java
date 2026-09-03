package com.domainify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class TicketSettingsDto {

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer reopenWindowDays;

    @NotNull
    @Min(1)
    @Max(20)
    private Integer maxAttachments;

    @NotNull
    @Min(1)
    @Max(50)
    private Integer maxAttachmentSizeMb;

    @NotEmpty
    @Size(max = 4)
    private List<String> allowedAttachmentKinds = new ArrayList<>();

    public TicketSettingsDto() {
    }

    public TicketSettingsDto(
            Integer reopenWindowDays,
            Integer maxAttachments,
            Integer maxAttachmentSizeMb,
            List<String> allowedAttachmentKinds) {
        this.reopenWindowDays = reopenWindowDays;
        this.maxAttachments = maxAttachments;
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
        this.allowedAttachmentKinds = allowedAttachmentKinds != null ? allowedAttachmentKinds : new ArrayList<>();
    }

    public Integer getReopenWindowDays() {
        return reopenWindowDays;
    }

    public void setReopenWindowDays(Integer reopenWindowDays) {
        this.reopenWindowDays = reopenWindowDays;
    }

    public Integer getMaxAttachments() {
        return maxAttachments;
    }

    public void setMaxAttachments(Integer maxAttachments) {
        this.maxAttachments = maxAttachments;
    }

    public Integer getMaxAttachmentSizeMb() {
        return maxAttachmentSizeMb;
    }

    public void setMaxAttachmentSizeMb(Integer maxAttachmentSizeMb) {
        this.maxAttachmentSizeMb = maxAttachmentSizeMb;
    }

    public List<String> getAllowedAttachmentKinds() {
        return allowedAttachmentKinds;
    }

    public void setAllowedAttachmentKinds(List<String> allowedAttachmentKinds) {
        this.allowedAttachmentKinds = allowedAttachmentKinds != null ? allowedAttachmentKinds : new ArrayList<>();
    }
}
