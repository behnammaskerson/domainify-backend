package com.domainify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TicketSettingsDto {

    @NotNull
    @Min(1)
    @Max(3650)
    private Integer reopenWindowDays;

    public TicketSettingsDto() {
    }

    public TicketSettingsDto(Integer reopenWindowDays) {
        this.reopenWindowDays = reopenWindowDays;
    }

    public Integer getReopenWindowDays() {
        return reopenWindowDays;
    }

    public void setReopenWindowDays(Integer reopenWindowDays) {
        this.reopenWindowDays = reopenWindowDays;
    }
}
