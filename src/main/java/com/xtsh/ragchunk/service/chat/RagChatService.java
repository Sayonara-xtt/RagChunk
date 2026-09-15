package com.xtsh.ragchunk.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.vo.chat.ChatResponse;
import com.xtsh.ragchunk.service.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.exception.BadRequestException;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.service.chat.trace.ChatExecutionContext;
import com.xtsh.ragchunk.service.chat.trace.ChatExecutionOptions;
import com.xtsh.ragchunk.service.chat.trace.ChatExecutionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 智能问答入口：校验参数后委托 {@link ChatOrchestrator} 按方案执行。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class RagChatService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatOrchestrator orchestrator;
    private final RagChunkProperties properties;
    private final ObjectProvider<ChatExecutionService> executionServiceProvider;

    public ChatResponse chat(String kbId, String question, Integer qaSchemeOverride) throws Exception {
        return executeChat(null, kbId, question, qaSchemeOverride);
    }

    public ChatResponse chat(String runId, String kbId, String question, Integer qaSchemeOverride) throws Exception {
        ChatExecutionService executionService = executionServiceProvider.getIfAvailable();
        if (executionService == null) {
            return executeChat(null, kbId, question, qaSchemeOverride);
        }
        ChatExecutionContext context = new ChatExecutionContext(
                runId,
                kbId,
                question == null ? "" : question,
                Instant.now(),
                initialOptions(qaSchemeOverride));
        return executionService.execute(context, () -> executeChat(context, kbId, question, qaSchemeOverride));
    }

    public ChatResponse chat(
            String runId, String kbId, String question, ChatExecutionOptions evaluationOptions) throws Exception {
        ChatExecutionService executionService = executionServiceProvider.getIfAvailable();
        ChatExecutionContext context = new ChatExecutionContext(
                runId, kbId, question == null ? "" : question, Instant.now(), evaluationOptions);
        if (executionService == null) {
            return executeChat(context, kbId, question, evaluationOptions.qaScheme(), evaluationOptions);
        }
        return executionService.execute(context,
                () -> executeChat(context, kbId, question, evaluationOptions.qaScheme(), evaluationOptions));
    }

    private ChatResponse executeChat(
            ChatExecutionContext context, String kbId, String question, Integer qaSchemeOverride) throws Exception {
        return executeChat(context, kbId, question, qaSchemeOverride, null);
    }

    private ChatResponse executeChat(
            ChatExecutionContext context,
            String kbId,
            String question,
            Integer qaSchemeOverride,
            ChatExecutionOptions evaluationOptions) throws Exception {
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
        if (evaluationOptions != null) {
            var original = kb.getConfig();
            var effective = new com.xtsh.ragchunk.dto.knowledge.KnowledgeBaseConfig(
                    original.chunking(), original.rule(), original.quality(), original.ai(),
                    original.embedding(),
                    new com.xtsh.ragchunk.dto.knowledge.KnowledgeBaseConfig.RetrievalConfig(
                            evaluationOptions.topK(), evaluationOptions.scoreThreshold()),
                    original.qa());
            var copy = new com.xtsh.ragchunk.dto.knowledge.KnowledgeBase();
            copy.setId(kb.getId());
            copy.setName(kb.getName());
            copy.setStatus(kb.getStatus());
            copy.setConfig(effective);
            copy.setCreatedAt(kb.getCreatedAt());
            kb = copy;
        }
        if (context != null) {
            context.associateKnowledgeBase(kb.getId());
            context.normalizeQuestion(question.trim());
        }
        log.info("[智能问答] 接收请求 kbId={}, qaSchemeOverride={}, 库配置 scheme={}",
                kbId, qaSchemeOverride, kb.getConfig().qa().scheme());
        return context == null
                ? orchestrator.orchestrate(kb, question.trim(), qaSchemeOverride)
                : orchestrator.orchestrate(context, kb, question.trim(), qaSchemeOverride);
    }

    private ChatExecutionOptions initialOptions(Integer qaSchemeOverride) {
        int code = qaSchemeOverride != null ? qaSchemeOverride : properties.getQa().getScheme();
        String schemeName;
        try {
            schemeName = QaScheme.fromCode(code).id();
        } catch (IllegalArgumentException ignored) {
            schemeName = "invalid";
        }
        return new ChatExecutionOptions(
                code,
                schemeName,
                properties.getDashscope().getProvider(),
                properties.getChat().getModel(),
                properties.getEmbedding().getModel(),
                "rag-answer-v1",
                "",
                properties.getRetrieval().getTopK(),
                properties.getRetrieval().getScoreThreshold());
    }
}
