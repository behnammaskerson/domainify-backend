package com.domainify.dto;

public class CloneTicketResultDto {

    private TicketDetailDto source;
    private TicketDto newTicket;

    public CloneTicketResultDto() {
    }

    public CloneTicketResultDto(TicketDetailDto source, TicketDto newTicket) {
        this.source = source;
        this.newTicket = newTicket;
    }

    public TicketDetailDto getSource() {
        return source;
    }

    public void setSource(TicketDetailDto source) {
        this.source = source;
    }

    public TicketDto getNewTicket() {
        return newTicket;
    }

    public void setNewTicket(TicketDto newTicket) {
        this.newTicket = newTicket;
    }
}
