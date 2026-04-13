package com.asamu.aiagent.idempotency;

import com.asamu.aiagent.common.IdempotencyConflictException;
import com.asamu.aiagent.config.AgentProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final Map<String, StoredRecord> records = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final AgentProperties properties;

    public IdempotencyService(ObjectMapper objectMapper, AgentProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public <T> IdempotencyResult<T> execute(String scope, String idempotencyKey, Object requestPayload, Supplier<T> supplier) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return new IdempotencyResult<>(supplier.get(), false);
        }
        String payloadHash = hashPayload(requestPayload);
        String mapKey = scope + "::" + idempotencyKey;
        Instant now = Instant.now();
        AtomicReference<IdempotencyResult<T>> resultRef = new AtomicReference<>();

        records.compute(mapKey, (key, existing) -> {
            if (existing != null && !existing.expireAt().isBefore(now)) {
                if (!existing.payloadHash().equals(payloadHash)) {
                    throw new IdempotencyConflictException("idempotency key reused with different payload");
                }
                @SuppressWarnings("unchecked")
                T response = (T) existing.response();
                resultRef.set(new IdempotencyResult<>(response, true));
                return existing;
            }
            T data = supplier.get();
            Instant expireAt = now.plusSeconds(properties.getApi().getIdempotencyTtlSeconds());
            StoredRecord created = new StoredRecord(payloadHash, data, expireAt);
            resultRef.set(new IdempotencyResult<>(data, false));
            return created;
        });
        return resultRef.get();
    }

    private String hashPayload(Object requestPayload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(requestPayload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to serialize request payload", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("sha-256 unavailable", e);
        }
    }

    private record StoredRecord(String payloadHash, Object response, Instant expireAt) {
    }
}
