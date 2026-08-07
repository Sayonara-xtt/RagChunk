# 文档上传：异步、流式与 OSS 归档

> 更新：2026-05-22 · 与当前代码对齐  
> 功能状态见 [开发进度.md](开发进度.md) §3、§6

## 1. 能力概览

| 能力 | 说明 |
|------|------|
| 统一上传 | 仅 `POST .../documents`，**HTTP 202**（无同步阻塞） |
| 流式归档 | 请求线程 `InputStream` → 8KB 缓冲写盘，**不用 `getBytes()`** |
| 单文件 / 批量 | `file` 或 `files`；多文件自动创建 `upload_batch` |
| 原件存储 | 默认 `ragchunk.oss.provider=local`，URL 为 MinIO 风格 |
| 后台建库 | 线程池 `documentIngestTaskExecutor`：解析 → 切片 → 向量 |
| 流程查询 | `processStage`、`progressPercent` |
| 重复训练 | `POST .../retrain`，从已归档原件重跑切片/向量 |
| 看板 | `upload-dashboard`、`upload-batches/{batchId}` |

**已移除**：`POST .../documents/async`、`.../batch`、同步 201 建库。

---

## 2. 业务流程（两阶段）

```text
【阶段 A · HTTP 请求线程 — 受理 + 流式归档】
  客户端 multipart
    → DocumentController.upload (202)
    → DocumentAsyncUploadService.submit / submitAsync / submitBatch
    → 创建 document (QUEUED) [+ upload_batch 若多文件]
    → DocumentStreamArchiveService.archiveFromUpload
         MultipartFile.getInputStream()
         → ObjectStorageService.putStream (SHA-256 + 写 local-root)
         → 更新 storageKey / storageUrl / fileSize / contentHash
    → DocumentIngestExecutor.runAsync (入队，无 byte[])
    → 返回 DocumentUploadResponse

【阶段 B · 线程池 — 离线建库】
  DocumentIngestExecutor.run
    → ChunkIngestPipeline.ingestFromStorage
         objectStorage.openStream(storageKey)
         → DocumentParser.parseStream
         → TextNormalizer → HybridChunkingService → Embedding → pgvector
    → document.status = SUCCESS | FAILED
```

### 流程阶段（`processStage`）

| 阶段 | 说明 | 默认进度 |
|------|------|----------|
| `QUEUED` | 记录已创建 / 等待线程池 | 0% |
| `OSS_ARCHIVING` | 流式写原件（多在 202 返回前完成） | 10% |
| `PARSING` | 解析 + 规范化 | 30% |
| `CHUNKING` | 规则 / 混合切片 | 55% |
| `EMBEDDING` | 向量化写入 | 80% |
| `SUCCESS` | 完成 | 100% |
| `FAILED` | 失败（见 `errorMessage`） | — |

---

## 3. API 清单

基础路径：`/api/v1/knowledge-bases/{kbId}`

| 方法 | 路径 | 状态码 | 说明 |
|------|------|--------|------|
| `POST` | `/documents` | 202 | 上传（`file` / `files`） |
| `GET` | `/documents` | 200 | 文档列表 |
| `GET` | `/documents/{docId}` | 200 | 详情 + 流程进度 |
| `POST` | `/documents/{docId}/retrain` | 202 | 重复训练 |
| `GET` | `/upload-batches/{batchId}` | 200 | 批量任务 |
| `GET` | `/upload-dashboard` | 200 | 上传看板 |

### 3.1 上传请求

```
POST /api/v1/knowledge-bases/{kbId}/documents
Content-Type: multipart/form-data
```

| 参数 | 必填 | 说明 |
|------|------|------|
| `file` | 与 `files` 二选一 | 单文件 |
| `files` | 与 `file` 二选一 | 多文件 |
| `smartChunk` | 否 | 默认 `false`；`true` 且 `aiMode≠never` 时强制 T8 |
| `sourceType` | 否 | 单文件默认 `API_SINGLE`；多文件默认 `LOCAL_BATCH` |

`sourceType` 可选：`API_SINGLE`、`LOCAL_BATCH`、`OSS_SERVER_BATCH`（后者当前仅标识来源，文件仍经 multipart）。

### 3.2 上传响应（202）

见 [api-samples/document-upload-response-single.json](api-samples/document-upload-response-single.json)、[document-upload-response-batch.json](api-samples/document-upload-response-batch.json)。

### 3.3 轮询文档详情（200）

建库完成后见 [api-samples/document-response-success.json](api-samples/document-response-success.json)；处理中见 [document-response-processing.json](api-samples/document-response-processing.json)。

---

## 4. 核心类（研发）

| 类 | 职责 |
|----|------|
| `DocumentController` | 统一上传入口、retrain |
| `DocumentAsyncUploadService` | 单/批路由、入队 |
| `DocumentStreamArchiveService` | 请求线程流式归档 |
| `DocumentIngestExecutor` | 异步建库 |
| `DocumentIngestJob` | 任务载荷（无 `byte[]`） |
| `ObjectStorageService` / `LocalObjectStorageService` | `putStream` / `openStream` |
| `ContentHashUtil` | 流式复制 + SHA-256 |
| `ChunkIngestPipeline` | `ingestFromStorage` |
| `DocumentProcessTracker` | 阶段与进度 |
| `UploadBatchController` / `UploadDashboardService` | 批次与看板 |

---

## 5. 配置

`application-optional.yaml`：

```yaml
ragchunk:
  upload:
    async-enabled: true
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 500
  oss:
    provider: local
    local-root: ./data/oss-archive
    public-base-url: http://127.0.0.1:9000/ragchunk
```

---

## 6. 联调示例

```powershell
# 1. 上传（202，此时已完成流式归档）
$form = @{ file = Get-Item ".\scripts\sample.md" }
$upload = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/knowledge-bases/$kbId/documents?smartChunk=false" -Form $form
$docId = $upload.docId

# 2. 轮询直到 SUCCESS
do {
  Start-Sleep -Seconds 2
  $doc = Invoke-RestMethod "http://localhost:8080/api/v1/knowledge-bases/$kbId/documents/$docId"
  Write-Host "status=$($doc.status) stage=$($doc.processStage) progress=$($doc.progressPercent)%"
} while ($doc.status -notin @("SUCCESS", "FAILED"))

if ($doc.status -eq "FAILED") { throw $doc.errorMessage }

# 3. 问答
$chatBody = '{"question":"样例文档讲了什么？"}'
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/knowledge-bases/$kbId/chat" `
  -ContentType "application/json; charset=utf-8" `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($chatBody))
```

---

## 7. 数据库（Flyway V3）

| 对象 | 说明 |
|------|------|
| `upload_batch` | 批量任务计数 |
| `document` 扩展 | `batch_id`, `process_stage`, `progress_percent`, `storage_key`, `storage_url`, `content_hash`, `source_type`, `retrain_version` 等 |

---

## 8. 未实现 · 未测试（上传相关）

| 项 | 说明 |
|----|------|
| S3/MinIO SDK | `provider=s3` 未实现 |
| OSS 服务端拉取 | `OSS_SERVER_BATCH` 仅元数据标识 |
| 超大 txt 分块解析 | txt/md 仍可能一次性读入内存 |
| Webhook / SSE 进度推送 | 仅 HTTP 轮询 |

已实现 · 已测试：202 受理、流式归档、异步入库、`processStage` 轮询、看板与 retrain（见 [开发进度.md](开发进度.md) §3.1）。

---

## 9. 修订记录

| 日期 | 说明 |
|------|------|
| 2026-05-22 | 统一异步上传 + 流式归档 + 两阶段流程说明 |
