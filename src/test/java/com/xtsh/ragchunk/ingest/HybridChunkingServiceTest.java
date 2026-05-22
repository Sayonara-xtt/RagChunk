package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HybridChunkingServiceTest {

    private final KnowledgeBaseConfig CONFIG = new KnowledgeBaseConfig(
            new KnowledgeBaseConfig.ChunkingConfig("hybrid", "never"),
            new KnowledgeBaseConfig.RuleConfig(300, 50, 0, List.of("\n\n"), List.of("\n## ", "\n\n"), true),
            new KnowledgeBaseConfig.QualityConfig(70),
            new KnowledgeBaseConfig.AiConfig("qwen-plus", 1, 8000, 1),
            new KnowledgeBaseConfig.EmbeddingConfig("text-embedding-v3"),
            new KnowledgeBaseConfig.RetrievalConfig(3, 0.5)
    );

    @Test
    void usesRuleWhenAiNever() {
        var svc = buildService();
        String text = "段落一内容。\n\n段落二内容。\n\n段落三内容。";
        var result = svc.chunk("doc_test", text, "test.md", CONFIG, false);
        assertFalse(result.aiTriggered());
        assertFalse(result.chunks().isEmpty());
    }

    private HybridChunkingService buildService() {
        var props = new com.xtsh.ragchunk.config.RagChunkProperties();
        return new HybridChunkingService(
                new RuleChunker(),
                new ChunkQualityEvaluator(),
                new AiChunkTrigger(),
                new SemanticResplitService(new DashScopeHttpClient(props), props, new RuleChunker()),
                new ChunkValidationService()
        );
    }
}
