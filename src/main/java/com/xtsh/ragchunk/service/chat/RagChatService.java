package com.xtsh.ragchunk.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.vo.chat.ChatResponse;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.exception.BadRequestException;
import org.springframework.stereotype.Service;

/**
 * 智能问答入口：校验参数后委托 {@link ChatOrchestrator} 按方案执行。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class RagChatService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatOrchestrator orchestrator;

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
