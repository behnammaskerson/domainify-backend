package com.domainify.dto;

import com.domainify.entity.TicketStatus;

import java.util.ArrayList;
import java.util.List;

public class TicketStatusWorkflowDto {

    private List<TicketStatusDefinitionDto> statuses = new ArrayList<>();
    private List<TicketStatusTransitionDto> transitions = new ArrayList<>();

    public TicketStatusWorkflowDto() {
    }

    public TicketStatusWorkflowDto(
            List<TicketStatusDefinitionDto> statuses,
            List<TicketStatusTransitionDto> transitions) {
        this.statuses = statuses != null ? statuses : new ArrayList<>();
        this.transitions = transitions != null ? transitions : new ArrayList<>();
    }

    public List<TicketStatusDefinitionDto> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<TicketStatusDefinitionDto> statuses) {
        this.statuses = statuses != null ? statuses : new ArrayList<>();
    }

    public List<TicketStatusTransitionDto> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<TicketStatusTransitionDto> transitions) {
        this.transitions = transitions != null ? transitions : new ArrayList<>();
    }

    public static class TicketStatusTransitionDto {
        private TicketStatus from;
        private TicketStatus to;

        public TicketStatusTransitionDto() {
        }

        public TicketStatusTransitionDto(TicketStatus from, TicketStatus to) {
            this.from = from;
            this.to = to;
        }

        public TicketStatus getFrom() {
            return from;
        }

        public void setFrom(TicketStatus from) {
            this.from = from;
        }

        public TicketStatus getTo() {
            return to;
        }

        public void setTo(TicketStatus to) {
            this.to = to;
        }
    }
}
