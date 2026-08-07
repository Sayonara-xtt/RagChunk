package com.xtsh.ragchunk.integration.dashscope;

import java.util.List;

/**
 * OpenAI 兼容 chat/completions 一次调用结果（支持 tool_calls）。
 */
public record ChatCompletionResult(String content, List<ToolCall> toolCalls) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public record ToolCall(String id, String name, String argumentsJson) {}
}
