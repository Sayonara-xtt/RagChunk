package com.xtsh.ragchunk.dto.knowledge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "创建知识库请求；未传字段与 application.yaml 默认值合并")
@Data
public class CreateKnowledgeBaseRequest {

    @Schema(description = "知识库名称", example = "kb-test-20260520-153630", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "知识库描述", example = "API test")
    private String description;

    @Schema(description = "切片策略配置")
    private ChunkingPayload chunking;

    @Schema(description = "规则切片参数")
    private RulePayload rule;

    @Schema(description = "切片质量评估阈值")
    private QualityPayload quality;

    @Schema(description = "千问语义重切配置")
    private AiPayload ai;

    @Schema(description = "向量化模型配置")
    private EmbeddingPayload embedding;

    @Schema(description = "检索问答参数（TopK、相似度阈值）")
    private RetrievalPayload retrieval;

    @Schema(description = "智能问答方案：1=纯应答 2=协作渐进 3=协作全量 5=Agent")
    private QaPayload qa;

    @Schema(description = "切片模式配置")
    @Data
    public static class ChunkingPayload {
        @Schema(description = "切片模式", example = "hybrid", allowableValues = {"hybrid"})
        private String mode;

        @Schema(description = "千问介入：never=仅规则；auto=质量分低或长文单段时触发；always=总是千问重切", example = "never", allowableValues = {"auto", "never", "always"})
        private String aiMode;
    }

    @Schema(description = "规则切片参数")
    @Data
    public static class RulePayload {
        @Schema(description = "单块最大字符数", example = "1200")
        private Integer maxChars;

        @Schema(description = "单块最小字符数", example = "80")
        private Integer minChars;

        @Schema(description = "相邻块重叠字符数", example = "80")
        private Integer overlap;

        @Schema(description = "纯文本分隔符列表", example = "[\"\\n\\n\", \"\\n\"]")
        private List<String> plainSeparators;

        @Schema(description = "Markdown 分隔符列表", example = "[\"\\n## \", \"\\n\\n\", \"\\n\"]")
        private List<String> markdownSeparators;

        @Schema(description = "超长块是否按句号边界回退切分", example = "true")
        private Boolean sentenceBoundaryFallback;
    }

    @Schema(description = "切片质量评估")
    @Data
    public static class QualityPayload {
        @Schema(description = "质量分阈值，低于则触发千问重切（0-100）", example = "70")
        private Integer scoreThreshold;
    }

    @Schema(description = "千问语义重切")
    @Data
    public static class AiPayload {
        @Schema(description = "切片用千问模型", example = "qwen-plus")
        private String chunkModel;

        @Schema(description = "单文档最大千问调用次数", example = "1")
        private Integer maxCallsPerDoc;

        @Schema(description = "单次输入最大 token", example = "8000")
        private Integer maxInputTokens;

        @Schema(description = "JSON 解析失败重试次数", example = "1")
        private Integer retryOnParseError;
    }

    @Schema(description = "向量化配置")
    @Data
    public static class EmbeddingPayload {
        @Schema(description = "Embedding 模型名", example = "text-embedding-v3")
        private String model;
    }

    @Schema(description = "检索配置")
    @Data
    public static class RetrievalPayload {
        @Schema(description = "检索返回的最大片段数", example = "3")
        private Integer topK;

        @Schema(description = "向量相似度下限（0-1）", example = "0.3")
        private Double scoreThreshold;
    }

    @Schema(description = "智能问答编排")
    @Data
    public static class QaPayload {
        @Schema(description = "方案：1/2/3/5", example = "2")
        private Integer scheme;
        @Schema(description = "协作渐进：低于该分触发改写", example = "0.35")
        private Double rewriteMinScore;
        @Schema(description = "改写检索短句条数上限", example = "2")
        private Integer maxRewriteQueries;
        @Schema(description = "单次问答 LLM 次数上限", example = "2")
        private Integer maxLlmCalls;
        @Schema(description = "单次问答检索轮次上限", example = "2")
        private Integer maxSearchRounds;
        @Schema(description = "Agent 对话轮次上限", example = "3")
        private Integer agentMaxIterations;
        @Schema(description = "Agent 每轮 tool 次数上限", example = "1")
        private Integer agentMaxToolCallsPerRound;
        @Schema(description = "Agent 是否允许放宽检索阈值")
        private Boolean agentAllowRelaxThreshold;
    }
}