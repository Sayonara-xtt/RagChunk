package com.xtsh.ragchunk.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.vo.chat.ChatResponse;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import com.xtsh.ragchunk.vector.ScoredChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据检索命中片段生成回答（或本地无 LLM 时返回检索摘要）。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class AnswerGenerationService {

    public static final String NO_HIT_MESSAGE =
            "知识库中未找到与您问题相关的内容，请尝试换个问法或补充文档。";

    private final DashScopeHttpClient dashScope;
    private final RagChunkProperties properties;


    /**
     * @param originalQuestion 用户原问（生成阶段必须用原问，不用改写句）
     * @return answer 文本；调用方负责 llmCalls 计数
     */
    public String generate(String originalQuestion, List<ScoredChunk> hits) throws Exception {
        return generate(originalQuestion, hits, properties.getChat().getModel());
    }

    public String generate(
            String originalQuestion, List<ScoredChunk> hits, String chatModel) throws Exception {
        String context = hits.stream()
                .map(h -> "---\n" + h.record().text())
                .collect(Collectors.joining("\n"));
        if (!dashScope.isConfigured()) {
            log.info("[智能问答] 无 LLM，返回检索摘要 hitCount={}", hits.size());
            return "【本地模式-无 LLM】检索到 " + hits.size() + " 条相关内容：\n\n"
                    + hits.stream().map(h -> h.record().text()).collect(Collectors.joining("\n\n---\n\n"));
        }
        String system = "你是企业知识库助手。仅根据下列「参考资料」回答问题；资料不足时请明确说明，不要编造。";
        String user = "参考资料：\n" + context + "\n\n用户问题：" + originalQuestion;
        String answer = dashScope.chat(chatModel, system, user);
        log.info("[智能问答] 答案生成完成 contextChunks={}, answerLen={}", hits.size(), answer.length());
        return answer;
    }

    public static List<ChatResponse.Citation> toCitations(List<ScoredChunk> hits) {
        return hits.stream()
                .map(h -> new ChatResponse.Citation(
                        h.record().chunkId(),
                        h.record().docId(),
                        h.record().chunkIndex(),
                        h.score(),
                        excerpt(h.record().text(), 200)))
                .collect(Collectors.toList());
    }

    private static String excerpt(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
