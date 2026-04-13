package com.asamu.aiagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AsamuAiAgentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthShouldReturnUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("up"));
    }

    @Test
    void chatShouldReturnReply() throws Exception {
        String body = objectMapper.writeValueAsString(new ChatReq("u1", "", null, null, null, "user", "你好"));
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotent-Replay", "false"))
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.reply").isNotEmpty())
                .andExpect(jsonPath("$.data.provider").isNotEmpty());
    }

    @Test
    void chatShouldKeepGivenSessionId() throws Exception {
        String sessionId = "session-fixed-id";
        String body = objectMapper.writeValueAsString(new ChatReq("u1", sessionId, null, null, null, "user", "继续对话"));
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId));
    }

    @Test
    void toolShouldExecuteTimeCommand() throws Exception {
        String body = objectMapper.writeValueAsString(new ChatReq("u1", "", null, null, null, "user", "/time"));
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolLogs").isArray())
                .andExpect(jsonPath("$.data.reply").isNotEmpty());
    }

    @Test
    void agentShouldStartAndResumeAndQuery() throws Exception {
        String startBody = objectMapper.writeValueAsString(new TaskRequest("u1", "整理技术调研结论,形成执行计划"));
        String startResult = mockMvc.perform(post("/agent/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.steps").isArray())
                .andReturn().getResponse().getContentAsString();

        String taskId = objectMapper.readTree(startResult).at("/data/taskId").asText();
        String resumeBody = objectMapper.writeValueAsString(new ResumeRequest("补充优先级"));

        mockMvc.perform(post("/agent/{taskId}/resume", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(taskId));

        mockMvc.perform(get("/agent/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps").isArray());
    }

    @Test
    void ragShouldIngestAndQuery() throws Exception {
        String ingestBody = objectMapper.writeValueAsString(new IngestRequest("default", "doc1", "Spring Boot supports REST APIs and SSE."));
        mockMvc.perform(post("/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ingestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty());

        String queryBody = objectMapper.writeValueAsString(new QueryRequest("default", "REST", 3));
        mockMvc.perform(post("/knowledge/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void chatShouldReplayWhenIdempotencyKeyMatches() throws Exception {
        String body = objectMapper.writeValueAsString(new ChatReq("u1", "idempotent-session", null, null, null, "user", "同一请求"));
        String key = "chat-key-1";

        mockMvc.perform(post("/chat")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotent-Replay", "false"));

        mockMvc.perform(post("/chat")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotent-Replay", "true"));
    }

    @Test
    void chatShouldRejectIdempotencyKeyReuseWithDifferentPayload() throws Exception {
        String key = "chat-key-2";
        String body1 = objectMapper.writeValueAsString(new ChatReq("u1", "idempotent-session-2", null, null, null, "user", "第一次"));
        String body2 = objectMapper.writeValueAsString(new ChatReq("u1", "idempotent-session-2", null, null, null, "user", "第二次"));

        mockMvc.perform(post("/chat")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/chat")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
    }

    private record ChatReq(String userId, String sessionId, String provider, String model, Double temperature, String role, String message) {}

    private record TaskRequest(String userId, String task) {}

    private record ResumeRequest(String answer) {}

    private record IngestRequest(String namespace, String title, String content) {}

    private record QueryRequest(String namespace, String question, Integer topK) {}
}
