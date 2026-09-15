# RagChunk 文档索引

> 更新日期：2026-09-05 · 技术说明以 **本目录** 为准。

## 对外必读

| 文档 | 说明 |
|------|------|
| [生产级演化方案.md](architecture/生产级演化方案.md) | 版本路线图、需求编号、完成门禁与暂停规则 |
| [开发进度.md](project/开发进度.md) | **功能状态**（已实现·已测试 / 未实现·未测试）、API、联调与 Scalar |
| [创建知识库接口参数.md](api/创建知识库接口参数.md) | `POST /knowledge-bases` 全量参数 |
| [智能问答方案.md](features/智能问答方案.md) | 问答编排（纯应用流水线 / 协作渐进 / 协作全量 / Agent） |
| [异步上传与OSS.md](features/异步上传与OSS.md) | 异步/批量上传、OSS 归档、流程阶段、看板 |
| [Java包与资源目录.md](architecture/Java包与资源目录.md) | 公用 Java 包结构、分层职责、resources 目录 |
| [TDD工作流.md](tdd/TDD工作流.md) | AI 辅助 TDD 微循环、测试分层、命令、门禁与完成定义 |
| [版本文档与审核规则](versions/README.md) | 需求、设计、测试两道人工门禁与标准模板入口 |
| [V0.9 版本总览](versions/V0.9/README.md) | 问答追踪与质量评测的已批准设计、测试和验收入口 |

## API 样例（`api/samples/`）

| 文件 | 说明 |
|------|------|
| [create-knowledge-base-request-full.json](api/samples/create-knowledge-base-request-full.json) | 创建库完整请求（含 `qa`） |
| [create-knowledge-base-request-scheme2.json](api/samples/create-knowledge-base-request-scheme2.json) | 协作渐进检索示例 |
| [chat-request-sample.json](api/samples/chat-request-sample.json) | 问答请求 |
| [chat-response-sample.json](api/samples/chat-response-sample.json) | 问答响应（含 `meta`） |
| [knowledge-base-response.json](api/samples/knowledge-base-response.json) | 创建库响应 |
| [chunk-list-sample.json](api/samples/chunk-list-sample.json) | 切片列表 |
| [document-upload-response-single.json](api/samples/document-upload-response-single.json) | 上传受理 202（单文件） |
| [document-upload-response-batch.json](api/samples/document-upload-response-batch.json) | 上传受理 202（批量） |
| [document-response-processing.json](api/samples/document-response-processing.json) | 轮询：处理中 |
| [document-response-success.json](api/samples/document-response-success.json) | 轮询：建库成功 |

## 文档分区

| 目录 | 用途 | 维护要求 |
|------|------|----------|
| `docs/architecture/` | 当前架构说明与生产级演化路线 | 架构决策或版本范围变化时更新 |
| `docs/features/` | 上传、问答等业务能力说明 | 可观察业务行为变化时更新 |
| `docs/api/` | API 参数、契约和请求/响应样例 | API 契约变化时同步更新 |
| `docs/project/` | 当前开发状态、联调结论和风险 | 每次版本验收后更新 |
| `docs/tdd/` | TDD 工作项、已知基线与本地证据目录 | 每个正式业务切片遵循 |
| `docs/versions/` | 每版需求单、详细设计、测试用例和验收报告 | 使用 `_template/`；门禁批准前不得编码，版本完成后等待指令 |
| `docs/archive/` | 合并后的历史决策摘要 | 不作为当前实现依据；只保留仍有解释价值的背景 |

## 不进入版本库的本地文件

- `.idea/`：IDE 私有配置，可由开发工具重新生成。
- `.ai/`：本地 AI 工具配置。
- `target/`：Maven 构建输出。
- `data/oss-archive/`：本地上传产生的运行时对象存储数据。
- `scripts/tmp_*.java`：临时源码副本。

## 日志前缀

| 前缀 | 链路 |
|------|------|
| `[文档上传]` | 流式归档 → 解析 → 混合切片 → 向量化入库 |
| `[智能问答]` | 问答编排（`ChatOrchestrator`） |
