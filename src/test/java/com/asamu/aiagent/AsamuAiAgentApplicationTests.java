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
        String body = objectMapper.writeValueAsString(new Request("", "你好"));
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.reply").isNotEmpty());
    }

    @Test
    void chatShouldKeepGivenSessionId() throws Exception {
        String sessionId = "session-fixed-id";
        String body = objectMapper.writeValueAsString(new Request(sessionId, "继续对话"));
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId));
    }

    @Test
    void agentShouldStartAndResume() throws Exception {
        String startBody = objectMapper.writeValueAsString(new TaskRequest("整理技术调研结论"));
        String startResult = mockMvc.perform(post("/agent/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String taskId = objectMapper.readTree(startResult).at("/data/taskId").asText();
        String resumeBody = objectMapper.writeValueAsString(new ResumeRequest("补充优先级"));

        mockMvc.perform(post("/agent/{taskId}/resume", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(taskId));
    }

    private record Request(String sessionId, String message) {}
    private record TaskRequest(String task) {}
    private record ResumeRequest(String answer) {}
}
