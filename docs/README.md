# RagChunk 文档索引

> 更新日期：2026-08-06 · 技术说明以 **本目录** 为准。

## 对外必读

| 文档 | 说明 |
|------|------|
| [开发进度.md](开发进度.md) | **功能状态**（已实现·已测试 / 未实现·未测试）、API、联调与 Scalar |
| [创建知识库接口参数.md](创建知识库接口参数.md) | `POST /knowledge-bases` 全量参数 |
| [智能问答方案.md](智能问答方案.md) | 问答编排（纯应用流水线 / 协作渐进 / 协作全量 / Agent） |
| [异步上传与OSS.md](异步上传与OSS.md) | 异步/批量上传、OSS 归档、流程阶段、看板 |
| [Java包与资源目录.md](Java包与资源目录.md) | 公用 Java 包结构、分层职责、resources 目录 |

## API 样例（`api-samples/`）

| 文件 | 说明 |
|------|------|
| [create-knowledge-base-request-full.json](api-samples/create-knowledge-base-request-full.json) | 创建库完整请求（含 `qa`） |
| [create-knowledge-base-request-scheme2.json](api-samples/create-knowledge-base-request-scheme2.json) | 协作渐进检索示例 |
| [chat-request-sample.json](api-samples/chat-request-sample.json) | 问答请求 |
| [chat-response-sample.json](api-samples/chat-response-sample.json) | 问答响应（含 `meta`） |
| [knowledge-base-response.json](api-samples/knowledge-base-response.json) | 创建库响应 |
| [chunk-list-sample.json](api-samples/chunk-list-sample.json) | 切片列表 |
| [document-upload-response-single.json](api-samples/document-upload-response-single.json) | 上传受理 202（单文件） |
| [document-upload-response-batch.json](api-samples/document-upload-response-batch.json) | 上传受理 202（批量） |
| [document-response-processing.json](api-samples/document-response-processing.json) | 轮询：处理中 |
| [document-response-success.json](api-samples/document-response-success.json) | 轮询：建库成功 |

## 日志前缀

| 前缀 | 链路 |
|------|------|
| `[文档上传]` | 流式归档 → 解析 → 混合切片 → 向量化入库 |
| `[智能问答]` | 问答编排（`ChatOrchestrator`） |
