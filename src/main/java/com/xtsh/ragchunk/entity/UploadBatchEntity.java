package com.xtsh.ragchunk.entity;

import lombok.NoArgsConstructor;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("upload_batch")
@Data

public class UploadBatchEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    @TableField("kb_id")
    private String kbId;
    @TableField("source_type")
    private String sourceType;
    private String status;
    @TableField("total_count")
    private int totalCount;
    @TableField("queued_count")
    private int queuedCount;
    @TableField("processing_count")
    private int processingCount;
    @TableField("success_count")
    private int successCount;
    @TableField("failed_count")
    private int failedCount;
    @TableField("smart_chunk")
    private boolean smartChunk;
    @TableField("error_message")
    private String errorMessage;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;

}
