package com.xtsh.ragchunk.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识库问答请求")
public class ChatRequest {

    @Schema(description = "用户自然语言问题", example = "一期离线建库有几步？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Schema(description = "单次覆盖问答方案：1=纯应用 2=协作渐进 3=协作全量 5=Agent；不传则用知识库 qa.scheme", example = "2")
    private Integer qaScheme;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Integer getQaScheme() { return qaScheme; }
    public void setQaScheme(Integer qaScheme) { this.qaScheme = qaScheme; }
}
