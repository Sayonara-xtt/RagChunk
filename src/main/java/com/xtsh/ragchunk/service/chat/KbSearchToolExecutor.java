package com.xtsh.ragchunk.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.dto.chat.KbSearchParams;
import com.xtsh.ragchunk.dto.knowledge.KnowledgeBaseConfig;
import com.xtsh.ragchunk.dto.knowledge.QaConfig;
import com.xtsh.ragchunk.vector.ScoredChunk;
import com.xtsh.ragchunk.service.chat.trace.ChatExecutionContext;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 方案：执行 LLM 发起的 search_kb 工具（参数经应用校验后由 {@link ChatRetrievalService} 检索）。
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class KbSearchToolExecutor {

    public static final String TOOL_NAME = "search_kb";

    private final ChatRetrievalService retrieval;
    private final ObjectMapper mapper = new ObjectMapper();


    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> toolDefinition() {
        var props = new LinkedHashMap<String, Object>();
        props.put("query", Map.of("type", "string", "description", "检索短句，必填"));
        props.put("topK", Map.of("type", "integer", "description", "最多返回条数，可选"));
        props.put("scoreThreshold", Map.of("type", "number", "description", "相似度下限 0-1，可选"));
        props.put("relaxThreshold", Map.of("type", "boolean", "description", "是否放宽阈值补救检索"));
        var params = new LinkedHashMap<String, Object>();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", List.of("query"));
        var fn = new LinkedHashMap<String, Object>();
        fn.put("name", TOOL_NAME);
        fn.put("description", "在企业知识库中按语义检索相关文档片段。仅在有需要时调用一次。");
        fn.put("parameters", params);
        var tool = new LinkedHashMap<String, Object>();
        tool.put("type", "function");
        tool.put("function", fn);
        return List.of(tool);
    }

    /**
     * 解析 tool 参数并检索；非法参数抛异常由 Agent 层捕获。
     */
    public List<ScoredChunk> execute(String kbId, String argumentsJson, KnowledgeBaseConfig config, QaConfig qa)
            throws Exception {
        return execute(null, kbId, argumentsJson, config, qa);
    }

    public List<ScoredChunk> execute(
            ChatExecutionContext context, String kbId, String argumentsJson,
            KnowledgeBaseConfig config, QaConfig qa) throws Exception {
        KbSearchParams params = parseAndValidate(argumentsJson);
        log.info("[智能问答] 执行 tool {} queryLen={}, relax={}", TOOL_NAME, params.query().length(), params.relaxThreshold());
        return retrieval.searchWithParams(context, kbId, params, config, qa);
    }

    public String formatToolResult(List<ScoredChunk> hits) throws Exception {
        var arr = hits.stream().limit(5).map(h -> Map.of(
                "chunkId", h.record().chunkId(),
                "docId", h.record().docId(),
                "score", h.score(),
                "text", h.record().text().length() > 500
                        ? h.record().text().substring(0, 500) + "..."
                        : h.record().text()
        )).toList();
        return mapper.writeValueAsString(Map.of("hits", arr, "count", hits.size()));
    }

    private KbSearchParams parseAndValidate(String argumentsJson) throws Exception {
        JsonNode root = mapper.readTree(argumentsJson);
        String query = root.path("query").asText("").trim();
        if (query.isEmpty() || query.length() > 500) {
            throw new IllegalArgumentException("search_kb.query required and max 500 chars");
        }
        Integer topK = root.has("topK") && !root.path("topK").isNull() ? root.path("topK").asInt() : null;
        Double threshold = root.has("scoreThreshold") && !root.path("scoreThreshold").isNull()
                ? root.path("scoreThreshold").asDouble() : null;
        boolean relax = root.path("relaxThreshold").asBoolean(false);
        if (topK != null && (topK < 1 || topK > 10)) {
            throw new IllegalArgumentException("search_kb.topK must be 1-10");
        }
        return new KbSearchParams(query, topK, threshold, relax);
    }
}
