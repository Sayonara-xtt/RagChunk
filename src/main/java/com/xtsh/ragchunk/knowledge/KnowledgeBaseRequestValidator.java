package com.xtsh.ragchunk.knowledge;

import com.xtsh.ragchunk.knowledge.dto.CreateKnowledgeBaseRequest;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import com.xtsh.ragchunk.web.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class KnowledgeBaseRequestValidator {

    private static final Set<String> AI_MODES = Set.of("never", "auto", "always");
    private static final Set<Integer> QA_SCHEMES = Set.of(1, 2, 3, 5);

    public void validateCreate(CreateKnowledgeBaseRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BadRequestException("name is required");
        }
        if (request.getName().length() > 64) {
            throw new BadRequestException("name must be at most 64 characters");
        }
        var c = request.getChunking();
        if (c != null && c.getAiMode() != null && !AI_MODES.contains(c.getAiMode())) {
            throw new BadRequestException("chunking.aiMode must be never, auto, or always");
        }
    }

    public void validateConfig(KnowledgeBaseConfig config) {
        var rule = config.rule();
        if (rule.maxChars() < rule.minChars()) {
            throw new BadRequestException("rule.maxChars must be >= rule.minChars");
        }
        var r = config.retrieval();
        if (r.topK() < 1 || r.topK() > 20) {
            throw new BadRequestException("retrieval.topK must be between 1 and 20");
        }
        var qa = config.qa();
        if (qa != null) {
            validateQa(qa);
        }
    }

    public void validateQa(com.xtsh.ragchunk.knowledge.model.QaConfig qa) {
        if (!QA_SCHEMES.contains(qa.scheme())) {
            throw new BadRequestException("qa.scheme must be 1, 2, 3, or 5");
        }
        if (qa.rewriteMinScore() < 0 || qa.rewriteMinScore() > 1) {
            throw new BadRequestException("qa.rewriteMinScore must be between 0 and 1");
        }
        if (qa.maxRewriteQueries() < 1 || qa.maxRewriteQueries() > 5) {
            throw new BadRequestException("qa.maxRewriteQueries must be between 1 and 5");
        }
        if (qa.maxLlmCalls() < 1 || qa.maxLlmCalls() > 5) {
            throw new BadRequestException("qa.maxLlmCalls must be between 1 and 5");
        }
        if (qa.maxSearchRounds() < 1 || qa.maxSearchRounds() > 5) {
            throw new BadRequestException("qa.maxSearchRounds must be between 1 and 5");
        }
        if (qa.agentMaxIterations() < 1 || qa.agentMaxIterations() > 10) {
            throw new BadRequestException("qa.agentMaxIterations must be between 1 and 10");
        }
        if (qa.agentMaxToolCallsPerRound() < 1 || qa.agentMaxToolCallsPerRound() > 3) {
            throw new BadRequestException("qa.agentMaxToolCallsPerRound must be between 1 and 3");
        }
    }
}
