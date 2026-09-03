package com.domainify.dto;

import com.domainify.entity.TicketChannel;
import com.domainify.entity.TicketPriority;
import com.domainify.entity.TicketStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TicketDto {

    private Long id;
    private String publicNumber;
    private String subject;
    private String description;
    private TicketCategoryDto category;
    private TicketPriority priority;
    private TicketStatus status;
    private TicketChannel channel;
    private Long requesterId;
    private String requesterEmail;
    private String requesterName;
    private Long assigneeId;
    private String assigneeEmail;
    private String assigneeName;
    private Instant dueAt;
    private Boolean overdue;
    private Instant closedAt;
    private Instant archivedAt;
    private Instant deletedAt;
    private Boolean archived;
    private Boolean deleted;
    private Long mergedIntoId;
    private String mergedIntoPublicNumber;
    private List<String> mergedSourcePublicNumbers = new ArrayList<>();
    private Long splitFromId;
    private String splitFromPublicNumber;
    private List<String> splitChildPublicNumbers = new ArrayList<>();
    private List<RelatedTicketDto> relatedTickets = new ArrayList<>();
    private List<TicketTagDto> tags = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
    private List<TicketAttachmentDto> attachments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicNumber() {
        return publicNumber;
    }

    public void setPublicNumber(String publicNumber) {
        this.publicNumber = publicNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketCategoryDto getCategory() {
        return category;
    }

    public void setCategory(TicketCategoryDto category) {
        this.category = category;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketChannel getChannel() {
        return channel;
    }

    public void setChannel(TicketChannel channel) {
        this.channel = channel;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeEmail() {
        return assigneeEmail;
    }

    public void setAssigneeEmail(String assigneeEmail) {
        this.assigneeEmail = assigneeEmail;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Boolean getOverdue() {
        return overdue;
    }

    public void setOverdue(Boolean overdue) {
        this.overdue = overdue;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Long getMergedIntoId() {
        return mergedIntoId;
    }

    public void setMergedIntoId(Long mergedIntoId) {
        this.mergedIntoId = mergedIntoId;
    }

    public String getMergedIntoPublicNumber() {
        return mergedIntoPublicNumber;
    }

    public void setMergedIntoPublicNumber(String mergedIntoPublicNumber) {
        this.mergedIntoPublicNumber = mergedIntoPublicNumber;
    }

    public List<String> getMergedSourcePublicNumbers() {
        return mergedSourcePublicNumbers;
    }

    public void setMergedSourcePublicNumbers(List<String> mergedSourcePublicNumbers) {
        this.mergedSourcePublicNumbers = mergedSourcePublicNumbers != null
                ? mergedSourcePublicNumbers
                : new ArrayList<>();
    }

    public Long getSplitFromId() {
        return splitFromId;
    }

    public void setSplitFromId(Long splitFromId) {
        this.splitFromId = splitFromId;
    }

    public String getSplitFromPublicNumber() {
        return splitFromPublicNumber;
    }

    public void setSplitFromPublicNumber(String splitFromPublicNumber) {
        this.splitFromPublicNumber = splitFromPublicNumber;
    }

    public List<String> getSplitChildPublicNumbers() {
        return splitChildPublicNumbers;
    }

    public void setSplitChildPublicNumbers(List<String> splitChildPublicNumbers) {
        this.splitChildPublicNumbers = splitChildPublicNumbers != null
                ? splitChildPublicNumbers
                : new ArrayList<>();
    }

    public List<RelatedTicketDto> getRelatedTickets() {
        return relatedTickets;
    }

    public void setRelatedTickets(List<RelatedTicketDto> relatedTickets) {
        this.relatedTickets = relatedTickets != null ? relatedTickets : new ArrayList<>();
    }

    public List<TicketTagDto> getTags() {
        return tags;
    }

    public void setTags(List<TicketTagDto> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TicketAttachmentDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<TicketAttachmentDto> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }
}
