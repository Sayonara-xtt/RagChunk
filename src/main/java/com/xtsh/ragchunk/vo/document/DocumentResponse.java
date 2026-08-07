package com.xtsh.ragchunk.vo.document;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "文档处理结果与流程进度")
@Data

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

}
