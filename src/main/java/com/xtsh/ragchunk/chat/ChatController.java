package com.xtsh.ragchunk.chat;

import com.xtsh.ragchunk.chat.dto.ChatRequest;
import com.xtsh.ragchunk.chat.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "问答", description = "RAG：向量检索 TopK → 千问生成（无 API Key 时返回检索原文）")
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}/chat")
public class ChatController {

    private final RagChatService ragChatService;

    public ChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    @Operation(summary = "知识库问答", description = "使用知识库 retrieval 配置做向量检索，再调用 chat 模型生成回答")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "问答成功",
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PostMapping
    public ChatResponse chat(
            @Parameter(description = "知识库 ID", required = true, example = "kb_a1b2c3d4e5f6")
            @PathVariable String kbId,
            @RequestBody(description = "问答请求体", required = true,
                    content = @Content(schema = @Schema(implementation = ChatRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody ChatRequest request) throws Exception {
        return ragChatService.chat(kbId, request.getQuestion());
    }
}
