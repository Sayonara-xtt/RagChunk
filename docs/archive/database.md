> **历史摘要**。当前数据库结构以 `src/main/resources/db/migration*` 为唯一事实来源，当前测试入口以 [TDD工作流](../tdd/TDD工作流.md) 为准。

# 历史数据、环境与测试说明

本文合并早期数据库、Windows pgvector、Ollama 和测试指南。固定密码、固定局域网地址、旧工作目录和已过时依赖说明已删除。

## 1. 数据技术基线

| 项目 | 选择 |
|---|---|
| 数据库 | PostgreSQL 16+ |
| 向量扩展 | pgvector |
| 迁移管理 | Flyway |
| 业务访问 | MyBatis Plus |
| 向量访问 | JdbcTemplate / pgvector |
| 开发替代 | `ragchunk.storage.mode=inmemory` |

数据库迁移是结构事实来源。已经发布的迁移不得修改，应通过新增版本演进。

## 2. 历史数据关系

```text
knowledge_base
├─ document
│  └─ chunk
└─ upload_batch
```

- `knowledge_base` 保存知识库元数据和配置快照。
- `document` 保存原始文档、处理状态、质量和归档信息。
- `chunk` 保存切片正文、来源和向量。
- `upload_batch` 保存批量上传统计。

删除和生命周期行为以当前迁移外键及业务服务实现为准，不依据本归档执行生产数据删除。

## 3. PostgreSQL 与 pgvector

推荐优先使用项目提供的 Docker 方式：

```powershell
.\scripts\start-postgres-docker.ps1
```

本机 PostgreSQL 可以使用项目安装脚本，但应通过环境变量传入凭据：

```powershell
.\scripts\install-pgvector-windows.cmd -PgRoot "<PostgreSQL安装目录>" -SuperPassword "<管理员密码>"
```

扩展自检：

```sql
SELECT extname, extversion
FROM pg_extension
WHERE extname = 'vector';
```

常见 `missing table` 排查顺序：

1. 确认应用连接的是目标数据库。
2. 检查 pgvector 扩展是否可用。
3. 检查 `flyway_schema_history` 和启动日志。
4. 不要直接修改已执行迁移来绕过 checksum。

## 4. Embedding 不变量

- `vector(n)` 的维度必须与 Embedding 输出一致。
- 同一知识库入库和查询必须使用兼容模型。
- 修改模型或维度需要新索引版本和重建流程。
- 本地 Hash Embedding 只适合开发联调，不能作为语义质量依据。

## 5. Ollama 历史接入方式

项目曾通过 OpenAI 兼容的 `/v1/chat/completions` 接口连接 Ollama。有效配置以 `application-ollama.yaml` 为准，地址和模型通过环境变量覆盖，不在文档中固化局域网地址。

历史注意事项：

- Chat 模型和 Embedding 模型职责不同。
- Embedding 模型输出维度必须与数据库一致。
- 仅使用 Ollama Chat、关闭远程 Embedding 时，会回退本地 Hash Embedding。
- 连接失败应检查 `/v1` 路径、模型是否存在和网络访问策略。

## 6. 当前测试入口

```powershell
# 单元回归
.\scripts\tdd-test.ps1 -Mode unit -Phase REGRESSION

# 不启用真实 PostgreSQL 的集成基线
.\scripts\tdd-test.ps1 -Mode integration -Phase REGRESSION

# PostgreSQL/Testcontainers 集成
.\scripts\tdd-test.ps1 -Mode integration -WithPostgres -Phase REGRESSION
```

端到端脚本仍应以 [已知测试基线](../tdd/已知基线.md) 为约束；旧文档中的同步上传检查已经失效，不能作为生产验收证据。

## 7. 保留的运维自检

```sql
SELECT COUNT(*) FROM knowledge_base;

SELECT d.id, d.file_name, d.status, d.chunk_count, COUNT(c.id) AS actual_chunks
FROM document d
LEFT JOIN chunk c ON c.doc_id = d.id
WHERE d.kb_id = '<knowledge-base-id>'
GROUP BY d.id, d.file_name, d.status, d.chunk_count;
```

所有生产连接信息必须通过环境或密钥管理提供，不提交真实密码、Token、固定主机地址和用户上传数据。
