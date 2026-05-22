package com.xtsh.ragchunk.chunk;

import com.xtsh.ragchunk.chunk.dto.ChunkResponse;
import com.xtsh.ragchunk.chunk.model.StoredChunk;
import com.xtsh.ragchunk.document.DocumentStore;
import com.xtsh.ragchunk.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.web.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChunkService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentStore documentStore;
    private final ChunkStore chunkStore;

    public ChunkService(KnowledgeBaseService knowledgeBaseService, DocumentStore documentStore,
                        ChunkStore chunkStore) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentStore = documentStore;
        this.chunkStore = chunkStore;
    }

    /**
     * 文档切片列表。docId 为空时返回该知识库下全部切片；有值时校验文档归属后按文档查询。
     */
    public List<ChunkResponse> listByDocument(String kbId, String docId) {
        knowledgeBaseService.require(kbId);
        if (docId == null || docId.isBlank()) {
            return chunkStore.findByKbId(kbId).stream().map(this::toResponse).collect(Collectors.toList());
        }
        String id = docId.trim();
        documentStore.findById(id)
                .filter(d -> kbId.equals(d.getKbId()))
                .orElseThrow(() -> new NotFoundException("document not found"));
        return chunkStore.findByDocId(kbId, id).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Object query(String kbId, String docId, String chunkId) {
        knowledgeBaseService.require(kbId);
        if (chunkId != null && !chunkId.isBlank()) {
            return chunkStore.findById(kbId, chunkId.trim())
                    .map(this::toResponse)
                    .orElseThrow(() -> new NotFoundException("chunk not found"));
        }
        if (docId != null && !docId.isBlank()) {
            documentStore.findById(docId.trim())
                    .filter(d -> kbId.equals(d.getKbId()))
                    .orElseThrow(() -> new NotFoundException("document not found"));
            return chunkStore.findByDocId(kbId, docId.trim()).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return chunkStore.findByKbId(kbId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ChunkResponse toResponse(StoredChunk c) {
        var r = new ChunkResponse();
        r.setId(c.getId());
        r.setKbId(c.getKbId());
        r.setDocId(c.getDocId());
        r.setChunkIndex(c.getChunkIndex());
        r.setTextContent(c.getTextContent());
        r.setSource(c.getSource());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }
}
