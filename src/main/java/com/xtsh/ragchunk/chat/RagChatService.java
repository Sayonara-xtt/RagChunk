package com.xtsh.ragchunk.chat;

import com.xtsh.ragchunk.chat.dto.ChatResponse;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.embedding.EmbeddingService;
import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import com.xtsh.ragchunk.knowledge.KnowledgeBaseService;
import com.xtsh.ragchunk.vector.ScoredChunk;
import com.xtsh.ragchunk.vector.VectorStore;
import com.xtsh.ragchunk.web.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagChatService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final DashScopeHttpClient dashScope;
    private final RagChunkProperties properties;

    public RagChatService(KnowledgeBaseService knowledgeBaseService, EmbeddingService embeddingService,
                          VectorStore vectorStore, DashScopeHttpClient dashScope,
                          RagChunkProperties properties) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.dashScope = dashScope;
        this.properties = properties;
    }

    public ChatResponse chat(String kbId, String question) throws Exception {
        if (question == null || question.isBlank()) {
            throw new BadRequestException("question is required");
        }
        var kb = knowledgeBaseService.require(kbId);
        var retrieval = kb.getConfig().retrieval();
        float[] qVec = embeddingService.embed(question, kb.getConfig().embedding());
        List<ScoredChunk> hits = vectorStore.search(kbId, qVec, retrieval.topK(), retrieval.scoreThreshold());

        var citations = hits.stream()
                .map(h -> new ChatResponse.Citation(
                        h.record().chunkId(),
                        h.record().docId(),
                        h.record().chunkIndex(),
                        h.score(),
                        excerpt(h.record().text(), 200)))
                .collect(Collectors.toList());

        String context = hits.stream()
                .map(h -> "---\n" + h.record().text())
                .collect(Collectors.joining("\n"));

        String answer;
        if (hits.isEmpty()) {
            answer = "知识库中未找到与您问题相关的内容，请尝试换个问法或补充文档。";
        } else if (dashScope.isConfigured()) {
            String system = "你是企业知识库助手。仅根据下列「参考资料」回答问题；资料不足时请明确说明，不要编造。";
            String user = "参考资料：\n" + context + "\n\n用户问题：" + question;
            answer = dashScope.chat(properties.getChat().getModel(), system, user);
        } else {
            answer = "【本地模式-无 LLM】检索到 " + hits.size() + " 条相关内容：\n\n"
                    + hits.stream().map(h -> h.record().text()).collect(Collectors.joining("\n\n---\n\n"));
        }

        var resp = new ChatResponse();
        resp.setAnswer(answer);
        resp.setCitations(citations);
        return resp;
    }

    private static String excerpt(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
