package com.xtsh.ragchunk.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "文档处理结果")
public class DocumentResponse {

    @Schema(description = "文档 ID", example = "doc_a1b2c3d4e5f6")
    private String id;

    @Schema(description = "所属知识库 ID", example = "kb_a1b2c3d4e5f6")
    private String kbId;

    @Schema(description = "原始文件名", example = "sample.md")
    private String fileName;

    @Schema(description = "处理状态", example = "INDEXED", allowableValues = {"INDEXED", "FAILED", "PROCESSING"})
    private String status;

    @Schema(description = "切片数量", example = "12")
    private int chunkCount;

    @Schema(description = "文档类型画像", example = "MARKDOWN", allowableValues = {"PLAIN", "MARKDOWN", "DOCX"})
    private String profile;

    @Schema(description = "规则切片质量分 0-100", example = "85")
    private int qualityScore;

    @Schema(description = "是否触发千问语义重切", example = "false")
    private boolean aiTriggered;

    @Schema(description = "触发的规则编号", example = "T2")
    private String aiTriggerId;

    @Schema(description = "千问失败是否回退规则切片", example = "false")
    private boolean aiFallback;

    @Schema(description = "失败原因（status=FAILED 时有值）")
    private String errorMessage;

    @Schema(description = "上传时间（UTC）")
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
    public boolean isAiTriggered() { return aiTriggered; }
    public void setAiTriggered(boolean aiTriggered) { this.aiTriggered = aiTriggered; }
    public String getAiTriggerId() { return aiTriggerId; }
    public void setAiTriggerId(String aiTriggerId) { this.aiTriggerId = aiTriggerId; }
    public boolean isAiFallback() { return aiFallback; }
    public void setAiFallback(boolean aiFallback) { this.aiFallback = aiFallback; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
