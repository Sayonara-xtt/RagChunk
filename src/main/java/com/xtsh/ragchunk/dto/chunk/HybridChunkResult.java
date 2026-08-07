package com.xtsh.ragchunk.dto.chunk;

import java.util.List;

public record HybridChunkResult(
        List<TextChunk> chunks,
        String profile,
        QualityReport qualityReport,
        boolean aiTriggered,
        String aiTriggerId,
        String aiTask,
        boolean aiFallback
) {}
