package com.domainify.dto;

import java.time.Instant;

public class TicketCsatDto {

    private short score;
    private String comment;
    private Instant ratedAt;

    public TicketCsatDto() {
    }

    public TicketCsatDto(short score, String comment, Instant ratedAt) {
        this.score = score;
        this.comment = comment;
        this.ratedAt = ratedAt;
    }

    public short getScore() {
        return score;
    }

    public void setScore(short score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getRatedAt() {
        return ratedAt;
    }

    public void setRatedAt(Instant ratedAt) {
        this.ratedAt = ratedAt;
    }
}
