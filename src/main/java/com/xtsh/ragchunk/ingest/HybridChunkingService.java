package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.HybridChunkResult;
import com.xtsh.ragchunk.chunk.model.TextChunk;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 混合切片：规则切片必走；千问语义重切按 {@link AiChunkTrigger} 决策（T0/T2/T4/T8 等）。
 */
@Service
public class HybridChunkingService {

    private static final Logger log = LoggerFactory.getLogger(HybridChunkingService.class);
    private static final String TASK = "SEMANTIC_RESPLIT";

    private final RuleChunker ruleChunker;
    private final ChunkQualityEvaluator qualityEvaluator;
    private final AiChunkTrigger aiChunkTrigger;
    private final SemanticResplitService semanticResplitService;
    private final ChunkValidationService validationService;

    public HybridChunkingService(RuleChunker ruleChunker, ChunkQualityEvaluator qualityEvaluator,
                                 AiChunkTrigger aiChunkTrigger, SemanticResplitService semanticResplitService,
                                 ChunkValidationService validationService) {
        this.ruleChunker = ruleChunker;
        this.qualityEvaluator = qualityEvaluator;
        this.aiChunkTrigger = aiChunkTrigger;
        this.semanticResplitService = semanticResplitService;
        this.validationService = validationService;
    }

    /**
     * 混合切片主流程：规则切片 → 质量评估 → 按需 AI 重切 → 校验 → 回退。
     *
     * @param smartChunk 上传参数，见 {@link AiChunkTrigger#decide}
     */
    public HybridChunkResult chunk(String docId, String text, String fileName, KnowledgeBaseConfig config,
                                   boolean smartChunk) {
        String profile = new ChunkProfileDetector().detect(fileName);
        List<TextChunk> ruleChunks = ruleChunker.chunk(text, profile, config.rule());
        var report = qualityEvaluator.evaluate(ruleChunks, text.length(), config.rule());
        log.info("[文档上传] docId={} 规则切片完成 画像={}, 规则切片数={}, 质量分={}",
                docId, profile, ruleChunks.size(), report.qualityScore());

        var decision = aiChunkTrigger.decide(config, report, smartChunk);

        if (!decision.shouldRunAi()) {
            log.info("[文档上传] docId={} 未触发AI，仅使用规则切片", docId);
            return new HybridChunkResult(ruleChunks, profile, report, false, null, null, false);
        }

        log.info("[文档上传] AI语义重切 开始 docId={}, 触发ID={}, 任务={}, 正文字数={}, smartChunk={}",
                docId, decision.triggerId(), TASK, text.length(), smartChunk);
        try {
            List<TextChunk> aiChunks = semanticResplitService.resplit(docId, text, fileName, profile, config);
            var validation = validationService.validateDetailed(aiChunks, text, config.rule(), ruleChunks.size());
            if (validation.passed()) {
                var aiReport = qualityEvaluator.evaluate(aiChunks, text.length(), config.rule());
                log.info("[文档上传] docId={} AI切片采纳 触发ID={}, AI切片数={}, 质量分={}, V2覆盖率={}%, 切片预览={}",
                        docId, decision.triggerId(), aiChunks.size(), aiReport.qualityScore(),
                        String.format("%.1f", validation.coverageRatio() * 100),
                        IngestLogSupport.previewChunks(aiChunks, 3));
                return new HybridChunkResult(aiChunks, profile, aiReport, true, decision.triggerId(), TASK, false);
            }
            log.warn("[文档上传] docId={} AI切片校验未通过 触发ID={}, 原因={}, 明细={}, "
                            + "AI段数={}, 规则段数={}, 回退规则切片",
                    docId, decision.triggerId(), validation.code(), validation.detail(),
                    validation.chunkCount(), ruleChunks.size());
        } catch (Exception e) {
            log.warn("[文档上传] docId={} AI切片异常 触发ID={}, 回退规则切片: {}",
                    docId, decision.triggerId(), rootCauseMessage(e), e);
        }
        return new HybridChunkResult(ruleChunks, profile, report, true, decision.triggerId(), TASK, true);
    }

    private static String rootCauseMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t.getMessage() != null && !t.getMessage().isBlank()) {
            return t.getMessage();
        }
        return t.getClass().getSimpleName();
    }
}
