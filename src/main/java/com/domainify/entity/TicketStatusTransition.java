package com.domainify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ticket_status_transitions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_status_transition", columnNames = {"from_status", "to_status"})
})
public class TicketStatusTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 16)
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 16)
    private TicketStatus toStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TicketStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(TicketStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public TicketStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(TicketStatus toStatus) {
        this.toStatus = toStatus;
    }
}
