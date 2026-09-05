package com.domainify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SubmitTicketCsatRequest {

    @NotNull(message = "{validation.required}")
    @Min(value = 1, message = "{validation.required}")
    @Max(value = 5, message = "{validation.required}")
    private Integer score;

    @Size(max = 1000)
    private String comment;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
