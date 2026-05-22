package com.xtsh.ragchunk.chat;

import com.xtsh.ragchunk.chat.dto.ChatResponse;
import com.xtsh.ragchunk.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.web.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 智能问答入口：校验参数后委托 {@link ChatOrchestrator} 按方案执行。
 */
@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatOrchestrator orchestrator;

    public RagChatService(KnowledgeBaseService knowledgeBaseService, ChatOrchestrator orchestrator) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.orchestrator = orchestrator;
    }

    public ChatResponse chat(String kbId, String question, Integer qaSchemeOverride) throws Exception {
        if (question == null || question.isBlank()) {
            throw new BadRequestException("question is required");
        }
        if (qaSchemeOverride != null) {
            try {
                QaScheme.fromCode(qaSchemeOverride);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage());
            }
        }
        var kb = knowledgeBaseService.require(kbId);
        log.info("[智能问答] 接收请求 kbId={}, qaSchemeOverride={}, 库配置 scheme={}",
                kbId, qaSchemeOverride, kb.getConfig().qa().scheme());
        return orchestrator.orchestrate(kb, question.trim(), qaSchemeOverride);
    }
}
