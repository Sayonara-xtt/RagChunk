# RagChunk

基于 **Spring Boot 4** 的 RAG 知识库服务：创建库 → 上传文档 → **混合切片**（规则 + 千问/Ollama 按需）→ 向量入库 → 检索问答。

> **⚠️ 一期说明**：功能 **开发基本完成**，**尚未进行全面测试**，解析/切片/AI/问答的 **正确性难以保证**，当前仅供开发联调，不宜作为生产就绪版本。详见 [docs/一期开发进度.md](docs/一期开发进度.md)。

| 项 | 说明 |
|----|------|
| 技术栈 | Java 17、Spring Boot 4.0.6、MyBatis Plus、PostgreSQL + pgvector、Flyway |
| 一期能力 | 离线建库 5 步、混合切片 T0/T1/T2/T4/T8、`SEMANTIC_RESPLIT`、pgvector 检索 |
| **交付状态** | **开发完成 · 测试不充分 · 正确性未保证** |
| 支持格式 | `txt` / `md` / `docx` / `xlsx` / `xls`（PDF 二期） |
| **开发进度** | [docs/一期开发进度.md](docs/一期开发进度.md)（**对外简版**）；[详细版](docs/一期开发进度-详细版.md)（研发） |
| **创建库参数** | [docs/创建知识库接口参数.md](docs/创建知识库接口参数.md) |

---

## 目录

