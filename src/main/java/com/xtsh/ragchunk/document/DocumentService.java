package com.xtsh.ragchunk.document;

import com.xtsh.ragchunk.document.dto.DocumentResponse;
import com.xtsh.ragchunk.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.web.NotFoundException;
import org.springframework.stereotype.Service;

/**
 * 文档查询（上传一律异步，见 {@link DocumentAsyncUploadService}）。
 */
@Service
public class DocumentService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentStore documentStore;
    private final DocumentResponseMapper responseMapper;

    public DocumentService(KnowledgeBaseService knowledgeBaseService, DocumentStore documentStore,
                           DocumentResponseMapper responseMapper) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentStore = documentStore;
        this.responseMapper = responseMapper;
    }

    public DocumentResponse get(String kbId, String docId) {
        var doc = documentStore.findById(docId)
                .filter(d -> kbId.equals(d.getKbId()))
                .orElseThrow(() -> new NotFoundException("document not found"));
        return responseMapper.toResponse(doc);
    }

    public java.util.List<DocumentResponse> list(String kbId) {
        knowledgeBaseService.require(kbId);
        return documentStore.findByKbId(kbId).stream().map(responseMapper::toResponse).toList();
    }
}
