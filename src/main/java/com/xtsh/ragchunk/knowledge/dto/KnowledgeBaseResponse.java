package com.xtsh.ragchunk.knowledge.dto;

import com.xtsh.ragchunk.knowledge.model.KnowledgeBaseConfig;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "知识库查询/创建响应（对应表 knowledge_base，规则在 config 内）")
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public KnowledgeBaseConfig getConfig() { return config; }
    public void setConfig(KnowledgeBaseConfig config) { this.config = config; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
