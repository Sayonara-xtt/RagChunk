> **已归档**。请以 [开发进度.md](../开发进度.md) 与 [docs/README.md](../README.md) 为准。

# RagChunk 测试指南

## 一、单元测试（不启动服务）

### 前置

- JDK 17+
- Maven 3.9+（或在 IDEA 里直接跑测试）

### 命令

```powershell
cd d:\code\RagChunk
.\mvnw.cmd test
```

### 覆盖范围

| 测试类 | 验证内容 |
|--------|----------|
| `RagChunkApplicationTests` | Spring 上下文加载 |
| `KnowledgeBaseServiceTest` | 创建库、参数合并 |
| `RuleChunkerTest` | 规则切片 |
| `AiChunkTriggerTest` | T2/T8 触发逻辑 |
| `HybridChunkingServiceTest` | `aiMode=never` 时纯规则切片 |

### IDEA

右键 `src/test/java` → **Run 'All Tests'**，或单独运行某个 `*Test` 类。

---

## 二、本地联调（启动服务 + API）

### 1. 启动

```powershell
cd d:\code\RagChunk

# 启动 PostgreSQL（含 pgvector）
docker compose up -d

# 可选：配置通义千问
$env:DASHSCOPE_API_KEY = "sk-你的密钥"

.\mvnw.cmd spring-boot:run
```

默认使用 **PostgreSQL 持久化**（见 [database.md](database.md)）。无数据库时可用内存模式：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=inmemory"
```

默认端口：**8080**（以控制台日志为准）。

### 2. 两种模式

| 模式 | 环境变量 | 行为 |
|------|----------|------|
| **离线开发** | 不设置 `DASHSCOPE_API_KEY` | 本地 hash 向量；切片走规则；问答返回「检索到的原文」 |
| **完整链路** | 设置 `DASHSCOPE_API_KEY` | DashScope Embedding + 千问切片/问答 |

### 3. 一键脚本（推荐）

```powershell
cd d:\code\RagChunk
.\scripts\test-e2e.ps1
```

脚本会：创建库 → 上传 `scripts/sample.md` → 发起一次问答，并打印 `kbId` 与结果。

### 4. 手动 curl（PowerShell）

**创建知识库**

```powershell
$body = @{
  name = "测试库"
  chunking = @{ aiMode = "never" }   # 无 API Key 时建议 never，便于稳定测试
  retrieval = @{ topK = 3; scoreThreshold = 0.3 }
} | ConvertTo-Json -Depth 5

$kb = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/knowledge-bases" `
  -ContentType "application/json" -Body $body
$kbId = $kb.id
Write-Host "kbId = $kbId"
```

**上传文档（202 异步）**

```powershell
$file = "d:\code\RagChunk\scripts\sample.md"
$upload = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/knowledge-bases/$kbId/documents?smartChunk=false" `
  -Form @{ file = Get-Item $file }
$docId = $upload.docId

do {
  Start-Sleep -Seconds 2
  $doc = Invoke-RestMethod "http://localhost:8080/api/v1/knowledge-bases/$kbId/documents/$docId"
} while ($doc.status -notin @("SUCCESS", "FAILED"))
```

检查：`status` 应为 `SUCCESS`，`processStage=SUCCESS`，`chunkCount` > 0。详见 [异步上传与OSS.md](../异步上传与OSS.md)。

**智能问答四方案（1/2/3/5）一键测试**

```powershell
# 需服务已启动
.\scripts\test-qa-schemes.ps1
```

**问答（单方案）**

```powershell
$chatBody = '{"question":"RagChunk 一期离线建库有几步？","qaScheme":2}'
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/knowledge-bases/$kbId/chat" `
  -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($chatBody))
```

**列出文档**

```powershell
Invoke-RestMethod "http://localhost:8080/api/v1/knowledge-bases/$kbId/documents"
```

**查询切片**

```powershell
# 某文档下全部切片
$docId = (Invoke-RestMethod "http://localhost:8080/api/v1/knowledge-bases/$kbId/documents")[0].id
Invoke-RestMethod "http://localhost:8080/api/v1/knowledge-bases/$kbId/documents/$docId/chunks"

