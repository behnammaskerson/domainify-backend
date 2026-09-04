package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class UpdateCategoryAgentsRequest {
    private List<Long> agentIds = new ArrayList<>();

    public List<Long> getAgentIds() {
        return agentIds;
    }

    public void setAgentIds(List<Long> agentIds) {
        this.agentIds = agentIds != null ? agentIds : new ArrayList<>();
    }
}
