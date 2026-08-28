package com.xtsh.ragchunk.vector;

import com.xtsh.ragchunk.mapper.VectorChunkMapper;
import com.xtsh.ragchunk.mapper.VectorChunkRow;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
@ConditionalOnProperty(name = "ragchunk.storage.vector-store", havingValue = "pgvector", matchIfMissing = true)
public class PgVectorStore implements VectorStore {

    private final VectorChunkMapper mapper;

    @Override
    public void upsert(VectorRecord record) {
        mapper.upsertPgVector(record);
    }

    @Override
    public void deleteByDocId(String docId) {
        mapper.deleteByDocId(docId);
    }

    @Override
    public void deleteByKbId(String kbId) {
        mapper.deleteByKbId(kbId);
    }

    @Override
    public List<ScoredChunk> search(String kbId, float[] query, int topK, double minScore) {
        return mapper.searchPgVector(kbId, query, topK, minScore).stream()
                .map(row -> new ScoredChunk(toRecord(row, query), row.getScore()))
                .toList();
    }

    private static VectorRecord toRecord(VectorChunkRow row, float[] embedding) {
        return new VectorRecord(
                row.getKbId(),
                row.getDocId(),
                row.getId(),
                row.getChunkIndex(),
                row.getTextContent(),
                row.getSource(),
                embedding
        );
    }
}
