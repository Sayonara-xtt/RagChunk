package com.xtsh.ragchunk.entity;

import lombok.NoArgsConstructor;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("document")
@Data

public class DocumentEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("kb_id")
    private String kbId;

    @TableField("file_name")
    private String fileName;

    @TableField("storage_key")
    private String storageKey;

    private String status;

    @TableField("chunk_count")
    private int chunkCount;

    private String profile;

    @TableField("quality_score")
    private int qualityScore;

    @TableField("ai_triggered")
    private boolean aiTriggered;

    @TableField("ai_trigger_id")
    private String aiTriggerId;

    @TableField("ai_fallback")
    private boolean aiFallback;

    @TableField("error_message")
    private String errorMessage;

    @TableField("batch_id")
    private String batchId;

    @TableField("process_stage")
    private String processStage;

    @TableField("progress_percent")
    private int progressPercent;

    @TableField("file_size")
    private long fileSize;

    @TableField("content_hash")
    private String contentHash;

    @TableField("storage_url")
    private String storageUrl;

    @TableField("source_type")
    private String sourceType;

    @TableField("retrain_version")
    private int retrainVersion;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

}
