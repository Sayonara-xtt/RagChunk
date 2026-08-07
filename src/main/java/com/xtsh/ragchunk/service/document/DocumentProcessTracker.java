package com.xtsh.ragchunk.service.document;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.service.document.DocumentStore;
import com.xtsh.ragchunk.dto.document.DocumentProcessStage;
import com.xtsh.ragchunk.dto.document.DocumentRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 更新文档处理阶段与进度（异步线程中调用，供上传流程查询）。
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class DocumentProcessTracker {

    private final DocumentStore documentStore;


    public void updateStage(String docId, DocumentProcessStage stage) {
        documentStore.findById(docId).ifPresent(doc -> {
            doc.setProcessStage(stage.name());
            doc.setProgressPercent(stage.defaultProgress());
            if (stage == DocumentProcessStage.SUCCESS) {
                doc.setStatus("SUCCESS");
                doc.setProgressPercent(100);
            } else if (stage == DocumentProcessStage.FAILED) {
                doc.setStatus("FAILED");
            } else {
                doc.setStatus("PROCESSING");
            }
            doc.setUpdatedAt(Instant.now());
            documentStore.save(doc);
            log.info("[文档上传] docId={} 流程阶段 -> {} ({}) 进度={}%",
                    docId, stage.name(), stage.label(), doc.getProgressPercent());
        });
    }

    public void markFailed(String docId, String message) {
        documentStore.findById(docId).ifPresent(doc -> {
            doc.setStatus("FAILED");
            doc.setProcessStage(DocumentProcessStage.FAILED.name());
            doc.setProgressPercent(0);
            doc.setErrorMessage(message);
            doc.setUpdatedAt(Instant.now());
            documentStore.save(doc);
            log.warn("[文档上传] docId={} 流程失败: {}", docId, message);
        });
    }
}
