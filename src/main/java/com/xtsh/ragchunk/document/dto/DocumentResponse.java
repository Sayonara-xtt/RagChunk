package com.xtsh.ragchunk.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "文档处理结果与流程进度")
public class DocumentResponse {

    @Schema(description = "文档 ID", example = "doc_a1b2c3d4e5f6")
    private String id;

    @Schema(description = "所属知识库 ID", example = "kb_a1b2c3d4e5f6")
    private String kbId;

    @Schema(description = "原始文件名", example = "sample.md")
    private String fileName;

    @Schema(description = "粗粒度状态", example = "PROCESSING",
            allowableValues = {"QUEUED", "PROCESSING", "SUCCESS", "FAILED"})
    private String status;

    @Schema(description = "流程阶段", example = "CHUNKING")
    private String processStage;

    @Schema(description = "流程阶段中文", example = "混合切片")
    private String processStageLabel;

    @Schema(description = "进度 0-100", example = "55")
    private int progressPercent;

    @Schema(description = "切片数量", example = "12")
    private int chunkCount;

    @Schema(description = "文档类型画像", example = "markdown")
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

    @Schema(description = "批量任务 ID")
    private String batchId;

    @Schema(description = "文件大小（字节）")
    private long fileSize;

    @Schema(description = "OSS 存储键")
    private String storageKey;

    @Schema(description = "原件访问 URL（默认 MinIO 风格地址）")
    private String storageUrl;

    @Schema(description = "内容 SHA-256")
    private String contentHash;

    @Schema(description = "上传来源", example = "LOCAL_BATCH")
    private String sourceType;

    @Schema(description = "重复训练次数")
    private int retrainVersion;

    @Schema(description = "上传时间（UTC）")
    private Instant createdAt;

    @Schema(description = "更新时间（UTC）")
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProcessStage() { return processStage; }
    public void setProcessStage(String processStage) { this.processStage = processStage; }
    public String getProcessStageLabel() { return processStageLabel; }
    public void setProcessStageLabel(String processStageLabel) { this.processStageLabel = processStageLabel; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
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
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getStorageUrl() { return storageUrl; }
    public void setStorageUrl(String storageUrl) { this.storageUrl = storageUrl; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public int getRetrainVersion() { return retrainVersion; }
    public void setRetrainVersion(int retrainVersion) { this.retrainVersion = retrainVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
