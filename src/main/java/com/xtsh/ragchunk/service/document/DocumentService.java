package com.xtsh.ragchunk.service.document;

import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.vo.document.DocumentResponse;
import com.xtsh.ragchunk.dto.document.DocumentRecord;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.service.document.DocumentStore;
import com.xtsh.ragchunk.exception.NotFoundException;
import org.springframework.stereotype.Service;

/**
 * 文档查询（上传一律异步，见 {@link DocumentAsyncUploadService}）。
 */
@RequiredArgsConstructor
@Service
public class DocumentService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentStore documentStore;
    private final DocumentResponseMapper responseMapper;

    public DocumentResponse get(String kbId, String docId) {
        return responseMapper.toResponse(requireInKb(kbId, docId));
    }

    /**
     * 校验文档存在且归属指定知识库。
     */
    public DocumentRecord requireInKb(String kbId, String docId) {
        return documentStore.findById(docId)
                .filter(d -> kbId.equals(d.getKbId()))
                .orElseThrow(() -> new NotFoundException("document not found"));
    }

    public java.util.List<DocumentResponse> list(String kbId) {
        knowledgeBaseService.require(kbId);
        return documentStore.findByKbId(kbId).stream().map(responseMapper::toResponse).toList();
    }
}
