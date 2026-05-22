package com.xtsh.ragchunk.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "创建知识库请求；未传字段与 application.yaml 默认值合并")
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

    @Schema(description = "智能问答方案：1=纯应用 2=协作渐进 3=协作全量 5=Agent")
    private QaPayload qa;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ChunkingPayload getChunking() { return chunking; }
    public void setChunking(ChunkingPayload chunking) { this.chunking = chunking; }
    public RulePayload getRule() { return rule; }
    public void setRule(RulePayload rule) { this.rule = rule; }
    public QualityPayload getQuality() { return quality; }
    public void setQuality(QualityPayload quality) { this.quality = quality; }
    public AiPayload getAi() { return ai; }
    public void setAi(AiPayload ai) { this.ai = ai; }
    public EmbeddingPayload getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingPayload embedding) { this.embedding = embedding; }
    public RetrievalPayload getRetrieval() { return retrieval; }
    public void setRetrieval(RetrievalPayload retrieval) { this.retrieval = retrieval; }
    public QaPayload getQa() { return qa; }
    public void setQa(QaPayload qa) { this.qa = qa; }

    @Schema(description = "切片模式配置")
    public static class ChunkingPayload {
        @Schema(description = "切片模式", example = "hybrid", allowableValues = {"hybrid"})
        private String mode;

        @Schema(description = "千问介入：never=仅规则；auto=质量分低或长文单段时触发；always=总是千问重切", example = "never", allowableValues = {"auto", "never", "always"})
        private String aiMode;

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getAiMode() { return aiMode; }
        public void setAiMode(String aiMode) { this.aiMode = aiMode; }
    }

    @Schema(description = "规则切片参数")
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

        public Integer getMaxChars() { return maxChars; }
        public void setMaxChars(Integer maxChars) { this.maxChars = maxChars; }
        public Integer getMinChars() { return minChars; }
        public void setMinChars(Integer minChars) { this.minChars = minChars; }
        public Integer getOverlap() { return overlap; }
        public void setOverlap(Integer overlap) { this.overlap = overlap; }
        public List<String> getPlainSeparators() { return plainSeparators; }
        public void setPlainSeparators(List<String> plainSeparators) { this.plainSeparators = plainSeparators; }
        public List<String> getMarkdownSeparators() { return markdownSeparators; }
        public void setMarkdownSeparators(List<String> markdownSeparators) { this.markdownSeparators = markdownSeparators; }
        public Boolean getSentenceBoundaryFallback() { return sentenceBoundaryFallback; }
        public void setSentenceBoundaryFallback(Boolean sentenceBoundaryFallback) { this.sentenceBoundaryFallback = sentenceBoundaryFallback; }
    }

    @Schema(description = "切片质量评估")
    public static class QualityPayload {
        @Schema(description = "质量分阈值，低于则触发千问重切（0-100）", example = "70")
        private Integer scoreThreshold;

        public Integer getScoreThreshold() { return scoreThreshold; }
        public void setScoreThreshold(Integer scoreThreshold) { this.scoreThreshold = scoreThreshold; }
    }

    @Schema(description = "千问语义重切")
    public static class AiPayload {
        @Schema(description = "切片用千问模型", example = "qwen-plus")
        private String chunkModel;

        @Schema(description = "单文档最大千问调用次数", example = "1")
        private Integer maxCallsPerDoc;

        @Schema(description = "单次输入最大 token", example = "8000")
        private Integer maxInputTokens;

        @Schema(description = "JSON 解析失败重试次数", example = "1")
        private Integer retryOnParseError;

        public String getChunkModel() { return chunkModel; }
        public void setChunkModel(String chunkModel) { this.chunkModel = chunkModel; }
        public Integer getMaxCallsPerDoc() { return maxCallsPerDoc; }
        public void setMaxCallsPerDoc(Integer maxCallsPerDoc) { this.maxCallsPerDoc = maxCallsPerDoc; }
        public Integer getMaxInputTokens() { return maxInputTokens; }
        public void setMaxInputTokens(Integer maxInputTokens) { this.maxInputTokens = maxInputTokens; }
        public Integer getRetryOnParseError() { return retryOnParseError; }
        public void setRetryOnParseError(Integer retryOnParseError) { this.retryOnParseError = retryOnParseError; }
    }

    @Schema(description = "向量化配置")
    public static class EmbeddingPayload {
        @Schema(description = "Embedding 模型名", example = "text-embedding-v3")
        private String model;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    @Schema(description = "检索配置")
    public static class RetrievalPayload {
        @Schema(description = "检索返回的最大片段数", example = "3")
        private Integer topK;

        @Schema(description = "向量相似度下限（0-1）", example = "0.3")
        private Double scoreThreshold;

        public Integer getTopK() { return topK; }
        public void setTopK(Integer topK) { this.topK = topK; }
        public Double getScoreThreshold() { return scoreThreshold; }
        public void setScoreThreshold(Double scoreThreshold) { this.scoreThreshold = scoreThreshold; }
    }

    @Schema(description = "智能问答编排")
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

        public Integer getScheme() { return scheme; }
        public void setScheme(Integer scheme) { this.scheme = scheme; }
        public Double getRewriteMinScore() { return rewriteMinScore; }
        public void setRewriteMinScore(Double rewriteMinScore) { this.rewriteMinScore = rewriteMinScore; }
        public Integer getMaxRewriteQueries() { return maxRewriteQueries; }
        public void setMaxRewriteQueries(Integer maxRewriteQueries) { this.maxRewriteQueries = maxRewriteQueries; }
        public Integer getMaxLlmCalls() { return maxLlmCalls; }
        public void setMaxLlmCalls(Integer maxLlmCalls) { this.maxLlmCalls = maxLlmCalls; }
        public Integer getMaxSearchRounds() { return maxSearchRounds; }
        public void setMaxSearchRounds(Integer maxSearchRounds) { this.maxSearchRounds = maxSearchRounds; }
        public Integer getAgentMaxIterations() { return agentMaxIterations; }
        public void setAgentMaxIterations(Integer agentMaxIterations) { this.agentMaxIterations = agentMaxIterations; }
        public Integer getAgentMaxToolCallsPerRound() { return agentMaxToolCallsPerRound; }
        public void setAgentMaxToolCallsPerRound(Integer agentMaxToolCallsPerRound) {
            this.agentMaxToolCallsPerRound = agentMaxToolCallsPerRound;
        }
        public Boolean getAgentAllowRelaxThreshold() { return agentAllowRelaxThreshold; }
        public void setAgentAllowRelaxThreshold(Boolean agentAllowRelaxThreshold) {
            this.agentAllowRelaxThreshold = agentAllowRelaxThreshold;
        }
    }
}
