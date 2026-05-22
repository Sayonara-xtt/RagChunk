package com.xtsh.ragchunk.document;

import com.xtsh.ragchunk.document.dto.DocumentResponse;
import com.xtsh.ragchunk.document.model.DocumentRecord;
import com.xtsh.ragchunk.ingest.ChunkIngestPipeline;
import com.xtsh.ragchunk.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.vector.VectorStore;
import com.xtsh.ragchunk.web.BadRequestException;
import com.xtsh.ragchunk.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档上传与查询。上传为<strong>同步</strong>流程：单次 HTTP 请求内完成解析、切片、向量化并写库。
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentStore documentStore;
    private final ChunkIngestPipeline ingestPipeline;
    private final VectorStore vectorStore;

    public DocumentService(KnowledgeBaseService knowledgeBaseService, DocumentStore documentStore,
                           ChunkIngestPipeline ingestPipeline, VectorStore vectorStore) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentStore = documentStore;
        this.ingestPipeline = ingestPipeline;
        this.vectorStore = vectorStore;
    }

    /**
     * 上传并入库。
     *
     * @param kbId        知识库 ID，不存在则 404
     * @param file        multipart 文件流（一期不落对象存储，仅内存解析）
     * @param smartChunk  是否请求强制千问重切；最终是否调 AI 还受库配置 chunking.aiMode 约束（never 时无效）
     */
    public DocumentResponse upload(String kbId, MultipartFile file, boolean smartChunk) {
        long t0 = System.nanoTime();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required");
        }
        var kb = knowledgeBaseService.require(kbId);
        var cfg = kb.getConfig();
        log.info("[文档上传] 知识库已加载 kbId={}, aiMode={}, 切片模型={}, 向量模型={}",
                kbId, cfg.chunking().aiMode(), cfg.ai().chunkModel(), cfg.embedding().model());

        var doc = new DocumentRecord();
        doc.setId("doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        doc.setKbId(kbId);
        doc.setFileName(file.getOriginalFilename());
        doc.setStatus("PROCESSING");
        doc.setCreatedAt(Instant.now());
        documentStore.save(doc);
        log.info("[文档上传] docId={} 已落库，状态=处理中", doc.getId());

        try {
            ingestPipeline.ingest(doc, kb, file, smartChunk);
            doc.setStatus("SUCCESS");
            log.info("[文档上传] docId={} 入库成功 切片数={}, 画像={}, 质量分={}, "
                            + "AI已触发={}, 触发ID={}, AI回退={}, 耗时={}ms",
                    doc.getId(), doc.getChunkCount(), doc.getProfile(), doc.getQualityScore(),
                    doc.isAiTriggered(), doc.getAiTriggerId(), doc.isAiFallback(), elapsedMs(t0));
        } catch (Exception e) {
            vectorStore.deleteByDocId(doc.getId());
            doc.setStatus("FAILED");
            doc.setErrorMessage(e.getMessage());
            log.warn("[文档上传] docId={} 入库失败 耗时={}ms, 原因={}",
                    doc.getId(), elapsedMs(t0), e.getMessage(), e);
        }
        documentStore.save(doc);
        return toResponse(doc);
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }

    public DocumentResponse get(String kbId, String docId) {
        var doc = documentStore.findById(docId)
                .filter(d -> kbId.equals(d.getKbId()))
                .orElseThrow(() -> new NotFoundException("document not found"));
        return toResponse(doc);
    }

    public List<DocumentResponse> list(String kbId) {
        knowledgeBaseService.require(kbId);
        return documentStore.findByKbId(kbId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private DocumentResponse toResponse(DocumentRecord doc) {
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
        return r;
    }
}
