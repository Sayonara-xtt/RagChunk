package com.xtsh.ragchunk.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.embedding.EmbeddingService;
import com.xtsh.ragchunk.dto.knowledge.KnowledgeBaseConfig;
import com.xtsh.ragchunk.dto.knowledge.QaConfig;
import com.xtsh.ragchunk.vector.ScoredChunk;
import com.xtsh.ragchunk.vector.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能问答检索层：向量化 + pgvector 检索 + 多 query 合并（纯应用执行，不经 LLM 直连库）。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ChatRetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;


    /**
     * 使用用户或改写后的自然语言 query 检索知识库。
     */
    public List<ScoredChunk> search(String kbId, String query, KnowledgeBaseConfig config, boolean relaxThreshold)
            throws Exception {
        var retrieval = config.retrieval();
        double threshold = retrieval.scoreThreshold();
        if (relaxThreshold) {
            threshold = Math.max(0.2, threshold * 0.8);
        }
        int topK = retrieval.topK();
        float[] vec = embeddingService.embed(query, config.embedding());
        List<ScoredChunk> hits = vectorStore.search(kbId, vec, topK, threshold);
        log.info("[智能问答] 向量检索 kbId={}, queryLen={}, topK={}, threshold={}, relax={}, 命中={}, maxScore={}",
                kbId, query != null ? query.length() : 0, topK, threshold, relaxThreshold,
                hits.size(), maxScore(hits));
        return hits;
    }

    /**
     * Agent / 工具检索：在知识库默认检索参数基础上应用 LLM 传入的受控覆盖项。
     */
    public List<ScoredChunk> searchWithParams(String kbId, com.xtsh.ragchunk.dto.chat.KbSearchParams params,
                                              KnowledgeBaseConfig config, QaConfig qa) throws Exception {
        var retrieval = config.retrieval();
        int topK = params.topK() != null ? Math.min(params.topK(), 10) : retrieval.topK();
        double threshold = params.scoreThreshold() != null
                ? clamp(params.scoreThreshold(), 0, 1)
                : retrieval.scoreThreshold();
        if (params.relaxThreshold() && qa.agentAllowRelaxThreshold()) {
            threshold = Math.max(0.2, threshold * 0.8);
        }
        float[] vec = embeddingService.embed(params.query(), config.embedding());
        List<ScoredChunk> hits = vectorStore.search(kbId, vec, topK, threshold);
        log.info("[智能问答] tool检索 kbId={}, queryLen={}, topK={}, threshold={}, 命中={}, maxScore={}",
                kbId, params.query().length(), topK, threshold, hits.size(), maxScore(hits));
        return hits;
    }

    /**
     * 多路检索结果按 chunkId 去重，保留最高分，截断为 topK。
     */
    public List<ScoredChunk> mergeHits(List<List<ScoredChunk>> rounds, int topK) {
        Map<String, ScoredChunk> best = new LinkedHashMap<>();
        for (List<ScoredChunk> round : rounds) {
            for (ScoredChunk h : round) {
                String id = h.record().chunkId();
                best.merge(id, h, (a, b) -> a.score() >= b.score() ? a : b);
            }
        }
        return best.values().stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    public static double maxScore(List<ScoredChunk> hits) {
        return hits.stream().mapToDouble(ScoredChunk::score).max().orElse(0);
    }

    /**
     * 是否达到进入生成的召回质量（有条目且最高分不低于库阈值）。
     */

    public static boolean isSufficient(List<ScoredChunk> hits, double threshold) {
        return !hits.isEmpty() && maxScore(hits) >= threshold;
    }

    /**
     * 协作渐进：是否应触发改写（无命中或最高分低于 rewriteMinScore）。
     */
    public static boolean shouldRewrite(List<ScoredChunk> hits, QaConfig qa) {
        if (hits.isEmpty()) {
            return true;
        }
        return maxScore(hits) < qa.rewriteMinScore();
    }

    public static List<List<ScoredChunk>> roundsFromQueries(String kbId, List<String> queries,
                                                            KnowledgeBaseConfig config,
                                                            ChatRetrievalService retrieval,
                                                            boolean relax) throws Exception {
        List<List<ScoredChunk>> rounds = new ArrayList<>();
        for (String q : queries) {
            if (q != null && !q.isBlank()) {
                rounds.add(retrieval.search(kbId, q.trim(), config, relax));
            }
        }
        return rounds;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
