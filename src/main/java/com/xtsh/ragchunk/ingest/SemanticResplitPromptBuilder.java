package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;

/**
 * 构建 {@link SemanticResplitService} 的 SYSTEM / USER Prompt，与 {@link ChunkValidationService} V1/V2 对齐。
 * <p>切分判断优先级（写入 Prompt）：① 结构边界 → ② 语义完整 → ③ 固定规则（字数、段数、句界等）。
 */
final class SemanticResplitPromptBuilder {

    private static final double MIN_COVERAGE_PERCENT = 95.0;
    private static final int TARGET_CHARS_PER_CHUNK = 400;

    private SemanticResplitPromptBuilder() {}

    static String buildSystem(KnowledgeBaseConfig.RuleConfig rule) {
        int min = rule.minChars();
        int max = rule.maxChars();
        int maxAllowed = (int) (max * 1.1);
        int idealLow = Math.min(200, Math.max(min, 80));
        int idealHigh = Math.min(max, 800);
        return """
                你是文档切片程序，不是聊天助手。
                任务：在【不删除、不改写、不概括】原文的前提下切分为若干段，供检索问答使用。

                【判断优先级 — 必须按顺序执行，不可颠倒】
                当规则冲突时，一律优先服从序号更小的层级。

                ▶ 第一优先级：结构边界（先决定「在哪切」）
                - 识别标题与章节：Markdown 的 # / ## / ###；或「第一章」「一、」「（一）」「1.」「第×条」等。
                - 默认在同级章节之间切开；不要把两个同级大节的主内容合并成一段。
                - 每个切片开头尽量保留其所属标题行。
                - 列表、表格、代码块作为结构单元：同一编号列表不拆散；表格不拆散单行；代码块/公式块单独成段。
                - 无明确结构时，再在空行、条款边界处寻找切分点。
                - 示例：全文约 1200 字且同属一小节 → 可 1 段；约 1800 字且中间有章节界 → 优先 2 段（如 1200+600 或按边界不等分），不要机械均分。

                ▶ 第二优先级：语义完整（再决定「切开后每段是否自洽」）
                - 每段应能独立回答「这一段在讲什么」；不在句子或词语中间切断。
                - 指代词（「上述」「本条」「如下表」）与其指代对象在同一段。
                - 「综上所述」「因此」等总结句跟随其归纳的前文，不单独成无意义的极短段。
                - 同一小节、同一条款下的内容优先在同一段；仅当超过字数上限时，再在段内按句号二次切分。

                ▶ 第三优先级：固定规则（最后才用字数、段数等硬性约束微调）
                - 每段 text 字符数必须在 %d～%d 之间（系统硬性校验）；理想 %d～%d 字。
                - 低于 %d 字的碎片必须与相邻段合并；超过 %d 字必须在【已满足第一优先级的边界】处再切，禁止为凑字数在句中硬切。
                - 切忌整篇只 1 段（除非全文本身不足一节）；切忌无结构依据的碎段。
                - 合并各段后须覆盖至少 %.0f%% 原文（去空白）；不得编造、删改、概括。

                【输出格式 — 与切分优先级无关，但必须遵守】
                - 只输出一个 JSON：{"chunks":[{"text":"..."}]}
                - 不要 markdown 围栏、不要解释；仅 text 字段，不要 reason/index/title。
                - 不要用「…」「略」「同上」代替原文。
                """.formatted(
                min, maxAllowed, idealLow, idealHigh,
                min, maxAllowed,
                MIN_COVERAGE_PERCENT);
    }

    static String buildUser(String content, String fileName, String profile,
                            KnowledgeBaseConfig.RuleConfig rule, int originalCharCount, boolean truncated) {
        ChunkCountHint countHint = suggestChunkCount(originalCharCount, rule.maxChars());
        String profileBlock = profileHints(profile);
        String truncateNote = truncated
                ? "（注意：下文为截断后的原文前缀，仅对可见部分切片，仍须完整保留可见内容）\n"
                : "";
        return """
                请对下列原文做语义切片。只输出 JSON（chunks 数组），不要其它内容。

                【执行顺序提醒】先找结构边界 → 再保证每段语义完整 → 最后用字数上下限微调；不要为均分字数而切断章节。

                【文档信息】
                - 文件名：%s
                - 画像：%s
                - 原文字符数（约）：%d
                - 段数参考（服从结构边界，可浮动）：约 %d 段，常见范围 %d～%d 段
                - 字数参考（第三优先级）：每段 %d～%d 字

                %s
                %s---
                %s
                ---
                """.formatted(
                fileName != null ? fileName : "unknown",
                profile != null ? profile : ChunkProfileDetector.PLAIN,
                originalCharCount,
                countHint.target(), countHint.min(), countHint.max(),
                rule.minChars(), (int) (rule.maxChars() * 1.1),
                profileBlock,
                truncateNote,
                content);
    }

    static String buildRetryHint(KnowledgeBaseConfig.RuleConfig rule) {
        int maxAllowed = (int) (rule.maxChars() * 1.1);
        return """

                【重试 — 上次输出未通过系统校验】
                仍按优先级：结构边界 → 语义完整 → 字数 %d～%d。
                1. 只输出 JSON，以 {"chunks": 开头，以 } 结尾。
                2. 保留标题；列表/表格不拆散；覆盖率 ≥ %.0f%%。
                """.formatted(rule.minChars(), maxAllowed, MIN_COVERAGE_PERCENT);
    }

    /**
     * 段数建议：短于一节可 1 段；约两倍 max 内常为 2 段；更长再按字数估算。
     */
    static ChunkCountHint suggestChunkCount(int originalCharCount, int maxChars) {
        int target;
        if (originalCharCount <= maxChars) {
            target = 1;
        } else if (originalCharCount <= (long) maxChars * 2 + 200) {
            target = 2;
        } else {
            target = Math.max(2, (int) Math.ceil((double) originalCharCount / TARGET_CHARS_PER_CHUNK));
        }
        int min = Math.max(1, target - 1);
        int max = target + 1;
        return new ChunkCountHint(target, min, max);
    }

    record ChunkCountHint(int target, int min, int max) {}

    private static String profileHints(String profile) {
        if (ChunkProfileDetector.MARKDOWN.equalsIgnoreCase(profile)) {
            return """
                    【本类文档 — 第一优先级：结构边界】
                    - 在 ##、### 处切开；# 下内容过长时再按 ### 或空行细分。
                    - 标题行写入该段 text 首行。
                    """;
        }
        return """
                【本类文档 — 第一优先级：结构边界】
                - 在空行、章节标题（「第×章」「一、」「（一）」「1.」「第×条」）处切开。
                - 制度/条款：一条一款可一段；FAQ：一问一答一段。
                """;
    }
}
