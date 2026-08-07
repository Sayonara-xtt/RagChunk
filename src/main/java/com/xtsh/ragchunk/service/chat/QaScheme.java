package com.xtsh.ragchunk.service.chat;

/**
 * 智能问答编排方案（创建知识库或单次请求可指定）。
 * <ul>
 *   <li>{@link #PIPELINE} — 纯应用：原问检索后直接生成</li>
 *   <li>{@link #COLLABORATIVE_PROGRESSIVE} — 协作渐进：原问检索，不足时 LLM 改写 query 再检索</li>
 *   <li>{@link #COLLABORATIVE_ALWAYS} — 协作全量：每条先 LLM 改写再检索</li>
 *   <li>{@link #AGENT} — Agent：LLM 通过 search_kb 工具检索（轮次受 maxAgentIterations 限制）</li>
 * </ul>
 */
public enum QaScheme {

    /** 方案 1：纯应用 RAG */
    PIPELINE(1, "pipeline"),
    /** 方案 2：协作·渐进 */
    COLLABORATIVE_PROGRESSIVE(2, "collaborative_progressive"),
    /** 方案 3：协作·全量改写 */
    COLLABORATIVE_ALWAYS(3, "collaborative_always"),
    /** 方案 5：Agent（多轮 tool，次数可配置） */
    AGENT(5, "agent");

    private final int code;
    private final String id;

    QaScheme(int code, String id) {
        this.code = code;
        this.id = id;
    }

    public int code() {
        return code;
    }

    public String id() {
        return id;
    }

    public static QaScheme fromCode(int code) {
        for (QaScheme s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("qa.scheme must be 1, 2, 3, or 5");
    }

    public static QaScheme fromCode(Integer code, QaScheme defaultScheme) {
        if (code == null) {
            return defaultScheme;
        }
        return fromCode(code.intValue());
    }
}
