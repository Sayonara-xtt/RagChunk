package com.xtsh.ragchunk.vector;

public record VectorRecord(
        String kbId,
        String docId,
        String chunkId,
        int chunkIndex,
        String text,
        String source,
        float[] embedding
) {}
