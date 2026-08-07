package com.xtsh.ragchunk.service.document;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.service.document.DocumentStore;
import com.xtsh.ragchunk.service.document.UploadBatchStore;
import com.xtsh.ragchunk.vo.document.AsyncUploadResponse;
import com.xtsh.ragchunk.vo.document.UploadBatchResponse;
import com.xtsh.ragchunk.dto.document.DocumentProcessStage;
import com.xtsh.ragchunk.dto.document.DocumentRecord;
import com.xtsh.ragchunk.dto.document.UploadBatchRecord;
import com.xtsh.ragchunk.dto.document.UploadSourceType;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 异步/批量上传：请求线程流式归档原件，后台解析切片向量；支持重复训练。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class DocumentAsyncUploadService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentStore documentStore;
    private final UploadBatchStore uploadBatchStore;
    private final DocumentIngestExecutor ingestExecutor;
    private final DocumentStreamArchiveService streamArchiveService;
    private final DocumentResponseMapper responseMapper;


    public com.xtsh.ragchunk.vo.document.DocumentUploadResponse submit(String kbId, List<MultipartFile> files,
                                                                        boolean smartChunk, UploadSourceType sourceType) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("file or files is required");
        }
        var nonEmpty = files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (nonEmpty.isEmpty()) {
            throw new BadRequestException("no non-empty files");
        }
        if (nonEmpty.size() == 1) {
            var async = submitAsync(kbId, nonEmpty.get(0), smartChunk, sourceType);
            return com.xtsh.ragchunk.vo.document.DocumentUploadResponse.single(
                    async.getDocId(), async.getStatus(), async.getProcessStage());
        }
        var batch = submitBatch(kbId, nonEmpty, smartChunk,
                sourceType == UploadSourceType.API_SINGLE ? UploadSourceType.LOCAL_BATCH : sourceType);
        return com.xtsh.ragchunk.vo.document.DocumentUploadResponse.batch(batch);
    }

    public AsyncUploadResponse submitAsync(String kbId, MultipartFile file, boolean smartChunk,
                                           UploadSourceType sourceType) {
        knowledgeBaseService.require(kbId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required");
        }
        String fileName = file.getOriginalFilename();
        long sizeHint = file.getSize() >= 0 ? file.getSize() : 0;
        DocumentRecord doc = createQueuedDocument(kbId, null, fileName, sizeHint, sourceType.name());
        try {
            streamArchiveService.archiveFromUpload(doc, file);
        } catch (Exception e) {
            throw new BadRequestException("failed to archive upload: " + e.getMessage());
        }
        enqueueIngest(doc, kbId, null, fileName, smartChunk, sourceType.name(), false);
        log.info("[文档上传] 流式受理并入队 docId={}, file={}", doc.getId(), fileName);
        return AsyncUploadResponse.forDocument(doc.getId(), null, "QUEUED", doc.getProcessStage());
    }

    public UploadBatchResponse submitBatch(String kbId, List<MultipartFile> files, boolean smartChunk,
                                           UploadSourceType sourceType) {
        knowledgeBaseService.require(kbId);
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("files are required");
        }
        var batch = new UploadBatchRecord();
        batch.setId("ub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        batch.setKbId(kbId);
        batch.setSourceType(sourceType.name());
        batch.setSmartChunk(smartChunk);
        batch.setStatus("PROCESSING");
        batch.setTotalCount(files.size());
        batch.setQueuedCount(files.size());
        batch.setCreatedAt(Instant.now());
        uploadBatchStore.save(batch);

        List<String> docIds = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String fileName = file.getOriginalFilename();
            long sizeHint = file.getSize() >= 0 ? file.getSize() : 0;
            DocumentRecord doc = createQueuedDocument(kbId, batch.getId(), fileName, sizeHint, sourceType.name());
            try {
                streamArchiveService.archiveFromUpload(doc, file);
                docIds.add(doc.getId());
                enqueueIngest(doc, kbId, batch.getId(), fileName, smartChunk, sourceType.name(), false);
            } catch (Exception e) {
                log.warn("[文档上传] 批量归档失败 file={}: {}", fileName, e.getMessage());
                batch.setFailedCount(batch.getFailedCount() + 1);
                batch.setQueuedCount(Math.max(0, batch.getQueuedCount() - 1));
            }
        }
        batch.setProcessingCount(0);
        uploadBatchStore.save(batch);
        log.info("[文档上传] 批量流式受理 batchId={}, 成功入队={}", batch.getId(), docIds.size());
        return responseMapper.toBatchResponse(uploadBatchStore.findById(batch.getId()).orElse(batch), docIds);
    }

    public AsyncUploadResponse retrain(String kbId, String docId, boolean smartChunk) {
        var doc = documentStore.findById(docId)
                .filter(d -> kbId.equals(d.getKbId()))
                .orElseThrow(() -> new BadRequestException("document not found"));
        if (doc.getStorageKey() == null || doc.getStorageKey().isBlank()) {
            throw new BadRequestException("document has no archived original; upload first");
        }
        doc.setStatus("PROCESSING");
        doc.setProcessStage(DocumentProcessStage.QUEUED.name());
        doc.setProgressPercent(0);
        doc.setErrorMessage(null);
        doc.setUpdatedAt(Instant.now());
        documentStore.save(doc);

        enqueueIngest(doc, kbId, doc.getBatchId(), doc.getFileName(), smartChunk, doc.getSourceType(), true);
        log.info("[文档上传] 重复训练入队 docId={}, version={}", docId, doc.getRetrainVersion() + 1);
        return AsyncUploadResponse.forDocument(docId, doc.getBatchId(), "QUEUED", DocumentProcessStage.QUEUED.name());
    }

    private void enqueueIngest(DocumentRecord doc, String kbId, String batchId, String fileName,
                               boolean smartChunk, String sourceType, boolean retrain) {
        ingestExecutor.runAsync(new DocumentIngestJob(
                doc.getId(), kbId, batchId, fileName, smartChunk, sourceType, retrain));
    }

    private DocumentRecord createQueuedDocument(String kbId, String batchId, String fileName, long size, String sourceType) {
        var doc = new DocumentRecord();
        doc.setId("doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        doc.setKbId(kbId);
        doc.setBatchId(batchId);
        doc.setFileName(fileName);
        doc.setFileSize(size);
        doc.setSourceType(sourceType);
        doc.setStatus("QUEUED");
        doc.setProcessStage(DocumentProcessStage.QUEUED.name());
        doc.setProgressPercent(0);
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        return documentStore.save(doc);
    }
}
