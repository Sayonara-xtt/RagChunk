package com.xtsh.ragchunk.service.document;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.service.document.DocumentStore;
import com.xtsh.ragchunk.dto.document.DocumentProcessStage;
import com.xtsh.ragchunk.dto.document.DocumentRecord;
import com.xtsh.ragchunk.objectstorage.ObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/**
 * 请求线程内将 multipart 流式写入原件存储，避免 {@code MultipartFile#getBytes()} 占满堆。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class DocumentStreamArchiveService {

    private final DocumentStore documentStore;
    private final ObjectStorageService objectStorage;
    private final DocumentProcessTracker processTracker;


    /**
     * 流式归档并更新文档记录；失败时标记 FAILED。
     */
    public void archiveFromUpload(DocumentRecord doc, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        long knownSize = file.getSize() >= 0 ? file.getSize() : -1;
        processTracker.updateStage(doc.getId(), DocumentProcessStage.OSS_ARCHIVING);
        try (var input = file.getInputStream()) {
            var result = objectStorage.putStream(doc.getKbId(), doc.getId(), fileName, input, knownSize);
            doc.setStorageKey(result.stored().storageKey());
            doc.setStorageUrl(result.stored().storageUrl());
            doc.setFileSize(result.stored().size());
            doc.setContentHash(result.contentHash());
            doc.setUpdatedAt(Instant.now());
            documentStore.save(doc);
            log.info("[文档上传] 流式归档完成 docId={}, size={}B", doc.getId(), result.stored().size());
        } catch (Exception e) {
            processTracker.markFailed(doc.getId(), "archive failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
