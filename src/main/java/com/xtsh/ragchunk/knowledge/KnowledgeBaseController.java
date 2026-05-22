package com.xtsh.ragchunk.knowledge;

import com.xtsh.ragchunk.knowledge.dto.CreateKnowledgeBaseRequest;
import com.xtsh.ragchunk.knowledge.dto.KnowledgeBaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "知识库", description = "创建与查询知识库（配置快照）")
@RestController
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService service;

    public KnowledgeBaseController(KnowledgeBaseService service) {
        this.service = service;
    }

    @Operation(summary = "创建知识库", description = "请求体可传 chunking/rule/quality/ai/embedding/retrieval；未传字段与 application.yaml 默认合并")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = KnowledgeBaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数校验失败")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeBaseResponse create(
            @RequestBody(description = "创建知识库参数", required = true,
                    content = @Content(schema = @Schema(implementation = CreateKnowledgeBaseRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody CreateKnowledgeBaseRequest request) {
        return service.create(request);
    }

    @Operation(summary = "查询知识库", description = "不传 id：返回全部（按创建时间倒序）；传 id：返回单个")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功（无 id 时为数组，有 id 时为对象）",
                    content = @Content(schema = @Schema(implementation = KnowledgeBaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "知识库不存在（传 id 时）")
    })
    @GetMapping
    public Object query(
            @Parameter(description = "知识库 ID；不传则返回全部", example = "kb_a1b2c3d4e5f6")
            @RequestParam(value = "id", required = false) String id) {
        if (id != null && !id.isBlank()) {
            return service.getById(id.trim());
        }
        return service.listAll();
    }
}