# 或：?docId= / ?id= 单个切片
Invoke-RestMethod "http://localhost:8080/api/v1/knowledge-bases/$kbId/chunks?docId=$docId"
```

---

## 三、验证清单

### 创建库

- [ ] 返回 `id` 以 `kb_` 开头
- [ ] `config` 中合并了请求参数与默认值

### 上传

- [ ] `POST .../documents` 返回 202，`docId` 非空
- [ ] 轮询后 `status`: `SUCCESS`，`processStage`: `SUCCESS`
- [ ] `storageKey`、`contentHash` 有值
- [ ] `chunkCount` ≥ 1
- [ ] `qualityScore` 0～100
- [ ] 无 Key 时 `aiTriggered` 通常为 false（`aiMode=never`）

### 问答

- [ ] 有相关内容时 `citations` 非空
- [ ] `answer` 与 sample 文档语义相关（有 Key 时为生成句；无 Key 时为检索片段）

### 千问切片（需 API Key）

创建库时 `"chunking": { "aiMode": "auto" }`，上传时 `smartChunk=true`，观察文档返回：

- [ ] `aiTriggered`: true
- [ ] `aiTriggerId`: T8 或 T2/T4
- [ ] `aiFallback`: false 表示千问结果通过校验

---

## 四、响应体中文乱码

**服务端**已配置 `server.servlet.encoding=UTF-8` 与 JSON UTF-8 转换器。

若 **终端里** 仍乱码，多半是 **显示编码** 问题（Windows 默认 GBK），不是接口真错了：

```powershell
# PowerShell 测试前执行
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# 或用 curl 写入文件再查看
curl.exe ... -o response.json
Get-Content response.json -Encoding utf8
```

IDEA HTTP Client、Postman、Apifox 一般显示正常。可用返回的 `id` 字段是否正常判断请求是否成功。

---

## 五、常见问题

| 现象 | 处理 |
|------|------|
| `mvn` 找不到 | 安装 Maven 或用 IDEA 自带 Maven 运行 |
| 端口占用 | 修改 `application.yaml` 增加 `server.port: 8081` |
| 上传 `FAILED` | 看返回 `errorMessage`；确认文件为 txt/md/docx |
| 问答无 citations | 降低 `retrieval.scoreThreshold`；或问题与文档用语不一致 |
| 千问 401/403 | 检查 `DASHSCOPE_API_KEY` 是否有效 |

---

## 五、接口文档（SpringDoc + Swagger UI）

1. 启动应用：`.\mvnw.cmd spring-boot:run` 或 IDEA 运行 `RagChunkApplication`
2. 打开任一入口：
   - **Scalar API 文档（推荐）**：http://localhost:8080/scalar
   - **兼容入口**：http://localhost:8080/doc.html（重定向到 Scalar）
   - **Swagger UI**：http://localhost:8080/swagger-ui/index.html
3. 右上角分组选 **RagChunk 一期 API**，按顺序调试：
   - **知识库** → `POST /api/v1/knowledge-bases` → 复制返回的 `id`
   - **文档** → `POST .../documents` → 202 受理后，用 `GET .../documents/{docId}` 轮询进度
   - **问答** → `POST .../chat` → 填 `kbId` 与 `question`

依赖：`springdoc-openapi-starter-webmvc-ui` + `springdoc-openapi-starter-webmvc-scalar` **3.0.3**（与 Spring Boot 4 匹配）。

### Knife4j「文档请求异常」说明

若使用 `knife4j-openapi3-jakarta-spring-boot-starter` **4.5.0** + 手动引入 SpringDoc **3.0.3**，访问 `/doc.html` 页面能打开，但拉取 OpenAPI 会 **HTTP 500**，典型错误：

```text
NoSuchMethodError: org.springdoc.core.properties.SpringDocConfigProperties.getGroupConfigs()
```

**原因**：Knife4j 4.5 的自动配置仍按 SpringDoc **2.x** API 编译，与 Spring Boot 4 所需的 SpringDoc **3.x** 二进制不兼容。  
**处理**：本项目已改为 SpringDoc 3 官方 UI；待官方 Knife4j 发布兼容 SpringDoc 3 的 starter 后再切回原生 `/doc.html`。

---

## 六、Postman / Apifox

导入以下集合要点：

1. `POST {{base}}/api/v1/knowledge-bases` — JSON body  
2. `POST {{base}}/api/v1/knowledge-bases/{{kbId}}/documents` — form-data，`file` 类型  
3. `POST {{base}}/api/v1/knowledge-bases/{{kbId}}/chat` — JSON `question`

`base` = `http://localhost:8080`
