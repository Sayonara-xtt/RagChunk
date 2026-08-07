package com.xtsh.ragchunk.dto.chunk;

public record QualityReport(
        int chunkCount,
        double shortRatio,
        double weakBoundaryRatio,
        boolean singleChunkDoc,
        int qualityScore
) {}
