package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleChunkerTest {

    private final KnowledgeBaseConfig.RuleConfig RULE = new KnowledgeBaseConfig.RuleConfig(
            200, 50, 0, List.of("\n\n", "\n"), List.of("\n## ", "\n\n"), true);

    @Test
    void splitsLongText() {
        String text = "第一句内容比较长需要被切分。".repeat(30);
        var chunks = new RuleChunker().chunk(text, ChunkProfileDetector.PLAIN, RULE);
        assertTrue(chunks.size() > 1);
    }
}
