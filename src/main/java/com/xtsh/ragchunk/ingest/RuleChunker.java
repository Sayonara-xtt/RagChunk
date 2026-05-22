package com.xtsh.ragchunk.ingest;

import com.xtsh.ragchunk.chunk.model.TextChunk;
import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则切片器（混合切片主路径；AI 校验失败时 {@link HybridChunkingService} 也回退到本类结果）。
 * <p>
 * 对应一期规则 R2，流水线顺序固定：
 * <ol>
 *   <li>按 profile 选分隔符列表，优先切出「有意义」的多段（R2-1）</li>
 *   <li>单段超长则按 maxChars 硬切，可选句界回退（R2-2 / R2-3）</li>
 *   <li>过短段与相邻段合并（R2-4）</li>
 *   <li>标记弱边界，供 {@link ChunkQualityEvaluator} 算 quality_score、触发 T2</li>
 *   <li>相邻段重叠 overlap 字符（R2-5）</li>
 * </ol>
 * profile 由 {@link ChunkProfileDetector} 决定：仅 .md 用 markdownSeparators，其余（含 docx/xlsx）用 plainSeparators。
 */
@Component
public class RuleChunker {

    /** 句界回退时向前扫描的标点（R2-3），与中文、英文常见句末一致 */
    private static final String BOUNDARY_CHARS = "。！？；.!?;";

    /**
     * 对规范化后的全文做规则切片。
     *
     * @param text    已规范化正文（非原始二进制）
     * @param profile {@link ChunkProfileDetector#MARKDOWN} 或 {@link ChunkProfileDetector#PLAIN}
     * @param rule    来自知识库 config_json 快照，创建库时合并默认值
     */
    public List<TextChunk> chunk(String text, String profile, KnowledgeBaseConfig.RuleConfig rule) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        // R2-1：markdown / plain 使用不同分隔符优先级（见知识库 rule.plainSeparators / markdownSeparators）
        var separators = ChunkProfileDetector.MARKDOWN.equals(profile)
                ? rule.markdownSeparators() : rule.plainSeparators();
        List<String> parts = splitBySeparators(text, separators);
        // R2-2：对仍超 maxChars 的段做二次切分（可能触发 R2-3 句界回退）
        List<String> sized = new ArrayList<>();
        for (String part : parts) {
            sized.addAll(enforceMaxSize(part, rule));
        }
        // R2-4：合并长度 < minChars 的碎段，减少检索噪音
        sized = mergeShort(sized, rule.minChars());
        List<TextChunk> chunks = new ArrayList<>();
        for (int i = 0; i < sized.size(); i++) {
            var c = new TextChunk(i, sized.get(i));
            // 弱边界标记进入 quality_score，aiMode=auto 时可能触发千问重切（T2）
            c.setWeakBoundary(isWeakBoundary(c.getText()));
            chunks.add(c);
        }
        // R2-5：后段头部拼接前段尾部 overlap 字，缓解「硬切断句」导致的检索丢上下文
        return applyOverlap(chunks, rule.overlap());
    }

    /**
     * R2-1：按分隔符优先级尝试切分。
     * <p>
     * 对 separators 列表从左到右：若某分隔符能把当前文本切成 <strong>至少 2 段</strong>，则采用该次切分并停止；
     * 否则继续尝试下一分隔符。若全部失败，退回整段 trim 后的原文（可能后续由 enforceMaxSize 再切）。
     */
    private List<String> splitBySeparators(String text, List<String> separators) {
        List<String> current = List.of(text);
        for (String sep : separators) {
            List<String> next = new ArrayList<>();
            for (String part : current) {
                if (part.contains(sep)) {
                    // Pattern.quote：分隔符可能含正则特殊字符（如 \n## ）
                    String[] split = part.split(java.util.regex.Pattern.quote(sep), -1);
                    for (int i = 0; i < split.length; i++) {
                        String s = split[i].trim();
                        if (!s.isEmpty()) next.add(s);
                    }
                } else if (!part.isBlank()) {
                    next.add(part.trim());
                }
            }
            if (next.size() > 1) {
                current = next;
                break;
            }
            current = next.isEmpty() ? current : next;
        }
        return current.isEmpty() ? List.of(text.trim()) : current;
    }

    /**
     * R2-2：单段长度上限 maxChars；超过则滑动窗口切分。
     * R2-3：若 sentenceBoundaryFallback=true，在 [start,end) 窗口内从后向前找句界标点，避免拦腰切断句子。
     */
    private List<String> enforceMaxSize(String text, KnowledgeBaseConfig.RuleConfig rule) {
        if (text.length() <= rule.maxChars()) {
            return List.of(text);
        }
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + rule.maxChars(), text.length());
            if (end < text.length() && rule.sentenceBoundaryFallback()) {
                int boundary = findBoundary(text, start, end);
                if (boundary > start) end = boundary;
            }
            out.add(text.substring(start, end).trim());
            start = end;
        }
        return out;
    }

    /**
     * 在 [start, end) 窗口的后 80% 区间内向前找最近句界（R2-3）。
     * 找不到则返回 end，由调用方按 maxChars 硬切。
     */
    private int findBoundary(String text, int start, int end) {
        for (int i = end - 1; i > start + (end - start) / 5; i--) {
            if (BOUNDARY_CHARS.indexOf(text.charAt(i)) >= 0) {
                return i + 1;
            }
        }
        return end;
    }

    /**
     * R2-4：累积过短片段；当前缓冲不足 minChars 时与下一段用双换行拼接，否则落盘并开始新缓冲。
     */
    private List<String> mergeShort(List<String> parts, int minChars) {
        if (parts.isEmpty()) return parts;
        List<String> merged = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String p : parts) {
            if (buf.isEmpty()) {
                buf.append(p);
            } else if (buf.length() < minChars) {
                buf.append("\n\n").append(p);
            } else {
                merged.add(buf.toString());
                buf = new StringBuilder(p);
            }
        }
        if (!buf.isEmpty()) merged.add(buf.toString());
        return merged;
    }

    /**
     * R2-5：从第二段起，将前一段末尾 overlap 个字符拼到本段开头（修改本段 text，不重复计数 charLen 前的索引）。
     * 注意：overlap 后单段实际长度可能 &gt; maxChars，一期接受该行为以换取检索上下文连贯。
     */
    private List<TextChunk> applyOverlap(List<TextChunk> chunks, int overlap) {
        if (overlap <= 0 || chunks.size() < 2) return chunks;
        for (int i = 1; i < chunks.size(); i++) {
            var prev = chunks.get(i - 1).getText();
            if (prev.length() > overlap) {
                String tail = prev.substring(prev.length() - overlap);
                chunks.get(i).setText(tail + chunks.get(i).getText());
            }
        }
        return chunks;
    }

    /**
     * 弱边界启发式：段首/段尾像被硬切断（续写标点开头、未闭合括号结尾等）。
     * 与 {@link ChunkQualityEvaluator} 中 weak_boundary_ratio 联动，影响 quality_score 与 T2 触发。
     */
    private boolean isWeakBoundary(String text) {
        if (text == null || text.isEmpty()) return true;
        char first = text.charAt(0);
        char last = text.charAt(text.length() - 1);
        if ("，、；：）】」".indexOf(first) >= 0) return true;
        if ("（【「".indexOf(last) >= 0) return true;
        return false;
    }
}
