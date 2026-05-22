package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.TextChunk;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkValidationServiceTest {

    private final ChunkValidationService service = new ChunkValidationService();
    private final KnowledgeBaseConfig.RuleConfig rule =
            new KnowledgeBaseConfig.RuleConfig(1200, 80, 80, List.of("\n\n"), List.of("\n## "), true);

    @Test
    void passesWhenLengthAndCoverageOk() {
        var chunks = List.of(
                new TextChunk(0, "a".repeat(100)),
                new TextChunk(1, "b".repeat(100))
        );
        var r = service.validateDetailed(chunks, "a".repeat(100) + "b".repeat(100), rule, 2);
        assertTrue(r.passed());
        assertEquals("OK", r.code());
    }

    @Test
    void failsV1TooShort() {
        var chunks = List.of(new TextChunk(0, "短"));
        var r = service.validateDetailed(chunks, "短文本", rule, 1);
        assertFalse(r.passed());
        assertEquals("V1_TOO_SHORT", r.code());
    }

    @Test
    void failsV3TooManyChunks() {
        var chunks = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> new TextChunk(i, "x".repeat(100)))
                .toList();
        String original = "x".repeat(1000);
        var r = service.validateDetailed(chunks, original, rule, 2);
        assertFalse(r.passed());
        assertEquals("V3_CHUNK_COUNT", r.code());
    }

    @Test
    void failsV2LowCoverage() {
        String kept = "x".repeat(80);
        String original = kept + "y".repeat(400);
        var chunks = List.of(new TextChunk(0, kept));
        var r = service.validateDetailed(chunks, original, rule, 1);
        assertFalse(r.passed());
        assertEquals("V2_COVERAGE", r.code());
        assertTrue(r.detail().contains("覆盖率"));
    }
}
