package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.QualityReport;
import com.xtsh.ragchunk.chunk.model.TextChunk;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 规则切片质量评估（不调用 LLM），产出 quality_score 供 {@link AiChunkTrigger} 在 aiMode=auto 时判断是否触发 T2/T4。
 */
@Component
public class ChunkQualityEvaluator {

    public QualityReport evaluate(List<TextChunk> chunks, int totalTextLength, KnowledgeBaseConfig.RuleConfig rule) {
        int n = chunks.size();
        if (n == 0) {
            return new QualityReport(0, 1.0, 1.0, totalTextLength > 1500, 0);
        }
        long shortCount = chunks.stream().filter(c -> c.charLen() < rule.minChars()).count();
        long weakCount = chunks.stream().filter(TextChunk::isWeakBoundary).count();
        double shortRatio = (double) shortCount / n;
        double weakRatio = (double) weakCount / n;
        // 长文仅 1 段 → 易触发 T4 千问重切
        boolean singleChunk = totalTextLength > 1500 && n == 1;
        // 一期简化公式：short*40 + weak*50 + 单段*30，见 docs/archive/phase1-scope.md
        int score = (int) Math.round(100 - shortRatio * 40 - weakRatio * 50 - (singleChunk ? 30 : 0));
        score = Math.max(0, Math.min(100, score));
        return new QualityReport(n, shortRatio, weakRatio, singleChunk, score);
    }
}
