package com.xtsh.ragchunk.vector;

import java.util.List;

public interface VectorStore {

    void upsert(VectorRecord record);

    void deleteByDocId(String docId);

    void deleteByKbId(String kbId);

    List<ScoredChunk> search(String kbId, float[] query, int topK, double minScore);
}
