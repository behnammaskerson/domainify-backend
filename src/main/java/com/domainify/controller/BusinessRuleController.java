package com.domainify.controller;

import com.domainify.dto.ApiResponse;
import com.domainify.dto.BusinessRuleDto;
import com.domainify.entity.BusinessRuleTrigger;
import com.domainify.service.BusinessRuleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for business rule management.
 */
@RestController
@RequestMapping("/admin/business-rules")
@PreAuthorize("hasRole('ADMIN')")
public class BusinessRuleController {

    private final BusinessRuleService businessRuleService;

    public BusinessRuleController(BusinessRuleService businessRuleService) {
        this.businessRuleService = businessRuleService;
    }

    /**
     * Get all business rules with pagination.
     */
    @GetMapping
    public ResponseEntity<Page<BusinessRuleDto>> getAllRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "priority") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Page<BusinessRuleDto> rules = businessRuleService.getAllRules(page, size, sortBy, sortDir);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get rules by trigger event.
     */
    @GetMapping("/trigger/{trigger}")
    public ResponseEntity<List<BusinessRuleDto>> getRulesByTrigger(@PathVariable BusinessRuleTrigger trigger) {
        List<BusinessRuleDto> rules = businessRuleService.getRulesByTrigger(trigger);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get a specific business rule.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessRuleDto> getRule(@PathVariable Long id) {
        BusinessRuleDto rule = businessRuleService.getRule(id);
        return ResponseEntity.ok(rule);
    }

    /**
     * Create a new business rule.
     */
    @PostMapping
    public ResponseEntity<BusinessRuleDto> createRule(@Valid @RequestBody BusinessRuleDto dto) {
        BusinessRuleDto createdRule = businessRuleService.createRule(dto);
        return ResponseEntity.ok(createdRule);
    }

    /**
     * Update an existing business rule.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BusinessRuleDto> updateRule(@PathVariable Long id, @Valid @RequestBody BusinessRuleDto dto) {
        BusinessRuleDto updatedRule = businessRuleService.updateRule(id, dto);
        return ResponseEntity.ok(updatedRule);
    }

    /**
     * Delete a business rule.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        businessRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Enable or disable a business rule.
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<BusinessRuleDto> toggleRule(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        boolean enabled = request.getOrDefault("enabled", false);
        BusinessRuleDto updatedRule = businessRuleService.toggleRule(id, enabled);
        return ResponseEntity.ok(updatedRule);
    }

    /**
     * Search rules by name.
     */
    @GetMapping("/search")
    public ResponseEntity<List<BusinessRuleDto>> searchRules(@RequestParam String q) {
        List<BusinessRuleDto> rules = businessRuleService.searchRules(q);
        return ResponseEntity.ok(rules);
    }

    /**
     * Test a rule (for validation before saving).
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Boolean>> testRule(@Valid @RequestBody BusinessRuleDto dto) {
        boolean result = businessRuleService.testRule(dto, null);
        return ResponseEntity.ok(ApiResponse.success("Rule test completed", result));
    }

    /**
     * Get available trigger events.
     */
    @GetMapping("/triggers")
    public ResponseEntity<BusinessRuleTrigger[]> getTriggers() {
        return ResponseEntity.ok(BusinessRuleTrigger.values());
    }
}