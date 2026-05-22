package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemanticResplitPromptBuilderTest {

    private final KnowledgeBaseConfig.RuleConfig rule =
            new KnowledgeBaseConfig.RuleConfig(1200, 80, 80, List.of("\n\n"), List.of("\n## "), true);

    @Test
    void systemContainsRuleBounds() {
        String system = SemanticResplitPromptBuilder.buildSystem(rule);
        assertTrue(system.contains("80"));
        assertTrue(system.contains("1320"));
        assertTrue(system.contains("第一优先级"));
        assertTrue(system.contains("结构边界"));
        assertTrue(system.contains("第二优先级"));
        assertTrue(system.contains("第三优先级"));
        assertTrue(system.contains("95"));
    }

    @Test
    void suggestChunkCountFor1800() {
        var hint = SemanticResplitPromptBuilder.suggestChunkCount(1800, 1200);
        assertEquals(2, hint.target());
    }

    @Test
    void suggestChunkCountFor1200() {
        var hint = SemanticResplitPromptBuilder.suggestChunkCount(1200, 1200);
        assertEquals(1, hint.target());
    }

    @Test
    void userContainsProfileAndChunkCountHint() {
        String user = SemanticResplitPromptBuilder.buildUser(
                "正文", "manual.md", ChunkProfileDetector.MARKDOWN, rule, 2000, false);
        assertTrue(user.contains("manual.md"));
        assertTrue(user.contains("markdown"));
        assertTrue(user.contains("##"));
        assertTrue(user.contains("2000"));
    }

    @Test
    void retryHintMentionsValidation() {
        String hint = SemanticResplitPromptBuilder.buildRetryHint(rule);
        assertTrue(hint.contains("80"));
        assertTrue(hint.contains("1320"));
    }
}
