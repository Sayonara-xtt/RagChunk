package com.xtsh.ragchunk.controller.chat;


import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.service.chat.QaScheme;
import com.xtsh.ragchunk.service.chat.RagChatService;
import com.xtsh.ragchunk.service.chat.trace.ChatRunIdGenerator;
import com.xtsh.ragchunk.dto.chat.ChatRequest;
import com.xtsh.ragchunk.vo.chat.ChatResponse;
import jakarta.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "问答", description = "智能问答：方案 1 纯应用 / 2 协作渐进 / 3 协作全量 / 5 Agent（可配置 qa.scheme）")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/knowledge-bases/{kbId}/chat")
public class ChatController {

    private final RagChatService ragChatService;
    private final ChatRunIdGenerator runIdGenerator;

    @Operation(summary = "知识库问答", description = "按知识库 qa.scheme（或请求体 qaScheme）编排检索与 LLM；响应 meta 含 llmCalls、searchRounds")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "问答成功",
                    headers = @Header(
                            name = "X-RagChunk-Run-Id",
                            description = "本次问答运行唯一标识",
                            schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PostMapping
    public ChatResponse chat(
            @Parameter(description = "知识库 ID", required = true, example = "kb_a1b2c3d4e5f6")
            @PathVariable String kbId,
            @RequestBody(description = "问答请求体", required = true,
                    content = @Content(schema = @Schema(implementation = ChatRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody ChatRequest request,
            HttpServletResponse servletResponse) throws Exception {
        String runId = runIdGenerator.generate();
        servletResponse.setHeader("X-RagChunk-Run-Id", runId);
        ChatResponse response = ragChatService.chat(runId, kbId, request.getQuestion(), request.getQaScheme());
        response.setRunId(runId);
        return response;
    }
}
