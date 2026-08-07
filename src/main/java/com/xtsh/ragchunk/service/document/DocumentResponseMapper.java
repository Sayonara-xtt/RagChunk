package com.xtsh.ragchunk.service.document;

import com.xtsh.ragchunk.vo.document.DocumentResponse;
import com.xtsh.ragchunk.vo.document.UploadBatchResponse;
import com.xtsh.ragchunk.dto.document.DocumentProcessStage;
import com.xtsh.ragchunk.dto.document.DocumentRecord;
import com.xtsh.ragchunk.dto.document.UploadBatchRecord;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentResponseMapper {

    public DocumentResponse toResponse(DocumentRecord doc) {
        var r = new DocumentResponse();
        r.setId(doc.getId());
        r.setKbId(doc.getKbId());
        r.setFileName(doc.getFileName());
        r.setStatus(doc.getStatus());
        r.setChunkCount(doc.getChunkCount());
        r.setProfile(doc.getProfile());
        r.setQualityScore(doc.getQualityScore());
        r.setAiTriggered(doc.isAiTriggered());
        r.setAiTriggerId(doc.getAiTriggerId());
        r.setAiFallback(doc.isAiFallback());
        r.setErrorMessage(doc.getErrorMessage());
        r.setCreatedAt(doc.getCreatedAt());
        r.setUpdatedAt(doc.getUpdatedAt());
        r.setBatchId(doc.getBatchId());
        r.setProcessStage(doc.getProcessStage());
        r.setProcessStageLabel(stageLabel(doc.getProcessStage()));
        r.setProgressPercent(doc.getProgressPercent());
        r.setFileSize(doc.getFileSize());
        r.setStorageKey(doc.getStorageKey());
        r.setStorageUrl(doc.getStorageUrl());
        r.setContentHash(doc.getContentHash());
        r.setSourceType(doc.getSourceType());
        r.setRetrainVersion(doc.getRetrainVersion());
        return r;
    }

    public UploadBatchResponse toBatchResponse(UploadBatchRecord batch, List<String> documentIds) {
        var r = new UploadBatchResponse();
        r.setId(batch.getId());
        r.setKbId(batch.getKbId());
        r.setSourceType(batch.getSourceType());
        r.setStatus(batch.getStatus());
        r.setTotalCount(batch.getTotalCount());
        r.setQueuedCount(batch.getQueuedCount());
        r.setProcessingCount(batch.getProcessingCount());
        r.setSuccessCount(batch.getSuccessCount());
        r.setFailedCount(batch.getFailedCount());
        r.setSmartChunk(batch.isSmartChunk());
        r.setDocumentIds(documentIds);
        r.setCreatedAt(batch.getCreatedAt());
        r.setUpdatedAt(batch.getUpdatedAt());
        return r;
    }

    private static String stageLabel(String stage) {
        if (stage == null) {
            return "";
        }
        try {
            return DocumentProcessStage.fromCode(stage).label();
        } catch (Exception e) {
            return stage;
        }
    }
}
