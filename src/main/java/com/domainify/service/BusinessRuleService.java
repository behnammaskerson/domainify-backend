package com.domainify.service;

import com.domainify.dto.BusinessRuleAction;
import com.domainify.dto.BusinessRuleCondition;
import com.domainify.dto.BusinessRuleDto;
import com.domainify.entity.BusinessRule;
import com.domainify.entity.BusinessRuleTrigger;
import com.domainify.exception.ApiException;
import com.domainify.exception.ErrorCode;
import com.domainify.repository.BusinessRuleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing business rules.
 */
@Service
@Transactional
public class BusinessRuleService {

    private final BusinessRuleRepository businessRuleRepository;
    private final ObjectMapper objectMapper;

    public BusinessRuleService(BusinessRuleRepository businessRuleRepository, ObjectMapper objectMapper) {
        this.businessRuleRepository = businessRuleRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all business rules with pagination.
     */
    @Transactional(readOnly = true)
    public Page<BusinessRuleDto> getAllRules(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        return businessRuleRepository.findAll(pageable).map(this::toDto);
    }

    /**
     * Get rules by trigger event.
     */
    @Transactional(readOnly = true)
    public List<BusinessRuleDto> getRulesByTrigger(BusinessRuleTrigger trigger) {
        return businessRuleRepository.findEnabledRulesByTrigger(trigger)
            .stream()
            .map(this::toDto)
            .toList();
    }

    /**
     * Get a business rule by ID.
     */
    @Transactional(readOnly = true)
    public BusinessRuleDto getRule(Long id) {
        BusinessRule rule = businessRuleRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_RULE_NOT_FOUND));
        return toDto(rule);
    }

    /**
     * Create a new business rule.
     */
    public BusinessRuleDto createRule(BusinessRuleDto dto) {
        validateRule(dto);
        
        BusinessRule rule = new BusinessRule();
        updateRuleFromDto(rule, dto);
        
        BusinessRule savedRule = businessRuleRepository.save(rule);
        return toDto(savedRule);
    }

    /**
     * Update an existing business rule.
     */
    public BusinessRuleDto updateRule(Long id, BusinessRuleDto dto) {
        BusinessRule rule = businessRuleRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_RULE_NOT_FOUND));
        
        validateRule(dto);
        updateRuleFromDto(rule, dto);
        
        BusinessRule savedRule = businessRuleRepository.save(rule);
        return toDto(savedRule);
    }

    /**
     * Delete a business rule.
     */
    public void deleteRule(Long id) {
        BusinessRule rule = businessRuleRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_RULE_NOT_FOUND));
        
        businessRuleRepository.delete(rule);
    }

    /**
     * Enable or disable a business rule.
     */
    public BusinessRuleDto toggleRule(Long id, boolean enabled) {
        BusinessRule rule = businessRuleRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.BUSINESS_RULE_NOT_FOUND));
        
        rule.setEnabled(enabled);
        BusinessRule savedRule = businessRuleRepository.save(rule);
        return toDto(savedRule);
    }

    /**
     * Search rules by name.
     */
    @Transactional(readOnly = true)
    public List<BusinessRuleDto> searchRules(String query) {
        return businessRuleRepository.findByNameContainingIgnoreCase(query)
            .stream()
            .map(this::toDto)
            .toList();
    }

    /**
     * Test a rule against mock data (for validation).
     */
    public boolean testRule(BusinessRuleDto dto, Object mockTicketData) {
        // This would be used for testing rules before saving
        // Implementation would depend on mock data format
        return true;
    }

    /**
     * Convert entity to DTO.
     */
    private BusinessRuleDto toDto(BusinessRule rule) {
        BusinessRuleDto dto = new BusinessRuleDto();
        dto.setId(rule.getId());
        dto.setName(rule.getName());
        dto.setDescription(rule.getDescription());
        dto.setEnabled(rule.isEnabled());
        dto.setTriggerEvent(rule.getTriggerEvent());
        dto.setPriority(rule.getPriority());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        dto.setLastExecutedAt(rule.getLastExecutedAt());
        dto.setExecutionCount(rule.getExecutionCount());

        try {
            // Parse conditions and actions from JSON
            List<BusinessRuleCondition> conditions = objectMapper.readValue(
                rule.getConditions(), new TypeReference<List<BusinessRuleCondition>>() {}
            );
            dto.setConditions(conditions);

            List<BusinessRuleAction> actions = objectMapper.readValue(
                rule.getActions(), new TypeReference<List<BusinessRuleAction>>() {}
            );
            dto.setActions(actions);
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID_JSON);
        }

        return dto;
    }

    /**
     * Update entity from DTO.
     */
    private void updateRuleFromDto(BusinessRule rule, BusinessRuleDto dto) {
        rule.setName(dto.getName());
        rule.setDescription(dto.getDescription());
        rule.setEnabled(dto.getEnabled());
        rule.setTriggerEvent(dto.getTriggerEvent());
        rule.setPriority(dto.getPriority());

        try {
            // Convert conditions and actions to JSON
            String conditionsJson = objectMapper.writeValueAsString(dto.getConditions());
            rule.setConditions(conditionsJson);

            String actionsJson = objectMapper.writeValueAsString(dto.getActions());
            rule.setActions(actionsJson);
        } catch (JsonProcessingException ex) {
            throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID_JSON);
        }
    }

    /**
     * Validate rule data.
     */
    private void validateRule(BusinessRuleDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID);
        }

        if (dto.getTriggerEvent() == null) {
            throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID);
        }

        if (dto.getConditions() == null || dto.getConditions().isEmpty()) {
            throw new ApiException(ErrorCode.BUSINESS_RULE_NO_CONDITIONS);
        }

        if (dto.getActions() == null || dto.getActions().isEmpty()) {
            throw new ApiException(ErrorCode.BUSINESS_RULE_NO_ACTIONS);
        }

        // Validate each condition
        for (BusinessRuleCondition condition : dto.getConditions()) {
            if (condition.getField() == null || condition.getField().trim().isEmpty()) {
                throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID_CONDITION);
            }
            if (condition.getOperator() == null) {
                throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID_CONDITION);
            }
            // Value can be null for IS_EMPTY/IS_NOT_EMPTY operators
            if (condition.getValue() == null && 
                condition.getOperator() != BusinessRuleCondition.BusinessRuleOperator.IS_EMPTY &&
                condition.getOperator() != BusinessRuleCondition.BusinessRuleOperator.IS_NOT_EMPTY) {
                throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID_CONDITION);
            }
        }

        // Validate each action
        for (BusinessRuleAction action : dto.getActions()) {
            if (action.getActionType() == null) {
                throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID_ACTION);
            }
            if (action.getParameters() == null || action.getParameters().isEmpty()) {
                throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID_ACTION);
            }
        }

        if (dto.getPriority() == null || dto.getPriority() < 1 || dto.getPriority() > 1000) {
            throw new ApiException(ErrorCode.BUSINESS_RULE_INVALID);
        }
    }
}