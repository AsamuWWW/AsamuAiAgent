package com.asamu.aiagent.controller;

import com.asamu.aiagent.common.ApiResponse;
import com.asamu.aiagent.dto.knowledge.KnowledgeIngestRequest;
import com.asamu.aiagent.dto.knowledge.KnowledgeQueryRequest;
import com.asamu.aiagent.rag.KnowledgeCitation;
import com.asamu.aiagent.rag.KnowledgeDocument;
import com.asamu.aiagent.rag.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/knowledge/documents")
    public ApiResponse<KnowledgeDocument> ingest(@Valid @RequestBody KnowledgeIngestRequest request) {
        return ApiResponse.ok(knowledgeService.ingest(request.namespace(), request.title(), request.content()));
    }

    @PostMapping("/knowledge/query")
    public ApiResponse<List<KnowledgeCitation>> query(@Valid @RequestBody KnowledgeQueryRequest request) {
        int topK = request.topK() == null ? 3 : request.topK();
        return ApiResponse.ok(knowledgeService.query(request.namespace(), request.question(), topK));
    }
}