- [一期 API 速览](#一期-api-速览)
- [快速开始](#快速开始)
- [构建与运行](#构建与运行)
- [配置与 Profile](#配置与-profile)
- [RAG 与业务流程](#rag-与业务流程)
- [混合切片（规则 + AI）](#混合切片规则--ai)
- [分段与检索参数](#分段与检索参数)
- [数据库](#数据库)
- [测试](#测试)
- [本地 Ollama](#本地-ollama)
- [本机安装 pgvector](#本机安装-pgvector-windows)
- [模块结构](#模块结构)
- [二期规划](#二期规划)
- [归档文档（docs/archive）](#归档文档docsarchive)

---

## 一期 API 速览

```
POST 创建知识库 → POST 上传文档 → GET 切片 / POST 问答
```

| 步骤 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建知识库 | `POST` | `/api/v1/knowledge-bases` | 名称 + 切片/检索/AI 等全量配置 |
| 查询知识库 | `GET` | `/api/v1/knowledge-bases` | 全部列表 |
| 查询单个 | `GET` | `/api/v1/knowledge-bases?id={kbId}` | 按 ID |
| 上传文档 | `POST` | `/api/v1/knowledge-bases/{kbId}/documents?smartChunk=false` | `multipart/form-data`，字段 `file` |
| 文档列表 | `GET` | `/api/v1/knowledge-bases/{kbId}/documents` | |
| 切片列表 | `GET` | `/api/v1/knowledge-bases/{kbId}/chunks` | `docId` 可选；空则返回该库全部切片 |
| 单文档切片 | `GET` | `/api/v1/knowledge-bases/{kbId}/documents/{docId}/chunks` | |
| 问答 | `POST` | `/api/v1/knowledge-bases/{kbId}/chat` | JSON：`{"question":"..."}` |

**Swagger UI**（启动后）：

| 入口 | URL |
|------|-----|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| 兼容入口 | http://localhost:8080/doc.html（重定向） |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

> Knife4j 4.5 与 SpringDoc 3 / Boot 4 不兼容，已改用 SpringDoc 官方 UI。详见 [docs/archive/testing.md](docs/archive/testing.md)。

上传链路日志前缀：**`[文档上传]`**（便于排查解析、切片、AI 回退）。

---

## 快速开始

### 1. 环境

- JDK 17+
- PostgreSQL 16+ 与 **pgvector**（本机 17 见 [本机安装 pgvector](#本机安装-pgvector-windows)）
- 可选：`DASHSCOPE_API_KEY`（通义千问）或 Ollama（见 [本地 Ollama](#本地-ollama)）

### 2. 启动

```powershell
cd d:\code\RagChunk
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
# 或：.\scripts\dev-run.cmd（默认 local+ollama；仅 PG：dev-run.cmd nollama）
```

### 3. 调用示例

**创建知识库**

```powershell
$body = @{
  name = "demo"
  chunking = @{ aiMode = "auto" }
  rule = @{ maxChars = 1200 }
  retrieval = @{ topK = 3; scoreThreshold = 0.5 }
} | ConvertTo-Json -Depth 5

$kb = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/knowledge-bases" `
  -ContentType "application/json" -Body $body
$kbId = $kb.id
```

**上传文档**

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/knowledge-bases/$kbId/documents?smartChunk=true" `
  -Form @{ file = Get-Item ".\scripts\sample.md" }
```

**问答**

```powershell
$chatBody = '{"question":"你的问题"}'
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/knowledge-bases/$kbId/chat" `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($chatBody))
```

**一键端到端**

```powershell
.\scripts\test-e2e.ps1
```

### 4. 运行模式

| 模式 | 条件 | 行为 |
|------|------|------|
| 离线开发 | 无 `DASHSCOPE_API_KEY`，未开 Ollama | 本地 hash 向量；切片以规则为主；问答返回检索片段摘要 |
| 通义千问 | 设置 `DASHSCOPE_API_KEY` | DashScope Embedding + 千问切片/问答 |
| 本地 Ollama | `local,ollama` profile | OpenAI 兼容接口做切片与问答；向量默认本地 hash |

---

## 构建与运行

项目使用 **Maven Wrapper**（`mvnw.cmd`），无需系统安装 Maven：

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

| 脚本 | 说明 |
|------|------|
| `scripts\dev-run.cmd` | 本地开发启动（默认 `local,ollama`；`nollama` 仅本机库） |
| `scripts\stop-app.cmd` | 释放 8080 端口 |
| `scripts\mvn.ps1` | 包装 `mvnw` 的 PowerShell 入口 |
| `scripts\test-e2e.ps1` | 创建库 → 上传 → 问答 |

---

## 配置与 Profile

| Profile | 说明 |
|---------|------|
| `local`（默认） | 本机 PostgreSQL `localhost:5432/ragchunk`，`postgres` / `123` |
| `ollama` | 本地大模型 `application-ollama.yaml` |
| `docker` | Docker PostgreSQL，`localhost:5433` |
| `array` | 本机 PG，`real[]` 向量，无 pgvector |
| `inmemory` | 内存存储，单元测试/演示 |

```yaml
# 一期核心配置（application.yaml / 创建库请求体）
ragchunk:
  chunking:
    mode: hybrid
    ai-mode: auto              # never | auto | always
  rule:
    max-chars: 1200
    min-chars: 80
    overlap: 80
  quality:
    score-threshold: 70
  ai:
    chunk-model: qwen-plus
    max-calls-per-doc: 1
    max-input-tokens: 8000
  embedding:
    model: text-embedding-v3
    dimensions: 1024
  retrieval:
    top-k: 3
    score-threshold: 0.5
```

**环境变量（数据库）**

| 变量 | 默认 | 说明 |
|------|------|------|
| `RAGCHUNK_DB_HOST` | `localhost` | |
| `RAGCHUNK_DB_PORT` | `5432` | Docker 常用 `5433` |
| `RAGCHUNK_DB_NAME` | `ragchunk` | |
| `RAGCHUNK_DB_USER` | `postgres` / `ragchunk` | 随 profile |
| `RAGCHUNK_DB_PASSWORD` | `123` / `ragchunk` | |

---

## RAG 与业务流程

### 标准 RAG 三阶段

| 阶段 | 含义 | 在线步骤 |
|------|------|----------|
| **检索** Retrieve | 从向量库找相关 Chunk | Query 向量化 → TopK / 阈值 → 可选 Rerank |
| **增强** Augment | 把命中片段写入 Prompt | 组装上下文 |
| **生成** Generate | LLM 有据作答 | 千问 / Ollama |

### 离线建库（一期 5 步）

```mermaid
flowchart TD
    S1[1 创建知识库] --> S2[2 上传文件]
    S2 --> S3[3 解析 + 文本规范化]
    S3 --> S4[4 混合切片<br/>规则 → 评估 → AI 可选]
    S4 --> S5[5 向量化入库]
```

| 步骤 | 一期内容 | 二期规划 |
|------|----------|----------|
| 创建库 | 名称 + 全局默认配置 | 分步向导 |
| 上传 | txt/md/docx/xlsx/xls | PDF、Notion、网页 |
| 解析 | R0-1～R0-3 规范化 | 去 URL 等可选 |
| 切片 | 规则 + T2/T4/T8 + `SEMANTIC_RESPLIT` | 父子模式、T3～T7、多 AI 任务 |
| 入库 | DashScope Embedding + pgvector | 经济索引、关键词倒排 |

文档处理状态：`PROCESSING` → `SUCCESS` / `FAILED`。

### 在线问答

```mermaid
sequenceDiagram
    participant U as 用户
    participant API as RagChunk API
    participant VDB as 向量库
    participant LLM as 千问/Ollama

    U->>API: 提问
    API->>API: Query 向量化
    API->>VDB: 检索 TopK / 阈值
    VDB-->>API: 相关 Chunk
    API->>LLM: 上下文 + 问题
    LLM-->>API: 回答
    API-->>U: 回答（含 citations）
```

### 关键约束

| 规则 | 说明 |
|------|------|
| Embedding 一致 | 同一知识库入库与查询须用同一向量模型 |
| 分段模式 | 通用/父子模式创建后不可改（一期仅通用） |
| 有据作答 | 无命中时应说明「未找到」，避免编造 |

业务活动编号（KB-01～QA-08）、角色与运维场景见 [docs/archive/business-process.md](docs/archive/business-process.md)。

---

## 混合切片（规则 + AI）

**原则**：先规则切片 → 质量评估 → 按需 AI → 校验失败则回退规则结果。

```mermaid
flowchart TD
    A[原始文本] --> B[预处理 R0]
    B --> C[规则切片 R2]
    C --> D[质量评估]
    D --> E{触发 AI?}
    E -->|否| F[规则 Chunk 入库]
    E -->|是| G[SEMANTIC_RESPLIT]
    G --> H{校验 V1/V2/V4}
    H -->|通过| I[hybrid 入库]
    H -->|失败| F
```

### 一期触发规则

| 触发 ID | 条件 | 任务 |
|---------|------|------|
| **T1** | `ai_mode=never` | 不调 AI |
| **T0** | `ai_mode=always` | 语义重切 |
| **T2** | `auto` 且 `quality_score < 70` | 语义重切 |
| **T4** | `auto` 且字数>1500 且仅 1 段 | 语义重切 |
| **T8** | 上传 `smartChunk=true` | 语义重切（优先） |

**决策顺序**（`auto`）：`never` → 规则；`always` → AI；`smartChunk=true` → AI；质量分低或单段过长 → AI；否则规则。

**一期质量分**（简化）：

```text
quality_score = 100 - short_ratio*40 - weak_boundary_ratio*50 - (single_chunk_doc ? 30 : 0)
```

**AI 输出**：JSON `{"chunks":[{"text":"..."}]}`；校验 V1 段长、V2 覆盖率≥95%、V4 JSON 合法；失败重试 1 次后 `ai_fallback=true`。

完整规则 R0～R3、触发 T0～T8、任务与配额见 [docs/archive/hybrid-chunking.md](docs/archive/hybrid-chunking.md)；一期裁剪见 [docs/archive/phase1-scope.md](docs/archive/phase1-scope.md)。

---

## 分段与检索参数

### 分段模式（规划对照 Dify）

| 模式 | 说明 | 一期 |
|------|------|------|
| 通用 | 单层 Chunk | ✅ |
| 父子 | 子段检索、父段返回 | 二期 |

通用参数：`max_chars`（默认 1200）、`min_chars`（80）、`overlap`（80）、分隔符按 `plain` / `markdown` profile。

### 索引（一期）

| 方式 | 一期 |
|------|------|
| 高质量向量（pgvector） | ✅ |
| 经济关键词倒排 | 二期 |
| 混合检索 / Rerank | 二期或可选 |

### 检索（问答时）

| 参数 | 默认 | 说明 |
|------|------|------|
| `topK` | 3 | 最多送入 LLM 的 Chunk 数 |
| `scoreThreshold` | 0.5 | 低于阈值的片段丢弃 |

调参速查：答非所问 → 提高阈值、减小 TopK；总说找不到 → 降低阈值或补文档。详见 [docs/archive/retrieval.md](docs/archive/retrieval.md)。

---

## 数据库

| 项 | 值 |
|----|-----|
| 引擎 | PostgreSQL 16+ |
| 扩展 | pgvector |
| Schema | Flyway `src/main/resources/db/migration` |
| ORM | MyBatis Plus（`knowledge_base`、`document`）+ JdbcTemplate（`chunk` 向量） |
| 向量维度 | 1024（与 `text-embedding-v3` 一致） |

### 表关系

```mermaid
erDiagram
    knowledge_base ||--o{ document : contains
    knowledge_base ||--o{ chunk : owns
    document ||--o{ chunk : has
```

| 表 | 说明 |
|----|------|
| `knowledge_base` | 库元数据 + `config_json` 配置快照 |
| `document` | 上传记录、状态、`quality_score`、`ai_triggered` 等 |
| `chunk` | 切片正文 + `vector(1024)`，HNSW 余弦检索 |

删除知识库 / 文档会 **CASCADE** 删除下属切片。上传失败时应用层删除该 `doc_id` 的 chunk 并标记 `FAILED`。

### 故障：`missing table [document]`

| 原因 | 处理 |
|------|------|
| 连错库 | 确认库名为 `ragchunk` |
| Flyway 未跑 | `.\mvnw.cmd clean compile` 后重启；看日志 Flyway |
| 无 pgvector | 安装扩展后重启 |
| 环境变量指向 Docker | 本机开发清除 `RAGCHUNK_DB_PORT=5433` 等 |

```sql
SELECT tablename FROM pg_tables WHERE schemaname = 'public';
SELECT * FROM flyway_schema_history;
```

完整字段说明、运维 SQL 见 [docs/archive/database.md](docs/archive/database.md)。

### Docker PostgreSQL（可选）

```powershell
.\scripts\start-postgres-docker.ps1
# 或 docker compose up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=docker"
```

---

## 测试

```powershell
.\mvnw.cmd test                    # 默认 inmemory，不依赖 PG
.\mvnw.cmd spring-boot:run         # 联调
.\scripts\test-e2e.ps1             # 端到端
```

| 测试类 | 内容 |
|--------|------|
| `KnowledgeBaseServiceTest` | 创建库、配置合并 |
| `RuleChunkerTest` | 规则切片 |
| `AiChunkTriggerTest` | T2/T8 |
| `HybridChunkingServiceTest` | `aiMode=never` |

PostgreSQL 集成测试（需 Docker）：

```powershell
$env:RUN_PG_INTEGRATION = "1"
.\mvnw.cmd test -Dtest=RagChunkPostgresIntegrationTest
```

验证清单、Swagger 调试、乱码处理见 [docs/archive/testing.md](docs/archive/testing.md)。

---

## 本地 Ollama

通过 OpenAI 兼容接口 `http://<host>:11434/v1` 调用对话与切片。

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,ollama"
# 或
$env:SPRING_PROFILES_ACTIVE = "local,ollama"
$env:OLLAMA_BASE_URL = "http://192.168.14.57:11434/v1"
$env:OLLAMA_CHAT_MODEL = "qwen2.5:14b"
```

| 项 | 默认（`application-ollama.yaml`） |
|----|-------------------------------------|
| 对话/切片模型 | `qwen2.5:14b` |
| 远程向量 | 关闭（`embedding.remote-enabled=false`，用本地 hash） |

上传 `smartChunk=true` 且库 `aiMode` 非 `never` 时会调 Ollama 做语义重切。故障排查表见 [docs/archive/ollama.md](docs/archive/ollama.md)。

---

## 本机安装 pgvector（Windows）

**管理员** cmd，项目根目录：

```cmd
cd /d d:\code\RagChunk
scripts\install-pgvector-windows.cmd -PgRoot "D:\AnZhuang\PostgreSQL17" -SuperPassword "123"
```

脚本会复制 `vector.dll`、扩展 SQL、重启 `postgresql-x64-17`、创建库 `ragchunk` 并 `CREATE EXTENSION vector`。

连接：`localhost:5432/ragchunk`，`postgres` / `123`（与 `application-local.yaml` 一致）。

手动步骤与验证命令见 [docs/archive/install-pgvector-windows.md](docs/archive/install-pgvector-windows.md)。

---

## 模块结构

```
src/main/java/com/xtsh/ragchunk/
├── knowledge/       # 知识库 CRUD、KnowledgeBaseConfig
├── document/        # 上传、DocumentService、Controller
├── ingest/          # 解析、规则切片、AI 触发、语义重切、ChunkIngestPipeline
├── chunk/           # 切片查询 API
├── embedding/       # DashScope / Ollama / 本地 hash
├── vector/          # PgVectorStore、内存 VectorStore
├── chat/            # 检索 + LLM 问答
├── integration/     # DashScope / OpenAI 兼容 HTTP
└── config/          # MyBatis、Swagger、存储模式
```

---

## 二期规划

| 文档 | 内容 |
|------|------|
| [二期规划-对象存储.md](docs/二期规划-对象存储.md) | **二期必做**：原始文件上传 **MinIO/S3**，`document.storage_key` |
| [二期规划-异步任务.md](docs/二期规划-异步任务.md) | **二期必做**：上传异步入队、处理阶段与进度轮询 API |

---

## 归档文档（docs/archive）

详细版设计文档已移至 **[docs/archive/](docs/archive/)**（日常以本文为准）：

| 文档 | 内容 |
|------|------|
| [phase1-scope.md](docs/archive/phase1-scope.md) | 一期范围与不做项 |
| [architecture.md](docs/archive/architecture.md) | 架构、与 Dify 对照 |
| [business-process.md](docs/archive/business-process.md) | 业务流程、角色、KB/QA 活动表 |
| [rag-flow.md](docs/archive/rag-flow.md) | 标准 RAG 8 步 + 一期 5 步对照 |
| [chunking.md](docs/archive/chunking.md) | 分段策略、通用/父子 |
| [hybrid-chunking.md](docs/archive/hybrid-chunking.md) | 混合切片完整规则 R0～R3、T0～T8 |
| [indexing.md](docs/archive/indexing.md) | 高质量/经济索引 |
| [retrieval.md](docs/archive/retrieval.md) | TopK、阈值、Rerank |
| [database.md](docs/archive/database.md) | 表结构、迁移、运维 SQL |
| [testing.md](docs/archive/testing.md) | 测试与 Swagger 说明 |
| [ollama.md](docs/archive/ollama.md) | Ollama 配置与排错 |
| [install-pgvector-windows.md](docs/archive/install-pgvector-windows.md) | pgvector 安装细节 |

---

## 许可证

见项目仓库约定（如有）。
