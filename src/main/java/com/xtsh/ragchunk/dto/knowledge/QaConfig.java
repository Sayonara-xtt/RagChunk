package com.xtsh.ragchunk.dto.knowledge;

import com.xtsh.ragchunk.config.RagChunkProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 智能问答编排配置（持久化在 knowledge_base.config_json.qa）。
 */
@Schema(description = "智能问答方案与受控上限")
public record QaConfig(
        @Schema(description = "方案：1=纯应用 2=协作渐进 3=协作全量改写 5=Agent", example = "1", allowableValues = {"1", "2", "3", "5"})
        int scheme,
        @Schema(description = "协作渐进时，低于该相似度触发改写（0-1）", example = "0.35")
        double rewriteMinScore,
        @Schema(description = "改写最多产出几条检索短句", example = "2", minimum = "1")
        int maxRewriteQueries,
        @Schema(description = "单次问答 LLM 调用上限", example = "2", minimum = "1")
        int maxLlmCalls,
        @Schema(description = "单次问答向量检索轮次上限", example = "2", minimum = "1")
        int maxSearchRounds,
        @Schema(description = "Agent 方案：LLM 对话轮次上限", example = "3", minimum = "1")
        int agentMaxIterations,
        @Schema(description = "Agent 每轮最多执行几次 search_kb", example = "1", minimum = "1")
        int agentMaxToolCallsPerRound,
        @Schema(description = "Agent 工具检索是否允许 relaxThreshold 放宽阈值")
        boolean agentAllowRelaxThreshold
) {
    public static QaConfig fromDefaults(RagChunkProperties props) {
        var q = props.getQa();
        return new QaConfig(
                q.getScheme(),
                q.getRewriteMinScore(),
                q.getMaxRewriteQueries(),
                q.getMaxLlmCalls(),
                q.getMaxSearchRounds(),
                q.getAgentMaxIterations(),
                q.getAgentMaxToolCallsPerRound(),
                q.isAgentAllowRelaxThreshold()
        );
    }
}
