package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.TextChunk;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 千问语义重切（任务 SEMANTIC_RESPLIT）：将全文交给 LLM，要求返回 JSON 切片列表。
 * <p>Prompt 规则见 {@link SemanticResplitPromptBuilder}，与 {@link ChunkValidationService} 校验对齐。
 */
@Service
public class SemanticResplitService {

    private static final Logger log = LoggerFactory.getLogger(SemanticResplitService.class);

    private final DashScopeHttpClient dashScope;
    private final RagChunkProperties ragChunkProperties;
    private final RuleChunker ruleChunker;
    private final ObjectMapper mapper = new ObjectMapper();

    public SemanticResplitService(DashScopeHttpClient dashScope, RagChunkProperties ragChunkProperties,
                                  RuleChunker ruleChunker) {
        this.dashScope = dashScope;
        this.ragChunkProperties = ragChunkProperties;
        this.ruleChunker = ruleChunker;
    }

    /**
     * 调用 LLM 重切；失败由 {@link HybridChunkingService} 捕获并回退规则切片。
     *
     * @param fileName 原始文件名，用于 Prompt 中的结构提示
     * @param profile  {@link ChunkProfileDetector} 画像（markdown / plain）
     */
    public List<TextChunk> resplit(String docId, String text, String fileName, String profile,
                                   KnowledgeBaseConfig config) throws Exception {
        var rule = config.rule();
        String input = truncate(text, config.ai().maxInputTokens());
        boolean truncated = input.length() < text.length();
        String system = SemanticResplitPromptBuilder.buildSystem(rule);
        int retries = config.ai().retryOnParseError();
        Exception last = null;
        for (int attempt = 0; attempt <= retries; attempt++) {
            try {
                List<TextChunk> chunks;
                if (dashScope.isConfigured()) {
                    String user = SemanticResplitPromptBuilder.buildUser(
                            input, fileName, profile, rule, text.length(), truncated);
                    if (attempt > 0) {
                        user = user + SemanticResplitPromptBuilder.buildRetryHint(rule);
                    }
                    log.info("[文档上传] AI语义重切 请求 docId={}, 模型={}, 画像={}, 正文字数={}, 送入字数={}, "
                                    + "第{}次尝试(共{}次), minChars={}, maxChars={}, 用户提示预览={}",
                            docId, config.ai().chunkModel(), profile, text.length(), input.length(),
                            attempt + 1, retries + 1, rule.minChars(), rule.maxChars(),
                            IngestLogSupport.previewText(user));
                    String raw = dashScope.chatJson(config.ai().chunkModel(), system, user);
                    log.info("[文档上传] AI语义重切 响应 docId={}, 原始响应预览={}",
                            docId, IngestLogSupport.previewResponse(raw));
                    chunks = parseChunks(extractJson(raw));
                    tagHybridChunks(chunks);
                    log.info("[文档上传] AI语义重切 解析完成 docId={}, 切片数={}, 切片预览={}",
                            docId, chunks.size(), IngestLogSupport.previewChunks(chunks, 3));
                } else {
                    var ds = ragChunkProperties.getDashscope();
                    log.warn("[文档上传] AI语义重切 请求 docId={}, LLM未配置，回退规则切片 送入字数={}, "
                                    + "provider={}, baseUrl={}, apiKey已设={}",
                            docId, input.length(), ds.getProvider(), ds.getBaseUrl(),
                            ds.getApiKey() != null && !ds.getApiKey().isBlank());
                    chunks = ruleChunker.chunk(input, profile, rule);
                    tagHybridChunks(chunks);
                }
                return chunks;
            } catch (Exception e) {
                last = e;
                log.warn("[文档上传] AI语义重切 失败 docId={}, 第{}次尝试, 原因={}",
                        docId, attempt + 1, e.getMessage());
            }
        }
        throw last != null ? last : new IllegalStateException("semantic resplit failed");
    }

    private static void tagHybridChunks(List<TextChunk> chunks) {
        for (int j = 0; j < chunks.size(); j++) {
            chunks.get(j).setIndex(j);
            chunks.get(j).setSource("hybrid");
        }
    }

    private String truncate(String text, int maxTokens) {
        int maxChars = Math.max(500, maxTokens * 2);
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars);
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("LLM returned empty content");
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "");
            int endFence = s.lastIndexOf("```");
            if (endFence >= 0) {
                s = s.substring(0, endFence).trim();
            }
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        throw new IllegalStateException("no JSON object in LLM response");
    }

    private List<TextChunk> parseChunks(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode arr = root.path("chunks");
        if (!arr.isArray() || arr.isEmpty()) {
            throw new IllegalStateException("JSON missing non-empty chunks array");
        }
        List<TextChunk> out = new ArrayList<>();
        int i = 0;
        for (JsonNode node : arr) {
            String t = node.path("text").asText("").trim();
            if (!t.isEmpty()) {
                out.add(new TextChunk(i++, t));
            }
        }
        if (out.isEmpty()) {
            throw new IllegalStateException("all chunk texts are empty");
        }
        return out;
    }
}
