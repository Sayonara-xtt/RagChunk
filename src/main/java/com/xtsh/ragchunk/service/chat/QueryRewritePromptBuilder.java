package com.xtsh.ragchunk.service.chat;

/**
 * 协作方案中「LLM 辅助检索」的 Prompt 模板（仅产出检索短句，禁止直接回答问题）。
 */
final class QueryRewritePromptBuilder {

    private QueryRewritePromptBuilder() {}

    static String system(int maxQueries) {
        return """
                你是企业知识库检索查询助手。根据用户问题生成适合向量检索的短句。
                只输出 JSON：{"search_queries":["..."]}，不要 markdown 围栏，不要解释。
                不要回答用户问题，不要编造文档中不存在的专有名词。
                检索短句最多 %d 条，每条不超过 120 字，使用用户问题中的关键词并可适度泛化。
                """.formatted(maxQueries);
    }

    static String user(String question) {
        return "用户问题：\n" + question;
    }
}
