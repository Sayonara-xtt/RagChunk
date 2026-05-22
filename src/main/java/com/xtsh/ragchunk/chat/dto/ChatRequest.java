package com.xtsh.ragchunk.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识库问答请求")
public class ChatRequest {

    @Schema(description = "用户自然语言问题", example = "一期离线建库有几步？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
