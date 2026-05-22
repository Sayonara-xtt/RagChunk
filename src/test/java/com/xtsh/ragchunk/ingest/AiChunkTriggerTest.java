package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.QualityReport;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiChunkTriggerTest {

    private final AiChunkTrigger trigger = new AiChunkTrigger();

  private KnowledgeBaseConfig config(String aiMode) {
        return new KnowledgeBaseConfig(
                new KnowledgeBaseConfig.ChunkingConfig("hybrid", aiMode),
                new KnowledgeBaseConfig.RuleConfig(1200, 80, 80, List.of(), List.of(), true),
                new KnowledgeBaseConfig.QualityConfig(70),
                new KnowledgeBaseConfig.AiConfig("qwen-plus", 1, 8000, 1),
                new KnowledgeBaseConfig.EmbeddingConfig("text-embedding-v3"),
                new KnowledgeBaseConfig.RetrievalConfig(3, 0.5)
        );
    }

    @Test
    void smartChunkTriggersT8() {
        var d = trigger.decide(config("auto"), new QualityReport(5, 0, 0, false, 90), true);
        assertTrue(d.shouldRunAi());
        assertEquals(AiChunkTrigger.T8, d.triggerId());
    }

    @Test
    void lowQualityTriggersT2() {
        var d = trigger.decide(config("auto"), new QualityReport(5, 0.5, 0.5, false, 50), false);
        assertTrue(d.shouldRunAi());
        assertEquals(AiChunkTrigger.T2, d.triggerId());
    }
}
