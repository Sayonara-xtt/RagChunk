package com.xtsh.ragchunk.vector;

import com.xtsh.ragchunk.mapper.VectorChunkMapper;
import com.xtsh.ragchunk.mapper.VectorChunkRow;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class VectorStorePersistenceBoundaryTest {

    @Test
    void pgVectorStoreDelegatesSqlAndMapsScoredRows() {
        VectorChunkMapper mapper = mock(VectorChunkMapper.class);
        PgVectorStore store = new PgVectorStore(mapper);
        var record = new VectorRecord("kb-1", "doc-1", "chunk-1", 0, "text", "source", new float[]{1, 0});
        var row = row("chunk-1", "kb-1", "doc-1", 0, "text", "source", null, 0.91);
        when(mapper.searchPgVector("kb-1", record.embedding(), 5, 0.7)).thenReturn(List.of(row));

        store.upsert(record);
        List<ScoredChunk> result = store.search("kb-1", record.embedding(), 5, 0.7);

        verify(mapper).upsertPgVector(record);
        assertEquals(1, result.size());
        assertEquals("chunk-1", result.get(0).record().chunkId());
        assertEquals(0.91, result.get(0).score());
    }

    @Test
    void arrayStoreDelegatesSqlAndKeepsCosineFilteringAndOrdering() {
        VectorChunkMapper mapper = mock(VectorChunkMapper.class);
        JdbcArrayVectorStore store = new JdbcArrayVectorStore(mapper);
        var best = row("best", "kb-1", "doc-1", 0, "best", "source", new float[]{1, 0}, null);
        var belowThreshold = row("low", "kb-1", "doc-1", 1, "low", "source", new float[]{0, 1}, null);
        when(mapper.findArrayByKbId("kb-1")).thenReturn(List.of(belowThreshold, best));

        List<ScoredChunk> result = store.search("kb-1", new float[]{1, 0}, 1, 0.5);

        assertEquals(1, result.size());
        assertEquals("best", result.get(0).record().chunkId());
        assertEquals(1.0, result.get(0).score());
    }

    private static VectorChunkRow row(String id, String kbId, String docId, int chunkIndex,
                                      String text, String source, float[] embedding, Double score) {
        var row = new VectorChunkRow();
        row.setId(id);
        row.setKbId(kbId);
        row.setDocId(docId);
        row.setChunkIndex(chunkIndex);
        row.setTextContent(text);
        row.setSource(source);
        row.setEmbedding(embedding);
        row.setScore(score);
        return row;
    }
}
