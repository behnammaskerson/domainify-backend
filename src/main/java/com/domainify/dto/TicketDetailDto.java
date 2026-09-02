package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class TicketDetailDto {

    private TicketDto ticket;
    private List<TicketMessageDto> messages = new ArrayList<>();
    private boolean canReply;

    public TicketDetailDto() {
    }

    public TicketDetailDto(TicketDto ticket, List<TicketMessageDto> messages, boolean canReply) {
        this.ticket = ticket;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.canReply = canReply;
    }

    public TicketDto getTicket() {
        return ticket;
    }

    public void setTicket(TicketDto ticket) {
        this.ticket = ticket;
    }

    public List<TicketMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<TicketMessageDto> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    public boolean isCanReply() {
        return canReply;
    }

    public void setCanReply(boolean canReply) {
        this.canReply = canReply;
    }
}
