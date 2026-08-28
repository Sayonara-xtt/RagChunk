package com.xtsh.ragchunk.vector;

import com.xtsh.ragchunk.mapper.VectorChunkMapper;
import com.xtsh.ragchunk.mapper.VectorChunkRow;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

/**
 * 本机 PostgreSQL 持久化（无需 pgvector），向量存 real[]，检索在应用内算余弦相似度。
 */
@RequiredArgsConstructor
@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
@ConditionalOnProperty(name = "ragchunk.storage.vector-store", havingValue = "array")
public class JdbcArrayVectorStore implements VectorStore {

    private final VectorChunkMapper mapper;

    @Override
    public void upsert(VectorRecord record) {
        mapper.upsertArray(record);
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
        return mapper.findArrayByKbId(kbId).stream()
                .map(row -> {
                    VectorRecord record = toRecord(row);
                    return new ScoredChunk(record, cosine(query, record.embedding()));
                })
                .filter(s -> s.score() >= minScore)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private static VectorRecord toRecord(VectorChunkRow row) {
        return new VectorRecord(
                row.getKbId(),
                row.getDocId(),
                row.getId(),
                row.getChunkIndex(),
                row.getTextContent(),
                row.getSource(),
                row.getEmbedding()
        );
    }

    private static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
