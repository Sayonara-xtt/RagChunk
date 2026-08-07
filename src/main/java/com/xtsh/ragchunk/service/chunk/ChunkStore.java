package com.xtsh.ragchunk.service.chunk;

import com.xtsh.ragchunk.dto.chunk.StoredChunk;

import java.util.List;
import java.util.Optional;

public interface ChunkStore {

    List<StoredChunk> findByKbId(String kbId);

    List<StoredChunk> findByDocId(String kbId, String docId);

    Optional<StoredChunk> findById(String kbId, String chunkId);
}
