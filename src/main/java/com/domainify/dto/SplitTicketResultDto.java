package com.domainify.dto;

public class SplitTicketResultDto {

    private TicketDetailDto source;
    private TicketDto newTicket;

    public SplitTicketResultDto() {
    }

    public SplitTicketResultDto(TicketDetailDto source, TicketDto newTicket) {
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
