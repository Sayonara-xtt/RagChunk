package com.xtsh.ragchunk.service.document;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.service.document.DocumentStore;
import com.xtsh.ragchunk.service.document.UploadBatchStore;
import com.xtsh.ragchunk.dto.document.DocumentProcessStage;
import com.xtsh.ragchunk.dto.document.DocumentRecord;
import com.xtsh.ragchunk.ingest.ChunkIngestPipeline;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.vector.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 文档入库执行器：原件已流式归档后，后台解析 → 切片 → 向量。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class DocumentIngestExecutor {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentStore documentStore;
    private final UploadBatchStore uploadBatchStore;
    private final ChunkIngestPipeline ingestPipeline;
    private final VectorStore vectorStore;
    private final DocumentProcessTracker processTracker;


    @Async("documentIngestTaskExecutor")
    public void runAsync(DocumentIngestJob job) {
        run(job);
    }

    private DocumentRecord run(DocumentIngestJob job) {
        long t0 = System.nanoTime();
        onDocStarted(job.batchId());
        var doc = documentStore.findById(job.docId()).orElseThrow();
        try {
            if (job.retrain()) {
                if (doc.getStorageKey() == null || doc.getStorageKey().isBlank()) {
                    throw new IllegalStateException("retrain requires archived original (storageKey missing)");
                }
                vectorStore.deleteByDocId(job.docId());
                doc.setRetrainVersion(doc.getRetrainVersion() + 1);
                documentStore.save(doc);
            } else if (doc.getStorageKey() == null || doc.getStorageKey().isBlank()) {
                throw new IllegalStateException("original not archived before ingest");
            }

            var kb = knowledgeBaseService.require(job.kbId());
            ingestPipeline.ingestFromStorage(doc, kb, doc.getStorageKey(), job.fileName(), job.smartChunk());
            doc.setStatus("SUCCESS");
            documentStore.save(doc);
            onDocFinished(job.batchId(), true);
            log.info("[文档上传] docId={} 异步入库完成 retrain={} 耗时={}ms",
                    job.docId(), job.retrain(), elapsedMs(t0));
        } catch (Exception e) {
            vectorStore.deleteByDocId(job.docId());
            processTracker.markFailed(job.docId(), e.getMessage());
            onDocFinished(job.batchId(), false);
            log.warn("[文档上传] docId={} 入库失败: {}", job.docId(), e.getMessage(), e);
        }
        return documentStore.findById(job.docId()).orElse(doc);
    }

    private void onDocStarted(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            return;
        }
        uploadBatchStore.findById(batchId).ifPresent(batch -> {
            batch.setQueuedCount(Math.max(0, batch.getQueuedCount() - 1));
            batch.setProcessingCount(batch.getProcessingCount() + 1);
            batch.setStatus("PROCESSING");
            batch.setUpdatedAt(java.time.Instant.now());
            uploadBatchStore.save(batch);
        });
    }

    private void onDocFinished(String batchId, boolean success) {
        if (batchId == null || batchId.isBlank()) {
            return;
        }
        uploadBatchStore.findById(batchId).ifPresent(batch -> {
            if (success) {
                batch.setSuccessCount(batch.getSuccessCount() + 1);
            } else {
                batch.setFailedCount(batch.getFailedCount() + 1);
            }
            batch.setProcessingCount(Math.max(0, batch.getProcessingCount() - 1));
            refreshBatchStatus(batch);
            uploadBatchStore.save(batch);
        });
    }

    private void refreshBatchStatus(com.xtsh.ragchunk.dto.document.UploadBatchRecord batch) {
        if (batch.getQueuedCount() > 0 || batch.getProcessingCount() > 0) {
            batch.setStatus("PROCESSING");
        } else if (batch.getFailedCount() > 0 && batch.getSuccessCount() == 0) {
            batch.setStatus("FAILED");
        } else if (batch.getFailedCount() > 0) {
            batch.setStatus("PARTIAL_SUCCESS");
        } else {
            batch.setStatus("SUCCESS");
        }
        batch.setUpdatedAt(java.time.Instant.now());
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
