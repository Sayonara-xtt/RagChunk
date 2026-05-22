package com.xtsh.ragchunk.knowledge;

import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.knowledge.dto.CreateKnowledgeBaseRequest;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import com.xtsh.ragchunk.knowledge.model.QaConfig;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseConfigMerger {

    public KnowledgeBaseConfig merge(RagChunkProperties d, CreateKnowledgeBaseRequest r) {
        var cr = r.getChunking();
        var rr = r.getRule();
        var qual = r.getQuality();
        var ar = r.getAi();
        var er = r.getEmbedding();
        var tr = r.getRetrieval();
        var qar = r.getQa();
        var dq = d.getQa();
        return new KnowledgeBaseConfig(
                new KnowledgeBaseConfig.ChunkingConfig(
                        or(cr != null ? cr.getMode() : null, d.getChunking().getMode()),
                        or(cr != null ? cr.getAiMode() : null, d.getChunking().getAiMode())),
                new KnowledgeBaseConfig.RuleConfig(
                        or(rr != null ? rr.getMaxChars() : null, d.getRule().getMaxChars()),
                        or(rr != null ? rr.getMinChars() : null, d.getRule().getMinChars()),
                        or(rr != null ? rr.getOverlap() : null, d.getRule().getOverlap()),
                        or(rr != null ? rr.getPlainSeparators() : null, d.getRule().getPlainSeparators()),
                        or(rr != null ? rr.getMarkdownSeparators() : null, d.getRule().getMarkdownSeparators()),
                        or(rr != null ? rr.getSentenceBoundaryFallback() : null, d.getRule().isSentenceBoundaryFallback())),
                new KnowledgeBaseConfig.QualityConfig(or(qual != null ? qual.getScoreThreshold() : null, d.getQuality().getScoreThreshold())),
                new KnowledgeBaseConfig.AiConfig(
                        or(ar != null ? ar.getChunkModel() : null, d.getAi().getChunkModel()),
                        or(ar != null ? ar.getMaxCallsPerDoc() : null, d.getAi().getMaxCallsPerDoc()),
                        or(ar != null ? ar.getMaxInputTokens() : null, d.getAi().getMaxInputTokens()),
                        or(ar != null ? ar.getRetryOnParseError() : null, d.getAi().getRetryOnParseError())),
                new KnowledgeBaseConfig.EmbeddingConfig(or(er != null ? er.getModel() : null, d.getEmbedding().getModel())),
                new KnowledgeBaseConfig.RetrievalConfig(
                        or(tr != null ? tr.getTopK() : null, d.getRetrieval().getTopK()),
                        or(tr != null ? tr.getScoreThreshold() : null, d.getRetrieval().getScoreThreshold())),
                new QaConfig(
                        or(qar != null ? qar.getScheme() : null, dq.getScheme()),
                        or(qar != null ? qar.getRewriteMinScore() : null, dq.getRewriteMinScore()),
                        or(qar != null ? qar.getMaxRewriteQueries() : null, dq.getMaxRewriteQueries()),
                        or(qar != null ? qar.getMaxLlmCalls() : null, dq.getMaxLlmCalls()),
                        or(qar != null ? qar.getMaxSearchRounds() : null, dq.getMaxSearchRounds()),
                        or(qar != null ? qar.getAgentMaxIterations() : null, dq.getAgentMaxIterations()),
                        or(qar != null ? qar.getAgentMaxToolCallsPerRound() : null, dq.getAgentMaxToolCallsPerRound()),
                        or(qar != null ? qar.getAgentAllowRelaxThreshold() : null, dq.isAgentAllowRelaxThreshold()))
        );
    }

    private static Boolean or(Boolean v, boolean def) {
        return v != null ? v : def;
    }

    private static <T> T or(T v, T def) { return v != null ? v : def; }
}
