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

@Tag(name = "问答", description = "智能问答：方案 1 纯应用 / 2 协作渐进 / 3 协作全量 / 5 Agent（可配置 qa.scheme）")
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}/chat")
public class ChatController {

    private final RagChatService ragChatService;

    public ChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    @Operation(summary = "知识库问答", description = "按知识库 qa.scheme（或请求体 qaScheme）编排检索与 LLM；响应 meta 含 llmCalls、searchRounds")
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
        return ragChatService.chat(kbId, request.getQuestion(), request.getQaScheme());
    }
}
