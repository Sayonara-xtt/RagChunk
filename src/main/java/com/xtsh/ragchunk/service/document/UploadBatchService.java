package com.xtsh.ragchunk.service.document;

import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.vo.document.UploadBatchResponse;
import com.xtsh.ragchunk.service.document.DocumentStore;
import com.xtsh.ragchunk.service.document.UploadBatchStore;
import com.xtsh.ragchunk.exception.NotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UploadBatchService {

    private final UploadBatchStore uploadBatchStore;
    private final DocumentStore documentStore;
    private final DocumentResponseMapper responseMapper;

    public UploadBatchResponse getBatch(String kbId, String batchId) {
        var batch = uploadBatchStore.findById(batchId)
                .filter(b -> kbId.equals(b.getKbId()))
                .orElseThrow(() -> new NotFoundException("upload batch not found"));
        var docIds = documentStore.findByKbId(kbId).stream()
                .filter(d -> batchId.equals(d.getBatchId()))
                .map(d -> d.getId())
                .toList();
        return responseMapper.toBatchResponse(batch, docIds);
    }
}
