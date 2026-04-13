package com.asamu.aiagent.memory;

import java.util.List;

public record MemoryRecall(
        List<String> shortTerm,
        List<String> longTerm
) {
}
