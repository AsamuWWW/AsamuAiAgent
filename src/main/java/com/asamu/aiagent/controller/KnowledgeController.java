package com.asamu.aiagent.controller;

import com.asamu.aiagent.common.ApiResponse;
import com.asamu.aiagent.dto.knowledge.KnowledgeIngestRequest;
import com.asamu.aiagent.dto.knowledge.KnowledgeQueryRequest;
import com.asamu.aiagent.idempotency.IdempotencyResult;
import com.asamu.aiagent.idempotency.IdempotencyService;
import com.asamu.aiagent.rag.KnowledgeCitation;
import com.asamu.aiagent.rag.KnowledgeDocument;
import com.asamu.aiagent.rag.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class KnowledgeController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_REPLAY_HEADER = "X-Idempotent-Replay";

    private final KnowledgeService knowledgeService;
    private final IdempotencyService idempotencyService;

    public KnowledgeController(KnowledgeService knowledgeService, IdempotencyService idempotencyService) {
        this.knowledgeService = knowledgeService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/knowledge/documents")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> ingest(
            @Valid @RequestBody KnowledgeIngestRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        IdempotencyResult<ApiResponse<KnowledgeDocument>> result = idempotencyService.execute(
                "POST:/knowledge/documents",
                idempotencyKey,
                request,
                () -> ApiResponse.ok(knowledgeService.ingest(request.namespace(), request.title(), request.content()))
        );
        return withIdempotencyHeaders(result);
    }

    @PostMapping("/knowledge/query")
    public ResponseEntity<ApiResponse<List<KnowledgeCitation>>> query(
            @Valid @RequestBody KnowledgeQueryRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        IdempotencyResult<ApiResponse<List<KnowledgeCitation>>> result = idempotencyService.execute(
                "POST:/knowledge/query",
                idempotencyKey,
                request,
                () -> {
                    int topK = request.topK() == null ? 3 : request.topK();
                    return ApiResponse.ok(knowledgeService.query(request.namespace(), request.question(), topK));
                }
        );
        return withIdempotencyHeaders(result);
    }

    private <T> ResponseEntity<ApiResponse<T>> withIdempotencyHeaders(IdempotencyResult<ApiResponse<T>> result) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(IDEMPOTENCY_REPLAY_HEADER, String.valueOf(result.replayed()));
        return ResponseEntity.ok().headers(headers).body(result.data());
    }
}
