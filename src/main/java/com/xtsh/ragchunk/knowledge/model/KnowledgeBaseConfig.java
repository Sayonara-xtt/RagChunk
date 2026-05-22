package com.xtsh.ragchunk.knowledge.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "知识库运行配置快照（创建时合并 application.yaml 默认值后持久化到 knowledge_base.config_json）")
public record KnowledgeBaseConfig(
        @Schema(description = "切片策略：模式与是否启用千问重切", requiredMode = Schema.RequiredMode.REQUIRED)
        ChunkingConfig chunking,
        @Schema(description = "规则切片参数：块大小、分隔符、重叠与句界回退", requiredMode = Schema.RequiredMode.REQUIRED)
        RuleConfig rule,
        @Schema(description = "切片质量评估：用于 auto 模式下判断是否触发千问重切", requiredMode = Schema.RequiredMode.REQUIRED)
        QualityConfig quality,
        @Schema(description = "千问语义重切（SEMANTIC_RESPLIT）模型与调用限额", requiredMode = Schema.RequiredMode.REQUIRED)
        AiConfig ai,
        @Schema(description = "文档切片向量化使用的 Embedding 模型", requiredMode = Schema.RequiredMode.REQUIRED)
        EmbeddingConfig embedding,
        @Schema(description = "问答检索：返回条数与相似度过滤阈值", requiredMode = Schema.RequiredMode.REQUIRED)
        RetrievalConfig retrieval
) {
    @Schema(description = "切片策略")
    public record ChunkingConfig(
            @Schema(
                    description = "切片模式；一期固定 hybrid（规则为主 + 按需千问）",
                    example = "hybrid",
                    allowableValues = {"hybrid"}
            )
            String mode,
            @Schema(
                    description = "千问介入：never=仅规则切片；auto=质量分低于阈值或长文单段时触发；always=总是千问重切。上传参数 smartChunk=true 等价强制重切",
                    example = "never",
                    allowableValues = {"auto", "never", "always"}
            )
            String aiMode
    ) {}

    @Schema(description = "规则切片参数（RuleChunker 使用）")
    public record RuleConfig(
            @Schema(description = "单块最大字符数，超过则继续切分", example = "1200", minimum = "1")
            int maxChars,
            @Schema(description = "单块最小字符数，过短片段会与相邻段合并", example = "80", minimum = "1")
            int minChars,
            @Schema(description = "相邻切片重叠字符数，后一片段头部拼接前一片段尾部", example = "80", minimum = "0")
            int overlap,
            @Schema(
                    description = "纯文本（.txt / .docx）分段分隔符，按列表顺序优先尝试",
                    example = "[\"\\n\\n\", \"\\n\"]"
            )
            List<String> plainSeparators,
            @Schema(
                    description = "Markdown（.md）分段分隔符，优先按标题与段落切",
                    example = "[\"\\n## \", \"\\n\\n\", \"\\n\"]"
            )
            List<String> markdownSeparators,
            @Schema(
                    description = "当按 maxChars 硬切时，是否向前查找句号/问号等句界再切断，减少断句",
                    example = "true"
            )
            boolean sentenceBoundaryFallback
    ) {}

    @Schema(description = "规则切片质量评估（ChunkQualityEvaluator）")
    public record QualityConfig(
            @Schema(
                    description = "质量分阈值 0–100；chunking.aiMode=auto 时，低于该分触发 T2 千问重切",
                    example = "70",
                    minimum = "0",
                    maximum = "100"
            )
            int scoreThreshold
    ) {}

    @Schema(description = "千问语义重切配置（SemanticResplitService）")
    public record AiConfig(
            @Schema(description = "语义重切使用的对话模型", example = "qwen-plus")
            String chunkModel,
            @Schema(description = "单个文档允许的最大千问调用次数", example = "1", minimum = "0")
            int maxCallsPerDoc,
            @Schema(description = "送入模型的原文最大 token 数，超出会截断", example = "8000", minimum = "1")
            int maxInputTokens,
            @Schema(description = "模型返回 JSON 解析失败时的重试次数", example = "1", minimum = "0")
            int retryOnParseError
    ) {}

    @Schema(description = "向量化配置（EmbeddingService）")
    public record EmbeddingConfig(
            @Schema(description = "Embedding 模型名，须与 knowledge_base.embedding_dimensions 匹配", example = "text-embedding-v3")
            String model
    ) {}

    @Schema(description = "检索问答配置（RagChatService / VectorStore）")
    public record RetrievalConfig(
            @Schema(description = "每次问答返回的最大切片条数", example = "3", minimum = "1")
            int topK,
            @Schema(description = "向量余弦相似度下限（0–1），低于阈值的切片不进入上下文", example = "0.3", minimum = "0", maximum = "1")
            double scoreThreshold
    ) {}
}
