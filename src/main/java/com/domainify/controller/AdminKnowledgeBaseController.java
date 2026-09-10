package com.domainify.controller;

import com.domainify.dto.KbArticleDto;
import com.domainify.dto.KbArticleRequest;
import com.domainify.dto.KbCategoryDto;
import com.domainify.dto.KbCategoryRequest;
import com.domainify.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
@RequestMapping("/admin/kb")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public AdminKnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<KbCategoryDto>> listCategories(
            @RequestParam(value = "flat", defaultValue = "true") boolean flat,
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(flat
                ? knowledgeBaseService.listCategoriesFlat(activeOnly)
                : knowledgeBaseService.listCategoriesTree(activeOnly));
    }

    @PostMapping("/categories")
    public ResponseEntity<KbCategoryDto> createCategory(@Valid @RequestBody KbCategoryRequest request) {
        return ResponseEntity.ok(knowledgeBaseService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<KbCategoryDto> updateCategory(@PathVariable Long id,
                                                        @Valid @RequestBody KbCategoryRequest request) {
        return ResponseEntity.ok(knowledgeBaseService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        knowledgeBaseService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/articles")
    public ResponseEntity<Page<KbArticleDto>> listArticles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(knowledgeBaseService.searchArticlesAdmin(q, categoryId, locale, published, page, size));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<KbArticleDto> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeBaseService.getArticleAdmin(id));
    }

    @PostMapping("/articles")
    public ResponseEntity<KbArticleDto> createArticle(@Valid @RequestBody KbArticleRequest request) {
        return ResponseEntity.ok(knowledgeBaseService.createArticle(request));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<KbArticleDto> updateArticle(@PathVariable Long id,
                                                      @Valid @RequestBody KbArticleRequest request) {
        return ResponseEntity.ok(knowledgeBaseService.updateArticle(id, request));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        knowledgeBaseService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
