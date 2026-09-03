package com.domainify.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketMessageDtoJacksonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesCanEditFlags() throws Exception {
        TicketMessageDto dto = new TicketMessageDto();
        dto.setCanEdit(true);
        dto.setCanDelete(true);
        dto.setHasRevisions(true);

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"canEdit\":true"), () -> "Expected canEdit in JSON but got: " + json);
        assertTrue(json.contains("\"canDelete\":true"), () -> "Expected canDelete in JSON but got: " + json);
        assertTrue(json.contains("\"hasRevisions\":true"), () -> "Expected hasRevisions in JSON but got: " + json);
    }
}
