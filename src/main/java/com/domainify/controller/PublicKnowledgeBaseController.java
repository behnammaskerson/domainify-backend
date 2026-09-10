package com.domainify.controller;

import com.domainify.dto.KbArticleDto;
import com.domainify.dto.KbCategoryDto;
import com.domainify.service.KnowledgeBaseService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/kb")
public class PublicKnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public PublicKnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<KbCategoryDto>> listCategories(
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(knowledgeBaseService.listActiveCategories(locale));
    }

    @GetMapping("/articles")
    public ResponseEntity<Page<KbArticleDto>> searchArticles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String locale,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(knowledgeBaseService.searchPublished(q, categoryId, locale, page, size));
    }

    @GetMapping("/articles/by-slug/{slug}")
    public ResponseEntity<KbArticleDto> getBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(knowledgeBaseService.getPublishedBySlug(slug, locale));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<KbArticleDto>> suggest(
            @RequestParam String q,
            @RequestParam(required = false) String locale,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(knowledgeBaseService.suggest(q, locale, limit));
    }
}
