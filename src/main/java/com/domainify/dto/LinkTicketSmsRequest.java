package com.domainify.dto;

import com.domainify.entity.TicketSmsLinkType;

import java.util.ArrayList;
import java.util.List;

public class LinkTicketSmsRequest {

    private List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public static class Item {
        private TicketSmsLinkType type;
        private String externalId;

        public TicketSmsLinkType getType() {
            return type;
        }

        public void setType(TicketSmsLinkType type) {
            this.type = type;
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }
    }
}
