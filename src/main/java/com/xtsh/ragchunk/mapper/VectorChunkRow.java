package com.xtsh.ragchunk.mapper;

import lombok.Data;

/** Database projection used by vector persistence mappers. */
@Data
public class VectorChunkRow {

    private String id;
    private String kbId;
    private String docId;
    private int chunkIndex;
    private String textContent;
    private String source;
    private float[] embedding;
    private Double score;
}
