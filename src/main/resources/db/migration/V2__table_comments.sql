-- 表与字段注释（与 docs/archive/database.md 一致）

COMMENT ON TABLE knowledge_base IS '知识库：切片/向量/检索配置快照';
COMMENT ON COLUMN knowledge_base.id IS '主键，如 kb_a1b2c3d4e5f6';
COMMENT ON COLUMN knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN knowledge_base.description IS '描述';
COMMENT ON COLUMN knowledge_base.status IS '状态，如 READY';
COMMENT ON COLUMN knowledge_base.config_json IS '合并后的 KnowledgeBaseConfig JSON 快照';
COMMENT ON COLUMN knowledge_base.embedding_model IS '入库与检索使用的 Embedding 模型名';
COMMENT ON COLUMN knowledge_base.embedding_dimensions IS '向量维度，默认 1024';
COMMENT ON COLUMN knowledge_base.created_at IS '创建时间（UTC）';
COMMENT ON COLUMN knowledge_base.updated_at IS '更新时间（UTC）';

COMMENT ON TABLE document IS '文档：上传、切片状态与质量元数据';
COMMENT ON COLUMN document.id IS '主键，如 doc_xxxxxxxxxxxx';
COMMENT ON COLUMN document.kb_id IS '所属知识库 ID，外键 knowledge_base.id，级联删除';
COMMENT ON COLUMN document.file_name IS '原始文件名';
COMMENT ON COLUMN document.storage_key IS '对象存储路径（预留，一期未持久化文件）';
COMMENT ON COLUMN document.status IS '处理状态：PROCESSING / SUCCESS / FAILED';
COMMENT ON COLUMN document.chunk_count IS '成功入库的切片数量';
COMMENT ON COLUMN document.profile IS '文档画像：PLAIN / MARKDOWN 等';
COMMENT ON COLUMN document.quality_score IS '规则切片质量分，0–100';
COMMENT ON COLUMN document.ai_triggered IS '是否触发千问重切';
COMMENT ON COLUMN document.ai_trigger_id IS 'AI 触发编号，如 T2、T8';
COMMENT ON COLUMN document.ai_fallback IS '千问失败时是否回退为规则切片结果';
COMMENT ON COLUMN document.error_message IS '失败原因说明';
COMMENT ON COLUMN document.created_at IS '上传时间（UTC）';
COMMENT ON COLUMN document.updated_at IS '最后更新时间（UTC）';

COMMENT ON TABLE chunk IS '切片与向量：检索主表';
COMMENT ON COLUMN chunk.id IS '切片主键，如 {docId}_c0000';
COMMENT ON COLUMN chunk.kb_id IS '所属知识库 ID，外键 knowledge_base.id，级联删除';
COMMENT ON COLUMN chunk.doc_id IS '来源文档 ID，外键 document.id，级联删除';
COMMENT ON COLUMN chunk.chunk_index IS '文档内切片序号，从 0 起';
COMMENT ON COLUMN chunk.text_content IS '切片正文，用于展示与检索上下文';
COMMENT ON COLUMN chunk.source IS '切片来源标记：规则切片或千问重切';
COMMENT ON COLUMN chunk.embedding IS 'pgvector 向量，维度 1024，余弦检索';
COMMENT ON COLUMN chunk.created_at IS '向量写入时间（UTC）';
