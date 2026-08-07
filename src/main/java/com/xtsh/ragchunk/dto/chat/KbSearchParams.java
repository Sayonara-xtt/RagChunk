package com.xtsh.ragchunk.dto.chat;

/**
 * 向量检索参数（应用校验后执行；Agent tool search_kb 入参映射到此）。
 */
public record KbSearchParams(
        String query,
        Integer topK,
        Double scoreThreshold,
        boolean relaxThreshold
) {
}
