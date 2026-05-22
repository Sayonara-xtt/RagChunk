> **已归档**。主文档见 [README.md](../../README.md)。

# 使用本地 Ollama（qwen2.5:14b）

RagChunk 通过 **OpenAI 兼容接口** 调用 Ollama：`/v1/chat/completions`（切片、问答）。  
向量可选：Ollama `/v1/embeddings`，或关闭远程向量、使用本地 hash（仅开发/联调）。

## 1. 前置

在 `192.168.14.57` 上已安装 Ollama 并拉取模型：

```bash
ollama pull qwen2.5:14b
# 若要用 Ollama 做向量（需与库维度 1024 一致，见下文）
# ollama pull bge-m3
```

确认本机可访问：

```bash
curl http://192.168.14.57:11434/api/tags
```

## 2. 推荐配置（profile）

项目已提供 `application-ollama.yaml`，默认地址与模型如下：

| 项 | 默认值 |
|----|--------|
| 地址 | `http://192.168.14.57:11434/v1` |
| 对话/切片模型 | `qwen2.5:14b` |
| 远程向量 | **关闭**（`embedding.remote-enabled=false`） |

**启动：**

```powershell
cd d:\code\RagChunk
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,ollama"
```

或环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE = "local,ollama"
$env:OLLAMA_BASE_URL = "http://192.168.14.57:11434/v1"
$env:OLLAMA_CHAT_MODEL = "qwen2.5:14b"
.\mvnw.cmd spring-boot:run
```

## 3. 在 application.yaml 中手写（不用 profile）

```yaml
ragchunk:
  dashscope:
    provider: ollama
    api-key: ollama          # Ollama 一般不校验，填任意非空即可
    base-url: http://192.168.14.57:11434/v1
  ai:
    chunk-model: qwen2.5:14b
  chat:
    model: qwen2.5:14b
  embedding:
    remote-enabled: false    # 仅用 Ollama 对话，向量用本地 hash
```

## 4. 向量（Embedding）说明

| 模式 | 配置 | 说明 |
|------|------|------|
| **仅 Ollama 对话**（推荐先试） | `embedding.remote-enabled: false` | 上传/问答可跑；向量为本地 hash，检索语义较弱 |
| **Ollama 向量** | `remote-enabled: true` + `embedding.model` 为 Ollama 中已 pull 的 embed 模型 | 向量维度须与库 **1024** 一致，否则入库报错 |

启用 Ollama 向量示例：

```yaml
ragchunk:
  embedding:
    remote-enabled: true
    model: bge-m3    # 须 ollama pull，且输出维度=1024
    dimensions: 1024
```

若 embed 模型维度不是 1024，需改 Flyway 中 `vector(1024)` 并重建库，或继续用 `remote-enabled: false`。

## 5. 创建知识库时的模型名

创建库时会把当前默认 **快照** 进 `config_json`。使用 Ollama 后新建库即可；旧库仍可能是 `qwen-plus`，仅影响未覆盖字段。

上传时 `smartChunk=true` 会调用 **Ollama** 的 `qwen2.5:14b` 做语义重切；问答接口同样走该模型。

## 6. 故障排查

| 现象 | 处理 |
|------|------|
| 连接超时 | 检查防火墙、`192.168.14.57:11434` 从运行 RagChunk 的机器是否可达 |
| model not found | 在 Ollama 主机执行 `ollama pull qwen2.5:14b` |
| chat 404 | `base-url` 须带 `/v1`，例如 `http://192.168.14.57:11434/v1` |
| 仍走 DashScope | 确认 `provider: ollama` 且 profile `ollama` 已激活 |
| 切片未调 AI | 库配置 `chunking.aiMode=never` 或未设 `smartChunk=true` |
