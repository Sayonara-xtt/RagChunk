package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.TextChunk;
import com.xtsh.ragchunk.document.model.DocumentRecord;
import com.xtsh.ragchunk.embedding.EmbeddingService;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBase;
import com.xtsh.ragchunk.vector.VectorRecord;
import com.xtsh.ragchunk.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档入库流水线：解析 → 规范化 → 混合切片 → 逐片 Embedding → 写入 chunk 表（pgvector）。
 */
@Service
public class ChunkIngestPipeline {

    private static final Logger log = LoggerFactory.getLogger(ChunkIngestPipeline.class);

    private final DocumentParser documentParser;
    private final TextNormalizer textNormalizer;
    private final HybridChunkingService hybridChunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public ChunkIngestPipeline(DocumentParser documentParser, TextNormalizer textNormalizer,
                               HybridChunkingService hybridChunkingService, EmbeddingService embeddingService,
                               VectorStore vectorStore) {
        this.documentParser = documentParser;
        this.textNormalizer = textNormalizer;
        this.hybridChunkingService = hybridChunkingService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    /**
     * 执行入库；异常向上抛出，由 DocumentService 标记 FAILED 并删除已写入向量。
     */
    public void ingest(DocumentRecord doc, KnowledgeBase kb, MultipartFile file, boolean smartChunk) throws Exception {
        String docId = doc.getId();
        log.info("[文档上传] docId={} 流水线开始 文件={}, smartChunk={}", docId, file.getOriginalFilename(), smartChunk);

        long tParse = System.nanoTime();
        String raw = documentParser.parse(file);
        String text = textNormalizer.normalize(raw);
        if (text.isBlank()) {
            throw new IllegalArgumentException("document is empty after normalization");
        }
        log.info("[文档上传] docId={} 解析与规范化完成 原始字数={}, 正文字数={}, 耗时={}ms",
                docId, raw.length(), text.length(), stepMs(tParse));

        long tChunk = System.nanoTime();
        var result = hybridChunkingService.chunk(docId, text, file.getOriginalFilename(), kb.getConfig(), smartChunk);
        doc.setProfile(result.profile());
        doc.setQualityScore(result.qualityReport().qualityScore());
        doc.setAiTriggered(result.aiTriggered());
        doc.setAiTriggerId(result.aiTriggerId());
        doc.setAiFallback(result.aiFallback());
        long hybridSources = result.chunks().stream().filter(c -> "hybrid".equals(c.getSource())).count();
        log.info("[文档上传] docId={} 混合切片完成 画像={}, 规则质量分={}, 切片数={}, AI切片数={}, "
                        + "AI已触发={}, 触发ID={}, AI回退={}, 耗时={}ms",
                docId, result.profile(), result.qualityReport().qualityScore(), result.chunks().size(), hybridSources,
                result.aiTriggered(), result.aiTriggerId(), result.aiFallback(), stepMs(tChunk));

        long tEmbed = System.nanoTime();
        int i = 0;
        for (TextChunk chunk : result.chunks()) {
            String chunkId = doc.getId() + "_c" + String.format("%04d", i);
            float[] vec = embeddingService.embed(chunk.getText(), kb.getConfig().embedding());
            vectorStore.upsert(new VectorRecord(
                    kb.getId(), doc.getId(), chunkId, i, chunk.getText(), chunk.getSource(), vec));
            i++;
        }
        doc.setChunkCount(i);
        log.info("[文档上传] docId={} 向量写入完成 切片数={}, 耗时={}ms",
                docId, i, stepMs(tEmbed));
    }

    private static long stepMs(long stepStartNano) {
        return (System.nanoTime() - stepStartNano) / 1_000_000;
    }
}
