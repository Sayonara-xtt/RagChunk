package com.xtsh.ragchunk.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "知识库问答响应")
public class ChatResponse {

    @Schema(description = "模型生成的回答；无 API Key 时为检索片段拼接说明")
    private String answer;

    @Schema(description = "检索命中的引用片段列表")
    private List<Citation> citations;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<Citation> getCitations() { return citations; }
    public void setCitations(List<Citation> citations) { this.citations = citations; }

    @Schema(description = "检索引用片段")
    public record Citation(
            @Schema(description = "切片 ID") String chunkId,
            @Schema(description = "来源文档 ID") String docId,
            @Schema(description = "切片序号（从 0 起）") int chunkIndex,
            @Schema(description = "向量相似度分数") double score,
            @Schema(description = "片段摘要文本") String excerpt
    ) {}
}
