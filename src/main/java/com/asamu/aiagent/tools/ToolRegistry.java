package com.asamu.aiagent.tools;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class ToolRegistry {

    private final Map<String, Supplier<String>> tools = Map.of(
            "time.now", () -> OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).toString()
    );

    public Optional<String> invoke(String toolName) {
        return Optional.ofNullable(tools.get(toolName)).map(Supplier::get);
    }
}
