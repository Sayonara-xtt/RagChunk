package com.xtsh.ragchunk.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import com.xtsh.ragchunk.dto.knowledge.QaConfig;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 协作方案：调用 LLM 将用户问题改写为 1～N 条检索短句（LLM 辅助，不执行检索）。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class QueryRewriteService {

    private final DashScopeHttpClient dashScope;
    private final RagChunkProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();


    /**
     * @return 检索短句列表；解析失败返回空列表（由编排层决定是否继续）
     */
    public List<String> rewriteToSearchQueries(String question, QaConfig qa) {
        if (!dashScope.isConfigured()) {
            log.warn("[智能问答] 跳过 Query 改写：LLM 未配置");
            return List.of();
        }
        int maxQ = Math.min(qa.maxRewriteQueries(), 5);
        try {
            String raw = dashScope.chatJson(
                    properties.getChat().getModel(),
                    QueryRewritePromptBuilder.system(maxQ),
                    QueryRewritePromptBuilder.user(question));
            List<String> queries = parseSearchQueries(raw, maxQ);
            log.info("[智能问答] Query 改写完成 产出条数={}, queries={}", queries.size(), queries);
            return queries;
        } catch (Exception e) {
            log.warn("[智能问答] Query 改写失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> parseSearchQueries(String raw, int maxQ) throws Exception {
        String json = extractJson(raw);
        JsonNode arr = mapper.readTree(json).path("search_queries");
        if (!arr.isArray()) {
            throw new IllegalStateException("missing search_queries array");
        }
        List<String> out = new ArrayList<>();
        for (JsonNode n : arr) {
            String t = n.asText("").trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
            if (out.size() >= maxQ) {
                break;
            }
        }
        return out;
    }

    private static String extractJson(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "");
            int end = s.lastIndexOf("```");
            if (end >= 0) {
                s = s.substring(0, end).trim();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        throw new IllegalStateException("no JSON in rewrite response");
    }
}
