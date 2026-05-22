package com.xtsh.ragchunk.vector;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "inmemory")
public class InMemoryVectorStore implements VectorStore {

    private final CopyOnWriteArrayList<VectorRecord> records = new CopyOnWriteArrayList<>();

    /** 供 InMemoryChunkStore 查询切片列表 */
    public List<VectorRecord> listRecords() {
        return List.copyOf(records);
    }

    @Override
    public void upsert(VectorRecord record) {
        records.removeIf(r -> r.chunkId().equals(record.chunkId()));
        records.add(record);
    }

    @Override
    public void deleteByDocId(String docId) {
        records.removeIf(r -> r.docId().equals(docId));
    }

    @Override
    public void deleteByKbId(String kbId) {
        records.removeIf(r -> r.kbId().equals(kbId));
    }

    @Override
    public List<ScoredChunk> search(String kbId, float[] query, int topK, double minScore) {
        List<ScoredChunk> scored = new ArrayList<>();
        for (VectorRecord r : records) {
            if (!r.kbId().equals(kbId)) continue;
            double sim = cosine(query, r.embedding());
            if (sim >= minScore) scored.add(new ScoredChunk(r, sim));
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .collect(Collectors.toList());
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
