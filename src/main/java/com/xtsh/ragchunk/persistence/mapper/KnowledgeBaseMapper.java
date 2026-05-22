package com.xtsh.ragchunk.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xtsh.ragchunk.persistence.entity.KnowledgeBaseEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {

    @Insert("""
            INSERT INTO knowledge_base (
                id, name, description, status, config_json,
                embedding_model, embedding_dimensions, created_at, updated_at
            ) VALUES (
                #{id}, #{name}, #{description}, #{status}, CAST(#{configJson} AS jsonb),
                #{embeddingModel}, #{embeddingDimensions}, #{createdAt}, #{updatedAt}
            )
            """)
    int insertRow(KnowledgeBaseEntity entity);

    @Update("""
            UPDATE knowledge_base SET
                name = #{name},
                description = #{description},
                status = #{status},
                config_json = CAST(#{configJson} AS jsonb),
                embedding_model = #{embeddingModel},
                embedding_dimensions = #{embeddingDimensions},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateRow(KnowledgeBaseEntity entity);
}
