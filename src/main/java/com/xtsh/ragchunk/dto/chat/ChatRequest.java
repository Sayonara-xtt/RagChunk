package com.xtsh.ragchunk.dto.chat;

import lombok.Data;
import com.xtsh.ragchunk.service.chat.QaScheme;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识库问答请求")
@Data

public class ChatRequest {

    @Schema(description = "用户自然语言问题", example = "一期离线建库有几步？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Schema(description = "单次覆盖问答方案：1=纯应用 2=协作渐进 3=协作全量 5=Agent；不传则用知识库 qa.scheme", example = "2")
    private Integer qaScheme;

}
