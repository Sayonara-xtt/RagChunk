package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.TextChunk;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 切片结果校验（V1 片长 + V2 覆盖率）。不通过则 {@link HybridChunkingService} 使用规则切片并设 aiFallback=true。
 */
@Component
public class ChunkValidationService {

    private static final double MIN_COVERAGE = 0.95;
    /** AI 段数相对规则段数的最大倍数（设计稿 V3） */
    private static final double MAX_CHUNK_COUNT_RATIO = 3.0;

    /**
     * @param chunkCount AI 切片段数（写入日志，便于与规则切片对比）
     */
    public record ValidationResult(
            boolean passed,
            String code,
            String detail,
            int chunkCount,
            double coverageRatio
    ) {
        static ValidationResult ok(int chunkCount, double coverageRatio) {
            return new ValidationResult(true, "OK", "校验通过", chunkCount, coverageRatio);
        }

        static ValidationResult fail(String code, String detail, int chunkCount, double coverageRatio) {
            return new ValidationResult(false, code, detail, chunkCount, coverageRatio);
        }
    }

    public boolean validate(List<TextChunk> chunks, String originalText, KnowledgeBaseConfig.RuleConfig rule,
                            int ruleChunkCount) {
        return validateDetailed(chunks, originalText, rule, ruleChunkCount).passed();
    }

    /**
     * 逐项校验并返回失败原因，供上传链路打明细日志。
     *
     * @param ruleChunkCount 规则切片段数，用于 V3 段数变化率校验；≤0 时跳过 V3
     */
    public ValidationResult validateDetailed(List<TextChunk> chunks, String originalText,
                                             KnowledgeBaseConfig.RuleConfig rule, int ruleChunkCount) {
        if (chunks == null || chunks.isEmpty()) {
            return ValidationResult.fail("EMPTY", "AI 切片列表为空", 0, 0);
        }
        int maxAllowed = (int) (rule.maxChars() * 1.1);
        for (TextChunk c : chunks) {
            if (c.getText() == null || c.getText().isBlank()) {
                return ValidationResult.fail("V1_BLANK",
                        "第 %d 段正文为空".formatted(c.getIndex()), chunks.size(), 0);
            }
            int len = c.charLen();
            if (len < rule.minChars()) {
                return ValidationResult.fail("V1_TOO_SHORT",
                        "第 %d 段过短：%d 字，要求 >= %d（minChars）".formatted(
                                c.getIndex(), len, rule.minChars()), chunks.size(), 0);
            }
            if (len > maxAllowed) {
                return ValidationResult.fail("V1_TOO_LONG",
                        "第 %d 段过长：%d 字，要求 <= %d（maxChars×1.1=%d）".formatted(
                                c.getIndex(), len, maxAllowed, maxAllowed), chunks.size(), 0);
            }
        }
        String merged = String.join("", chunks.stream().map(TextChunk::getText).toList())
                .replaceAll("\\s+", "");
        String orig = originalText.replaceAll("\\s+", "");
        if (orig.isEmpty()) {
            return ValidationResult.ok(chunks.size(), 1.0);
        }
        double ratio = (double) merged.length() / orig.length();
        if (ratio < MIN_COVERAGE) {
            return ValidationResult.fail("V2_COVERAGE",
                    "合并正文覆盖率 %.1f%%（%d/%d 字，去空白后），要求 >= %.0f%%".formatted(
                            ratio * 100, merged.length(), orig.length(), MIN_COVERAGE * 100),
                    chunks.size(), ratio);
        }
        if (ruleChunkCount > 0) {
            double countRatio = (double) chunks.size() / ruleChunkCount;
            if (countRatio > MAX_CHUNK_COUNT_RATIO) {
                return ValidationResult.fail("V3_CHUNK_COUNT",
                        "AI 段数 %d 为规则段数 %d 的 %.1f 倍，要求不超过 %.0f 倍".formatted(
                                chunks.size(), ruleChunkCount, countRatio, MAX_CHUNK_COUNT_RATIO),
                        chunks.size(), ratio);
            }
        }
        return ValidationResult.ok(chunks.size(), ratio);
    }
}
