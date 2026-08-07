package com.xtsh.ragchunk.entity;

import lombok.NoArgsConstructor;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("knowledge_base")
@Data

public class KnowledgeBaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    private String description;

    private String status;

    @TableField("config_json")
    private String configJson;

    @TableField("embedding_model")
    private String embeddingModel;

    @TableField("embedding_dimensions")
    private int embeddingDimensions;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

}
