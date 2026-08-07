package com.xtsh.ragchunk.vo.chat;

import lombok.Data;
import com.xtsh.ragchunk.dto.chat.ChatRunStats;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "知识库问答响应")
@Data

public class ChatResponse {

    @Schema(description = "模型生成的回答；无 API Key 时为检索片段拼接说明")
    private String answer;

    @Schema(description = "检索命中的引用片段列表")
    private List<Citation> citations;

    @Schema(description = "运行统计：方案、LLM/检索次数等")
    private Meta meta;

    @Schema(description = "检索引用片段")
    public record Citation(
            @Schema(description = "切片 ID") String chunkId,
            @Schema(description = "来源文档 ID") String docId,
            @Schema(description = "切片序号（从 0 起）") int chunkIndex,
            @Schema(description = "向量相似度分数") double score,
            @Schema(description = "片段摘要文本") String excerpt
    ) {}

    @Schema(description = "问答运行元数据")
    @Data

    public static class Meta {
        @Schema(description = "方案编号：1/2/3/5", example = "1")
        private int scheme;
        @Schema(description = "方案标识", example = "pipeline")
        private String schemeName;
        @Schema(description = "LLM 调用次数")
        private int llmCalls;
        @Schema(description = "向量检索轮次")
        private int searchRounds;
        @Schema(description = "是否触发 Query 改写")
        private boolean rewriteTriggered;
        @Schema(description = "最终命中条数")
        private int hitCount;
        @Schema(description = "最高相似度")
        private double maxScore;

        public static Meta from(ChatRunStats stats) {
            var m = new Meta();
            m.scheme = stats.getScheme().code();
            m.schemeName = stats.getScheme().id();
            m.llmCalls = stats.getLlmCalls();
            m.searchRounds = stats.getSearchRounds();
            m.rewriteTriggered = stats.isRewriteTriggered();
            m.hitCount = stats.getHitCount();
            m.maxScore = stats.getMaxScore();
            return m;
        }

    }
}
