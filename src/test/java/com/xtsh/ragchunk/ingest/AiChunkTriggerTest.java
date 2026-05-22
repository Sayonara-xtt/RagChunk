package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.QualityReport;
import com.xtsh.ragchunk.testutil.TestKnowledgeBaseConfigs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiChunkTriggerTest {

    private final AiChunkTrigger trigger = new AiChunkTrigger();

  private com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig config(String aiMode) {
        return TestKnowledgeBaseConfigs.minimal(aiMode);
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
