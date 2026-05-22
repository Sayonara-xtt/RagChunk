package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.QualityReport;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 决定是否调用千问语义重切。判断顺序固定，上传接口的 {@code smartChunk} 仅在其中一步生效。
 */
@Component
public class AiChunkTrigger {

    private static final Logger log = LoggerFactory.getLogger(AiChunkTrigger.class);

    public static final String T0 = "T0";
    public static final String T1 = "T1";
    public static final String T2 = "T2";
    public static final String T4 = "T4";
    public static final String T8 = "T8";

    /** @param triggerId 写入 document.ai_trigger_id，如 T0/T8，便于排查 */
    public record TriggerDecision(boolean shouldRunAi, String triggerId) {}

    /**
     * @param smartChunk 上传参数；在 aiMode=never 时被忽略
     */
    public TriggerDecision decide(KnowledgeBaseConfig config, QualityReport report, boolean smartChunk) {
        String aiMode = config.chunking().aiMode();
        TriggerDecision decision;
        // 1. never：永不调千问（smartChunk 无效）
        if ("never".equalsIgnoreCase(aiMode)) {
            decision = new TriggerDecision(false, T1);
        } else if (smartChunk) {
            decision = new TriggerDecision(true, T8);
        } else if ("always".equalsIgnoreCase(aiMode)) {
            decision = new TriggerDecision(true, T0);
        } else if ("auto".equalsIgnoreCase(aiMode)) {
            if (report.qualityScore() < config.quality().scoreThreshold()) {
                decision = new TriggerDecision(true, T2);
            } else if (report.singleChunkDoc()) {
                decision = new TriggerDecision(true, T4);
            } else {
                decision = new TriggerDecision(false, null);
            }
        } else {
            decision = new TriggerDecision(false, null);
        }
        log.info("[文档上传] AI触发判定 aiMode={}, smartChunk={}, 质量分={}, 阈值={}, "
                        + "长文单段={}, 是否调AI={}, 触发ID={}（never 时 smartChunk 无效）",
                aiMode, smartChunk, report.qualityScore(), config.quality().scoreThreshold(),
                report.singleChunkDoc(), decision.shouldRunAi(), decision.triggerId());
        return decision;
    }
}
