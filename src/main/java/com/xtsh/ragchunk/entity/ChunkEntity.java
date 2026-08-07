package com.xtsh.ragchunk.entity;

import lombok.NoArgsConstructor;
import lombok.Data;
import com.xtsh.ragchunk.vector.VectorStore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

/**
 * chunk 表元数据映射（不含 embedding；向量读写仍由 VectorStore 负责）。
 */
@TableName("chunk")
@Data

public class ChunkEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("kb_id")
    private String kbId;

    @TableField("doc_id")
    private String docId;

    @TableField("chunk_index")
    private int chunkIndex;

    @TableField("text_content")
    private String textContent;

    private String source;

    @TableField("created_at")
    private Instant createdAt;

}
