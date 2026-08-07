package com.xtsh.ragchunk.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xtsh.ragchunk.vo.chat.ChatResponse;
import com.xtsh.ragchunk.dto.chat.ChatRunStats;
import com.xtsh.ragchunk.config.RagChunkProperties;
import com.xtsh.ragchunk.integration.dashscope.ChatCompletionResult;
import com.xtsh.ragchunk.integration.dashscope.DashScopeHttpClient;
import com.xtsh.ragchunk.dto.knowledge.KnowledgeBase;
import com.xtsh.ragchunk.dto.knowledge.KnowledgeBaseConfig;
import com.xtsh.ragchunk.dto.knowledge.QaConfig;
import com.xtsh.ragchunk.vector.ScoredChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能问答编排：按知识库 {@link QaConfig#scheme()} 选择方案 1 / 2 / 3 / 5，并统计 LLM、检索次数。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ChatOrchestrator {

    private final ChatRetrievalService retrieval;
    private final QueryRewriteService queryRewrite;
    private final AnswerGenerationService answerGeneration;
    private final KbSearchToolExecutor toolExecutor;
    private final DashScopeHttpClient dashScope;
    private final RagChunkProperties properties;


    /**
     * 执行问答主流程。
     *
     * @param schemeOverride 单次请求覆盖方案（可为 null，则用库配置）
     */
    public ChatResponse orchestrate(KnowledgeBase kb, String question, Integer schemeOverride) throws Exception {
        KnowledgeBaseConfig config = kb.getConfig();
        QaConfig qa = config.qa();
        QaScheme scheme = QaScheme.fromCode(schemeOverride, QaScheme.fromCode(qa.scheme(), QaScheme.PIPELINE));
        ChatRunStats stats = new ChatRunStats();
        stats.setScheme(scheme);

        log.info("[智能问答] 开始 kbId={}, scheme={}({}), questionLen={}",
                kb.getId(), scheme.code(), scheme.id(), question.length());

        return switch (scheme) {
            case PIPELINE -> runPipeline(kb.getId(), question, config, qa, stats);
            case COLLABORATIVE_PROGRESSIVE -> runCollaborativeProgressive(kb.getId(), question, config, qa, stats);
            case COLLABORATIVE_ALWAYS -> runCollaborativeAlways(kb.getId(), question, config, qa, stats);
            case AGENT -> runAgent(kb.getId(), question, config, qa, stats);
        };
    }

    /** 方案 1：纯应用 — 原问检索 → 生成。 */
    private ChatResponse runPipeline(String kbId, String question, KnowledgeBaseConfig config,
                                   QaConfig qa, ChatRunStats stats) throws Exception {
        List<ScoredChunk> hits = searchRound(kbId, question, config, stats, false);
        return finish(kbId, question, config, qa, stats, hits);
    }

    /**
     * 方案 2：协作渐进 — 原问检索；不足时 LLM 改写 query 再检索（受 maxSearchRounds / maxLlmCalls 约束）。
     */
    private ChatResponse runCollaborativeProgressive(String kbId, String question, KnowledgeBaseConfig config,
                                                     QaConfig qa, ChatRunStats stats) throws Exception {
        List<ScoredChunk> hits = searchRound(kbId, question, config, stats, false);
        double threshold = config.retrieval().scoreThreshold();

        if (!ChatRetrievalService.isSufficient(hits, threshold)
                && stats.getSearchRounds() < qa.maxSearchRounds()
                && stats.getLlmCalls() < qa.maxLlmCalls()
                && ChatRetrievalService.shouldRewrite(hits, qa)) {

            stats.setRewriteTriggered(true);
            List<String> queries = rewriteRound(question, qa, stats);
            if (!queries.isEmpty()) {
                List<List<ScoredChunk>> rounds = new ArrayList<>();
                rounds.add(hits);
                for (String q : queries) {
                    if (stats.getSearchRounds() >= qa.maxSearchRounds()) {
                        break;
                    }
                    rounds.add(retrieval.search(kbId, q, config, false));
                    stats.incrementSearchRounds();
                }
                hits = retrieval.mergeHits(rounds, config.retrieval().topK());
            }
        }
        return finish(kbId, question, config, qa, stats, hits);
    }

    /** 方案 3：协作全量 — 每条先 LLM 改写再检索（仍受次数上限约束）。 */
    private ChatResponse runCollaborativeAlways(String kbId, String question, KnowledgeBaseConfig config,
                                                QaConfig qa, ChatRunStats stats) throws Exception {
        stats.setRewriteTriggered(true);
        List<String> queries = rewriteRound(question, qa, stats);
        List<ScoredChunk> hits;
        if (queries.isEmpty()) {
            hits = searchRound(kbId, question, config, stats, false);
        } else {
            List<List<ScoredChunk>> rounds = ChatRetrievalService.roundsFromQueries(kbId, queries, config, retrieval, false);
            stats.setSearchRounds(rounds.size());
            hits = retrieval.mergeHits(rounds, config.retrieval().topK());
            log.info("[智能问答] 全量改写检索 合并命中={}, maxScore={}", hits.size(), ChatRetrievalService.maxScore(hits));
        }
        return finish(kbId, question, config, qa, stats, hits);
    }

    /**
     * 方案 5：Agent — LLM 通过 search_kb 工具检索（轮次 ≤ agentMaxIterations，tool 次数受控）。
     */
    private ChatResponse runAgent(String kbId, String question, KnowledgeBaseConfig config,
                                  QaConfig qa, ChatRunStats stats) throws Exception {
        if (!dashScope.isConfigured()) {
            log.warn("[智能问答] Agent 方案需要 LLM，回退为纯应用检索");
            return runPipeline(kbId, question, config, qa, stats);
        }

        String model = properties.getChat().getModel();
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", agentSystemPrompt(qa)));
        messages.add(Map.of("role", "user", "content", question));

        List<ScoredChunk> accumulated = new ArrayList<>();
        String finalAnswer = null;
        int toolCallsThisRun = 0;

        for (int iter = 0; iter < qa.agentMaxIterations(); iter++) {
            if (stats.getLlmCalls() >= qa.maxLlmCalls()) {
                log.info("[智能问答] Agent 已达 maxLlmCalls={}", qa.maxLlmCalls());
                break;
            }
            ChatCompletionResult completion = dashScope.chatWithTools(model, messages, KbSearchToolExecutor.toolDefinition());
            stats.incrementLlmCalls();
            log.info("[智能问答] Agent 轮次={}/{} toolCalls={} hasContent={}",
                    iter + 1, qa.agentMaxIterations(), completion.toolCalls().size(),
                    completion.content() != null && !completion.content().isBlank());

            if (completion.hasToolCalls()) {
                messages.add(assistantToolMessage(completion));
                toolCallsThisRun = 0;
                for (ChatCompletionResult.ToolCall tc : completion.toolCalls()) {
                    if (toolCallsThisRun >= qa.agentMaxToolCallsPerRound()) {
                        break;
                    }
                    if (stats.getSearchRounds() >= qa.maxSearchRounds()) {
                        break;
                    }
                    if (!KbSearchToolExecutor.TOOL_NAME.equals(tc.name())) {
                        continue;
                    }
                    try {
                        List<ScoredChunk> toolHits = toolExecutor.execute(kbId, tc.argumentsJson(), config, qa);
                        stats.incrementSearchRounds();
                        toolCallsThisRun++;
                        accumulated = retrieval.mergeHits(List.of(accumulated, toolHits), config.retrieval().topK());
                        messages.add(Map.of(
                                "role", "tool",
                                "tool_call_id", tc.id(),
                                "content", toolExecutor.formatToolResult(toolHits)));
                    } catch (Exception e) {
                        log.warn("[智能问答] tool search_kb 失败: {}", e.getMessage());
                        messages.add(Map.of(
                                "role", "tool",
                                "tool_call_id", tc.id(),
                                "content", "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}"));
                    }
                }
                continue;
            }

            if (completion.content() != null && !completion.content().isBlank()) {
                finalAnswer = completion.content();
                break;
            }
        }

        if (finalAnswer != null && !accumulated.isEmpty()) {
            return buildResponse(finalAnswer, accumulated, stats);
        }
        if (!accumulated.isEmpty() && stats.getLlmCalls() < qa.maxLlmCalls()) {
            stats.incrementLlmCalls();
            String answer = answerGeneration.generate(question, accumulated);
            return buildResponse(answer, accumulated, stats);
        }
        if (!accumulated.isEmpty()) {
            return buildResponse(
                    answerGeneration.generate(question, accumulated), accumulated, stats);
        }
        return noHitResponse(stats);
    }

    private List<ScoredChunk> searchRound(String kbId, String query, KnowledgeBaseConfig config,
                                          ChatRunStats stats, boolean relax) throws Exception {
        List<ScoredChunk> hits = retrieval.search(kbId, query, config, relax);
        stats.incrementSearchRounds();
        return hits;
    }

    private List<String> rewriteRound(String question, QaConfig qa, ChatRunStats stats) {
        List<String> queries = queryRewrite.rewriteToSearchQueries(question, qa);
        if (!queries.isEmpty()) {
            stats.incrementLlmCalls();
        }
        return queries;
    }

    private ChatResponse finish(String kbId, String question, KnowledgeBaseConfig config, QaConfig qa,
                                ChatRunStats stats, List<ScoredChunk> hits) throws Exception {
        if (hits.isEmpty()) {
            return noHitResponse(stats);
        }
        String answer;
        if (dashScope.isConfigured() && stats.getLlmCalls() < qa.maxLlmCalls()) {
            stats.incrementLlmCalls();
            answer = answerGeneration.generate(question, hits);
        } else {
            answer = answerGeneration.generate(question, hits);
        }
        return buildResponse(answer, hits, stats);
    }

    private ChatResponse noHitResponse(ChatRunStats stats) {
        log.info("[智能问答] 无命中 scheme={}, searchRounds={}, llmCalls={}",
                stats.getScheme().id(), stats.getSearchRounds(), stats.getLlmCalls());
        var resp = new ChatResponse();
        resp.setAnswer(AnswerGenerationService.NO_HIT_MESSAGE);
        resp.setCitations(List.of());
        resp.setMeta(ChatResponse.Meta.from(stats));
        return resp;
    }

    private ChatResponse buildResponse(String answer, List<ScoredChunk> hits, ChatRunStats stats) {
        stats.setHitCount(hits.size());
        stats.setMaxScore(ChatRetrievalService.maxScore(hits));
        log.info("[智能问答] 完成 scheme={}, hits={}, maxScore={}, llmCalls={}, searchRounds={}, rewrite={}",
                stats.getScheme().id(), stats.getHitCount(), stats.getMaxScore(),
                stats.getLlmCalls(), stats.getSearchRounds(), stats.isRewriteTriggered());
        var resp = new ChatResponse();
        resp.setAnswer(answer);
        resp.setCitations(AnswerGenerationService.toCitations(hits));
        resp.setMeta(ChatResponse.Meta.from(stats));
        return resp;
    }

    private static String agentSystemPrompt(QaConfig qa) {
        return """
                你是企业知识库 Agent。需要查资料时请调用 search_kb，传入 query（必填），可选 topK、scoreThreshold、relaxThreshold。
                无足够资料时请明确说明，不要编造。每个用户问题最多调用 search_kb %d 次（每轮）。
                找到资料后请根据工具返回的片段用中文回答用户。
                """.formatted(qa.agentMaxToolCallsPerRound());
    }

    private static Map<String, Object> assistantToolMessage(ChatCompletionResult completion) {
        var msg = new LinkedHashMap<String, Object>();
        msg.put("role", "assistant");
        msg.put("content", completion.content() != null ? completion.content() : "");
        List<Map<String, Object>> tcs = new ArrayList<>();
        for (ChatCompletionResult.ToolCall tc : completion.toolCalls()) {
            var fn = new LinkedHashMap<String, Object>();
            fn.put("name", tc.name());
            fn.put("arguments", tc.argumentsJson());
            var toolCall = new LinkedHashMap<String, Object>();
            toolCall.put("id", tc.id());
            toolCall.put("type", "function");
            toolCall.put("function", fn);
            tcs.add(toolCall);
        }
        msg.put("tool_calls", tcs);
        return msg;
    }
}
