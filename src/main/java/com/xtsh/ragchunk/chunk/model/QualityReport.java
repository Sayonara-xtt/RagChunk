package com.xtsh.ragchunk.chunk.model;

public record QualityReport(
        int chunkCount,
        double shortRatio,
        double weakBoundaryRatio,
        boolean singleChunkDoc,
        int qualityScore
) {}
