package com.xtsh.ragchunk.chunk;

import com.xtsh.ragchunk.chunk.model.StoredChunk;

import java.util.List;
import java.util.Optional;

public interface ChunkStore {

    List<StoredChunk> findByKbId(String kbId);

    List<StoredChunk> findByDocId(String kbId, String docId);

    Optional<StoredChunk> findById(String kbId, String chunkId);
}
