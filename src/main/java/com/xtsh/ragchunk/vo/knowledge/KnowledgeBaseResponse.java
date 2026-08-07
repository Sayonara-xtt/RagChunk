package com.xtsh.ragchunk.vo.knowledge;

import lombok.Data;
import com.xtsh.ragchunk.dto.knowledge.KnowledgeBaseConfig;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "知识库查询/创建响应（对应表 knowledge_base，规则在 config 内）")
@Data

public class KnowledgeBaseResponse {

    @Schema(description = "知识库主键", example = "kb_93154bcde024", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "知识库名称", example = "kb-test-20260520-153630", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "知识库描述", example = "API test")
    private String description;

    @Schema(description = "知识库状态；创建成功后一般为 READY", example = "READY", allowableValues = {"READY"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "合并后的完整配置快照（chunking / rule / quality / ai / embedding / retrieval）", requiredMode = Schema.RequiredMode.REQUIRED)
    private KnowledgeBaseConfig config;

    @Schema(description = "创建时间（UTC，ISO-8601）", example = "2026-05-20T07:36:31.068083Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;

}
