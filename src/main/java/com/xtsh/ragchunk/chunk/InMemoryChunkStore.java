package com.xtsh.ragchunk.chunk;

import com.xtsh.ragchunk.chunk.model.StoredChunk;
import com.xtsh.ragchunk.vector.InMemoryVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "inmemory")
public class InMemoryChunkStore implements ChunkStore {

    private final InMemoryVectorStore vectorStore;

    public InMemoryChunkStore(InMemoryVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<StoredChunk> findByKbId(String kbId) {
        return vectorStore.listRecords().stream()
                .filter(r -> kbId.equals(r.kbId()))
                .sorted(Comparator.comparing(com.xtsh.ragchunk.vector.VectorRecord::docId)
                        .thenComparingInt(com.xtsh.ragchunk.vector.VectorRecord::chunkIndex))
                .map(this::toStored)
                .collect(Collectors.toList());
    }

    @Override
    public List<StoredChunk> findByDocId(String kbId, String docId) {
        return vectorStore.listRecords().stream()
                .filter(r -> kbId.equals(r.kbId()) && docId.equals(r.docId()))
                .sorted(Comparator.comparingInt(com.xtsh.ragchunk.vector.VectorRecord::chunkIndex))
                .map(this::toStored)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<StoredChunk> findById(String kbId, String chunkId) {
        return vectorStore.listRecords().stream()
                .filter(r -> kbId.equals(r.kbId()) && chunkId.equals(r.chunkId()))
                .findFirst()
                .map(this::toStored);
    }

    private StoredChunk toStored(com.xtsh.ragchunk.vector.VectorRecord r) {
        var c = new StoredChunk();
        c.setId(r.chunkId());
        c.setKbId(r.kbId());
        c.setDocId(r.docId());
        c.setChunkIndex(r.chunkIndex());
        c.setTextContent(r.text());
        c.setSource(r.source());
        c.setCreatedAt(Instant.now());
        return c;
    }
}
