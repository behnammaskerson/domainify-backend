package com.domainify.controller;

import com.domainify.dto.DomainCategoryDto;
import com.domainify.dto.DomainCategoryRequest;
import com.domainify.service.DomainCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class DomainCategoryController {

    private final DomainCategoryService domainCategoryService;

    public DomainCategoryController(DomainCategoryService domainCategoryService) {
        this.domainCategoryService = domainCategoryService;
    }

    /** Active category tree for domain forms (any authenticated user). */
    @GetMapping("/domain-categories/tree")
    public ResponseEntity<List<DomainCategoryDto>> activeTree() {
        return ResponseEntity.ok(domainCategoryService.listTree(true));
    }

    /** Flat indented list of active categories (any authenticated user). */
    @GetMapping("/domain-categories")
    public ResponseEntity<List<DomainCategoryDto>> activeFlat() {
        return ResponseEntity.ok(domainCategoryService.listFlat(true));
    }

    @GetMapping("/admin/domain-categories/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DomainCategoryDto>> adminTree(
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(domainCategoryService.listTree(activeOnly));
    }

    @GetMapping("/admin/domain-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DomainCategoryDto>> adminFlat(
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(domainCategoryService.listFlat(activeOnly));
    }

    @PostMapping("/admin/domain-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DomainCategoryDto> create(@Valid @RequestBody DomainCategoryRequest request) {
        return ResponseEntity.ok(domainCategoryService.create(request));
    }

    @PutMapping("/admin/domain-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DomainCategoryDto> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody DomainCategoryRequest request) {
        return ResponseEntity.ok(domainCategoryService.update(id, request));
    }

    @DeleteMapping("/admin/domain-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        domainCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
