package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.TextChunk;
import com.xtsh.ragchunk.document.DocumentProcessTracker;
import com.xtsh.ragchunk.document.model.DocumentProcessStage;
import com.xtsh.ragchunk.document.model.DocumentRecord;
import com.xtsh.ragchunk.embedding.EmbeddingService;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBase;
import com.xtsh.ragchunk.storage.ObjectStorageService;
import com.xtsh.ragchunk.vector.VectorRecord;
import com.xtsh.ragchunk.vector.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 鏂囨。鍏ュ簱娴佹按绾匡細瑙ｆ瀽 鈫?瑙勮寖鍖?鈫?娣峰悎鍒囩墖 鈫?閫愮墖 Embedding 鈫?鍐欏叆 chunk 琛紙pgvector锛夈€? */
@Service
public class ChunkIngestPipeline {

    private static final Logger log = LoggerFactory.getLogger(ChunkIngestPipeline.class);

    private final DocumentParser documentParser;
    private final TextNormalizer textNormalizer;
    private final HybridChunkingService hybridChunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final DocumentProcessTracker processTracker;
    private final ObjectStorageService objectStorage;

    public ChunkIngestPipeline(DocumentParser documentParser, TextNormalizer textNormalizer,
                               HybridChunkingService hybridChunkingService, EmbeddingService embeddingService,
                               VectorStore vectorStore, DocumentProcessTracker processTracker,
                               ObjectStorageService objectStorage) {
        this.documentParser = documentParser;
        this.textNormalizer = textNormalizer;
        this.hybridChunkingService = hybridChunkingService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.processTracker = processTracker;
        this.objectStorage = objectStorage;
    }

    /**
     * 鎵ц鍏ュ簱锛涘紓甯稿悜涓婃姏鍑猴紝鐢辫皟鐢ㄦ柟鏍囪 FAILED 骞跺垹闄ゅ凡鍐欏叆鍚戦噺銆?     */
    public void ingest(DocumentRecord doc, KnowledgeBase kb, MultipartFile file, boolean smartChunk) throws Exception {
        try (var in = file.getInputStream()) {
            runPipeline(doc, kb, () -> documentParser.parseStream(in, file.getOriginalFilename()),
                    file.getOriginalFilename(), smartChunk);
        }
    }

    /**
     * 浠庡凡褰掓。鍘熶欢娴佸紡瑙ｆ瀽骞跺叆搴擄紙涓嶅皢鏁翠釜鏂囦欢杞藉叆鍫嗭級銆?     */
    public void ingestFromStorage(DocumentRecord doc, KnowledgeBase kb, String storageKey, String fileName,
                                  boolean smartChunk) throws Exception {
        try (var in = objectStorage.openStream(storageKey)) {
            runPipeline(doc, kb, () -> documentParser.parseStream(in, fileName), fileName, smartChunk);
        }
    }

    /**
     * 浠庡瓧鑺傛墽琛屽叏娴佺▼锛堟祴璇曠敤锛夈€?     */
    public void ingestBytes(DocumentRecord doc, KnowledgeBase kb, byte[] fileBytes, String fileName, boolean smartChunk)
            throws Exception {
        runPipeline(doc, kb, () -> documentParser.parseBytes(fileBytes, fileName), fileName, smartChunk);
    }

    private void runPipeline(DocumentRecord doc, KnowledgeBase kb, ThrowingSupplier<String> parseRaw, String fileName,
                             boolean smartChunk) throws Exception {
        String docId = doc.getId();
        log.info("[鏂囨。涓婁紶] docId={} 娴佹按绾垮紑濮?鏂囦欢={}, smartChunk={}", docId, fileName, smartChunk);

        processTracker.updateStage(docId, DocumentProcessStage.PARSING);
        long tParse = System.nanoTime();
        String raw = parseRaw.get();
        String text = textNormalizer.normalize(raw);
        if (text.isBlank()) {
            throw new IllegalArgumentException("document is empty after normalization");
        }
        log.info("[鏂囨。涓婁紶] docId={} 瑙ｆ瀽涓庤鑼冨寲瀹屾垚 鍘熷瀛楁暟={}, 姝ｆ枃瀛楁暟={}, 鑰楁椂={}ms",
                docId, raw.length(), text.length(), stepMs(tParse));

        processTracker.updateStage(docId, DocumentProcessStage.CHUNKING);
        long tChunk = System.nanoTime();
        var result = hybridChunkingService.chunk(docId, text, fileName, kb.getConfig(), smartChunk);
        doc.setProfile(result.profile());
        doc.setQualityScore(result.qualityReport().qualityScore());
        doc.setAiTriggered(result.aiTriggered());
        doc.setAiTriggerId(result.aiTriggerId());
        doc.setAiFallback(result.aiFallback());
        long hybridSources = result.chunks().stream().filter(c -> "hybrid".equals(c.getSource())).count();
        log.info("[鏂囨。涓婁紶] docId={} 娣峰悎鍒囩墖瀹屾垚 鐢诲儚={}, 瑙勫垯璐ㄩ噺鍒?{}, 鍒囩墖鏁?{}, AI鍒囩墖鏁?{}, "
                        + "AI宸茶Е鍙?{}, 瑙﹀彂ID={}, AI鍥為€€={}, 鑰楁椂={}ms",
                docId, result.profile(), result.qualityReport().qualityScore(), result.chunks().size(), hybridSources,
                result.aiTriggered(), result.aiTriggerId(), result.aiFallback(), stepMs(tChunk));

        processTracker.updateStage(docId, DocumentProcessStage.EMBEDDING);
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
        log.info("[鏂囨。涓婁紶] docId={} 鍚戦噺鍐欏叆瀹屾垚 鍒囩墖鏁?{}, 鑰楁椂={}ms",
                docId, i, stepMs(tEmbed));
        processTracker.updateStage(docId, DocumentProcessStage.SUCCESS);
    }

    private static long stepMs(long stepStartNano) {
        return (System.nanoTime() - stepStartNano) / 1_000_000;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
