# Java 后台包与资源目录

> 公用分层约定。与 `.cursor/rules/java-architecture.mdc` 一致；以本仓当前代码树为准。

## 分层依赖

```text
controller → service → mapper → entity/DB
                ↓
         dto / vo / util
                ↓
    ingest / embedding / vector / objectstorage / integration
```

依赖只向下；跨业务走对方 `service`。**禁止新增** `store/`、`persistence/`、`web/`、`model/`（存取接口放在对应 `service/<域>/` 内）。

## Java 包目录

根包：`com.xtsh.ragchunk`

```text
src/main/java/com/xtsh/ragchunk/
├── RagChunkApplication.java          # 启动类
├── controller/                       # REST 入口（按业务域分子包）
│   ├── chat/
│   ├── chunk/
│   ├── document/
│   ├── knowledge/
│   ├── DocPageRedirectController.java
│   └── RuntimeConfigController.java
├── service/                          # 业务编排与事务（按域分子包）
│   ├── chat/
│   ├── chunk/                        # 含 *Store 接口与实现
│   ├── document/
│   └── knowledge/
├── mapper/                           # MyBatis Mapper、数据库投影
│   └── typehandler/                  # JSONB / pgvector / real[] JDBC 类型边界
├── entity/                           # 表映射实体（*Entity）
├── dto/                              # 入参 / 内部传输（按域）
│   ├── chat/
│   ├── chunk/
│   ├── document/
│   ├── knowledge/
│   └── ApiErrorResponse.java         # 跨域公共 DTO
├── vo/                               # 出参 / API 响应（按域）
│   ├── chat/
│   ├── chunk/
│   ├── document/
│   └── knowledge/
├── config/                           # Spring / Flyway / OpenAPI / 属性绑定
├── exception/                        # 业务异常 + @RestControllerAdvice
├── util/                             # 无状态工具
├── ingest/                           # 解析、切片、入库流水线（可选域包）
├── embedding/                        # 向量化抽象与实现
├── vector/                           # 向量库抽象与实现
├── objectstorage/                    # 对象存储（本地/OSS）
└── integration/                      # 外部 HTTP/SDK
    └── dashscope/
```

### 各包职责

| 包 | 负责 | 禁止 / 注意 |
|---|---|---|
| `controller` | 路由、`@Valid`、OpenAPI、返回 VO | 业务逻辑、事务、直调 Mapper |
| `service` | 编排、事务、DTO↔VO、*Store | Servlet API；跨业务直调对方 Mapper |
| `mapper` | `BaseMapper` / XML | 业务规则 |
| `entity` | `@TableName` 表映射 | 直接当 API 响应 |
| `dto` | 请求体、内部传输、校验注解 | 写库用 VO |
| `vo` | 对外响应字段裁剪 | 写库；敏感字段需脱敏/`@JsonIgnore` |
| `config` | Bean、配置属性、基础设施 | 业务编排 |
| `exception` | 统一错误体与 HTTP 映射 | — |
| `util` | 纯函数工具 | 持有 Spring 状态 |
| `ingest` 等可选包 | 领域技术能力，供 service 调用 | 反向依赖 controller |

### 业务域约定

| 域 | controller | service | dto / vo |
|---|---|---|---|
| 知识库 | `controller.knowledge` | `service.knowledge` | `dto/vo.knowledge` |
| 文档上传 | `controller.document` | `service.document` | `dto/vo.document` |
| 切片查询 | `controller.chunk` | `service.chunk` | `dto/vo.chunk` |
| 智能问答 | `controller.chat` | `service.chat` | `dto/vo.chat` |

新增业务域时：同步增加 `controller/<域>`、`service/<域>`、`dto/<域>`、`vo/<域>`；表实体进 `entity/`，Mapper 进 `mapper/`（扁平）。

## 资源目录

```text
src/main/resources/
├── application.yaml                  # 主配置
├── application-local.yaml            # 本地 profile
├── application-docker.yaml           # Docker profile
├── application-inmemory.yaml         # 内存存储 profile
├── application-array.yaml            # JDBC array 向量 profile
├── application-ollama.yaml           # Ollama profile
├── application-optional.yaml         # 可选覆盖
├── mapper/
│   └── xml/                          # MyBatis XML（namespace 对齐 Mapper 接口）
│       └── VectorChunkMapper.xml     # 向量写入、删除与检索 SQL
├── db/
│   ├── migration/                    # 默认 Flyway 脚本（只增不改已发布）
│   │   ├── V1__init_schema.sql
│   │   ├── V2__table_comments.sql
│   │   └── V3__async_upload_oss.sql
│   └── migration-local/              # local profile 专用迁移
│       ├── V1__init_schema.sql
│       ├── V2__table_comments.sql
│       └── V3__async_upload_oss.sql
├── static/                           # 静态资源（按需）
└── META-INF/
```

| 路径 | 说明 |
|---|---|
| `application*.yaml` | 密钥用 `${ENV:}`，禁止硬编码 |
| `mapper/xml/` | 复杂及数据库方言 SQL；`namespace` = Mapper 全限定名 |
| `db/migration/` | 生产/默认库结构；**已发布脚本只增不改** |
| `db/migration-local/` | 本地开发库；由 `FlywayLocalConfig` 等切换 |
| `static/` | 前端静态或文档页资源（若有） |

## 命名速查

| 类型 | 命名 | 示例 |
|---|---|---|
| Controller | `*Controller` | `DocumentController` |
| Service | `*Service` / 编排类 | `DocumentService`、`ChatOrchestrator` |
| 存取抽象 | `*Store`（放 service 子包） | `DocumentStore` |
| Mapper | `*Mapper` | `DocumentMapper` |
| Entity | `*Entity` | `DocumentEntity` |
| 请求 DTO | `*Request` / 领域名 | `CreateKnowledgeBaseRequest`、`ChatRequest` |
| 响应 VO | `*Response` | `DocumentResponse` |
| 配置 | `*Config` / `*Properties` | `AsyncUploadConfig`、`RagChunkProperties` |

## 放置决策（新增类时）

1. 有 HTTP 映射？→ `controller/<域>`
2. 有事务/业务编排？→ `service/<域>`
3. 仅表字段映射？→ `entity`
4. 仅入参/校验？→ `dto/<域>`；仅出参？→ `vo/<域>`
5. 跨域错误结构？→ `dto` 根或 `exception`
6. 解析/切片/向量/OSS/外部 SDK？→ 对应可选包，**不要**塞进 controller
7. 无状态字符串/哈希等？→ `util`

## 与旧结构对照（迁移完成态）

| 旧位置（已废弃） | 新位置 |
|---|---|
| `*/dto`、`*/model` 散落各域 | 统一 `dto/`、`vo/`、`entity/` |
| `persistence/`、顶层 `*Store` 包 | `mapper/` + `service/<域>/*Store` |
| 域根包下 `*Controller` | `controller/<域>/` |
