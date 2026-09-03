package com.domainify.dto;

import com.domainify.entity.TicketStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TicketDetailDto {

    private TicketDto ticket;
    private List<TicketMessageDto> messages = new ArrayList<>();
    private boolean canReply;
    private boolean canClose;
    private boolean canReopen;
    private boolean canArchive;
    private boolean canUnarchive;
    private boolean canSoftDelete;
    private boolean canRestore;
    private boolean canMerge;
    private boolean canSplit;
    private boolean canLinkRelated;
    private Instant reopenUntil;
    private Integer reopenWindowDays;
    private List<TicketStatus> allowedNextStatuses = new ArrayList<>();

    public TicketDetailDto() {
    }

    public TicketDetailDto(TicketDto ticket, List<TicketMessageDto> messages, boolean canReply) {
        this(ticket, messages, canReply, List.of());
    }

    public TicketDetailDto(
            TicketDto ticket,
            List<TicketMessageDto> messages,
            boolean canReply,
            List<TicketStatus> allowedNextStatuses) {
        this.ticket = ticket;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.canReply = canReply;
        this.allowedNextStatuses = allowedNextStatuses != null ? new ArrayList<>(allowedNextStatuses) : new ArrayList<>();
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

    public boolean isCanClose() {
        return canClose;
    }

    public void setCanClose(boolean canClose) {
        this.canClose = canClose;
    }

    public boolean isCanReopen() {
        return canReopen;
    }

    public void setCanReopen(boolean canReopen) {
        this.canReopen = canReopen;
    }

    public boolean isCanArchive() {
        return canArchive;
    }

    public void setCanArchive(boolean canArchive) {
        this.canArchive = canArchive;
    }

    public boolean isCanUnarchive() {
        return canUnarchive;
    }

    public void setCanUnarchive(boolean canUnarchive) {
        this.canUnarchive = canUnarchive;
    }

    public boolean isCanSoftDelete() {
        return canSoftDelete;
    }

    public void setCanSoftDelete(boolean canSoftDelete) {
        this.canSoftDelete = canSoftDelete;
    }

    public boolean isCanRestore() {
        return canRestore;
    }

    public void setCanRestore(boolean canRestore) {
        this.canRestore = canRestore;
    }

    public boolean isCanMerge() {
        return canMerge;
    }

    public void setCanMerge(boolean canMerge) {
        this.canMerge = canMerge;
    }

    public boolean isCanSplit() {
        return canSplit;
    }

    public void setCanSplit(boolean canSplit) {
        this.canSplit = canSplit;
    }

    public boolean isCanLinkRelated() {
        return canLinkRelated;
    }

    public void setCanLinkRelated(boolean canLinkRelated) {
        this.canLinkRelated = canLinkRelated;
    }

    public Instant getReopenUntil() {
        return reopenUntil;
    }

    public void setReopenUntil(Instant reopenUntil) {
        this.reopenUntil = reopenUntil;
    }

    public Integer getReopenWindowDays() {
        return reopenWindowDays;
    }

    public void setReopenWindowDays(Integer reopenWindowDays) {
        this.reopenWindowDays = reopenWindowDays;
    }

    public List<TicketStatus> getAllowedNextStatuses() {
        return allowedNextStatuses;
    }

    public void setAllowedNextStatuses(List<TicketStatus> allowedNextStatuses) {
        this.allowedNextStatuses = allowedNextStatuses != null ? new ArrayList<>(allowedNextStatuses) : new ArrayList<>();
    }
}
