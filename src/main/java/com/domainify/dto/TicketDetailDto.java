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
    private boolean canEditDueDate;
    private boolean canWatch;
    private boolean watching;
    private boolean canTransfer;
    private boolean canEscalate;
    private boolean canRateCsat;
    private TicketCsatDto csat;
    private List<TicketAssigneeOptionDto> watchers = new ArrayList<>();
    private List<TicketTransferDto> transfers = new ArrayList<>();
    private List<TicketEscalationDto> escalations = new ArrayList<>();
    private Instant reopenUntil;
    private Integer reopenWindowDays;
    private List<TicketStatus> allowedNextStatuses = new ArrayList<>();
    private TicketReplyDraftDto replyDraft;

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

    public boolean isCanEditDueDate() {
        return canEditDueDate;
    }

    public void setCanEditDueDate(boolean canEditDueDate) {
        this.canEditDueDate = canEditDueDate;
    }

    public boolean isCanWatch() {
        return canWatch;
    }

    public void setCanWatch(boolean canWatch) {
        this.canWatch = canWatch;
    }

    public boolean isWatching() {
        return watching;
    }

    public void setWatching(boolean watching) {
        this.watching = watching;
    }

    public List<TicketAssigneeOptionDto> getWatchers() {
        return watchers;
    }

    public void setWatchers(List<TicketAssigneeOptionDto> watchers) {
        this.watchers = watchers != null ? new ArrayList<>(watchers) : new ArrayList<>();
    }

    public boolean isCanTransfer() {
        return canTransfer;
    }

    public void setCanTransfer(boolean canTransfer) {
        this.canTransfer = canTransfer;
    }

    public List<TicketTransferDto> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<TicketTransferDto> transfers) {
        this.transfers = transfers != null ? new ArrayList<>(transfers) : new ArrayList<>();
    }

    public boolean isCanEscalate() {
        return canEscalate;
    }

    public void setCanEscalate(boolean canEscalate) {
        this.canEscalate = canEscalate;
    }

    public boolean isCanRateCsat() {
        return canRateCsat;
    }

    public void setCanRateCsat(boolean canRateCsat) {
        this.canRateCsat = canRateCsat;
    }

    public TicketCsatDto getCsat() {
        return csat;
    }

    public void setCsat(TicketCsatDto csat) {
        this.csat = csat;
    }

    public List<TicketEscalationDto> getEscalations() {
        return escalations;
    }

    public void setEscalations(List<TicketEscalationDto> escalations) {
        this.escalations = escalations != null ? new ArrayList<>(escalations) : new ArrayList<>();
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

    public TicketReplyDraftDto getReplyDraft() {
        return replyDraft;
    }

    public void setReplyDraft(TicketReplyDraftDto replyDraft) {
        this.replyDraft = replyDraft;
    }
}
