package com.xtsh.ragchunk.service.document;


import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.service.document.DocumentStore;
import com.xtsh.ragchunk.service.document.UploadBatchStore;
import com.xtsh.ragchunk.vo.document.UploadDashboardResponse;
import com.xtsh.ragchunk.dto.document.UploadBatchRecord;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 上传数据看板：按状态/流程阶段汇总，展示最近批次与文档。
 */
@RequiredArgsConstructor
@Service
public class UploadDashboardService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentStore documentStore;
    private final UploadBatchStore uploadBatchStore;
    private final DocumentResponseMapper responseMapper;


    public UploadDashboardResponse dashboard(String kbId) {
        knowledgeBaseService.require(kbId);
        var docs = documentStore.findByKbId(kbId);
        var batches = uploadBatchStore.findByKbId(kbId, 20);

        var resp = new UploadDashboardResponse();
        resp.setKbId(kbId);

        var summary = new UploadDashboardResponse.Summary();
        summary.setTotalDocuments(docs.size());
        summary.setTotalBatches(batches.size());
        summary.setQueued(docs.stream().filter(d -> "QUEUED".equals(d.getStatus())).count());
        summary.setProcessing(docs.stream().filter(d -> "PROCESSING".equals(d.getStatus())).count());
        summary.setSuccess(docs.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count());
        summary.setFailed(docs.stream().filter(d -> "FAILED".equals(d.getStatus())).count());
        resp.setSummary(summary);

        resp.setByStatus(docs.stream().collect(Collectors.groupingBy(
                d -> d.getStatus() != null ? d.getStatus() : "UNKNOWN", Collectors.counting())));
        resp.setByProcessStage(docs.stream().collect(Collectors.groupingBy(
                d -> d.getProcessStage() != null ? d.getProcessStage() : "UNKNOWN", Collectors.counting())));

        List<com.xtsh.ragchunk.vo.document.UploadBatchResponse> batchDtos = batches.stream()
                .map(b -> responseMapper.toBatchResponse(b, docs.stream()
                        .filter(d -> b.getId().equals(d.getBatchId()))
                        .map(com.xtsh.ragchunk.dto.document.DocumentRecord::getId)
                        .toList()))
                .toList();
        resp.setRecentBatches(batchDtos);

        resp.setRecentDocuments(docs.stream()
                .sorted((a, b) -> {
                    var ta = a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt();
                    var tb = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
                    if (ta == null || tb == null) return 0;
                    return tb.compareTo(ta);
                })
                .limit(50)
                .map(responseMapper::toResponse)
                .toList());

        return resp;
    }
}
