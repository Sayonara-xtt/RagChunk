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
    }
}
