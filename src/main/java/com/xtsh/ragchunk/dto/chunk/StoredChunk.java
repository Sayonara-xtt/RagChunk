package com.xtsh.ragchunk.dto.chunk;

import lombok.Data;
import java.time.Instant;

@Data

public class StoredChunk {

    private String id;
    private String kbId;
    private String docId;
    private int chunkIndex;
    private String textContent;
    private String source;
    private Instant createdAt;

}
