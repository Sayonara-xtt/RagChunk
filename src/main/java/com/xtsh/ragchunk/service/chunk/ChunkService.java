package com.xtsh.ragchunk.service.chunk;

import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.vo.chunk.ChunkResponse;
import com.xtsh.ragchunk.dto.chunk.StoredChunk;
import com.xtsh.ragchunk.service.document.DocumentService;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.service.chunk.ChunkStore;
import com.xtsh.ragchunk.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ChunkService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final ChunkStore chunkStore;

    /**
     * 文档切片列表。docId 为空时返回该知识库下全部切片；有值时校验文档归属后按文档查询。
     */
    public List<ChunkResponse> listByDocument(String kbId, String docId) {
        knowledgeBaseService.require(kbId);
        if (docId == null || docId.isBlank()) {
            return chunkStore.findByKbId(kbId).stream().map(this::toResponse).collect(Collectors.toList());
        }
        String id = docId.trim();
        documentService.requireInKb(kbId, id);
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
            documentService.requireInKb(kbId, docId.trim());
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
