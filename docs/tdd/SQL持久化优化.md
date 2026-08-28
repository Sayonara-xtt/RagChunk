# SQL 持久化优化 TDD 工作项

## 目标

将业务适配器中的数据库方言细节收敛到持久化边界：知识库 CRUD 使用 MyBatis-Plus 标准方法，向量 SQL 放入 MyBatis XML，Service、公开 API、配置和 Flyway 迁移保持不变。

## 切片 1：JSONB 参数绑定

- Given：知识库配置已序列化为 JSON 字符串；
- When：MyBatis 写入 `knowledge_base.config_json`；
- Then：参数以 PostgreSQL `jsonb` 类型绑定，并可按字符串读回。

不包含：修改 JSON 数据结构、迁移历史数据、调整知识库 API。

## 切片 2：向量 SQL 边界

- Given：pgvector 或 `real[]` 存储模式下的向量记录；
- When：执行写入、删除或检索；
- Then：向量存储适配器只负责编排与评分，SQL 和 JDBC 类型转换由专用 Mapper/XML 与 TypeHandler 负责。

不包含：改变相似度算法、TopK/阈值语义、表结构、索引或存储模式配置。

## 验证记录

| 阶段 | 命令 | 结果 | 证据文件 |
|---|---|---|---|
| RED 1 | `.\scripts\tdd-test.ps1 -Mode focus -Test JsonbStringTypeHandlerTest -Phase RED` | 因 `JsonbStringTypeHandler` 尚不存在而编译失败，RED 有效 | `20260827-180320-RED-focus-JsonbStringTypeHandlerTest.log` |
| GREEN 1 | `.\scripts\tdd-test.ps1 -Mode focus -Test JsonbStringTypeHandlerTest -Phase GREEN` | 2/2 通过 | `20260827-180504-GREEN-focus-JsonbStringTypeHandlerTest.log` |
| RED 2a | `.\scripts\tdd-test.ps1 -Mode focus -Test VectorTypeHandlerTest -Phase RED` | pgvector/real[] TypeHandler 尚不存在，RED 有效 | `20260827-180630-RED-focus-VectorTypeHandlerTest.log` |
| GREEN 2a | `.\scripts\tdd-test.ps1 -Mode focus -Test VectorTypeHandlerTest -Phase GREEN` | 2/2 通过 | `20260827-180736-GREEN-focus-VectorTypeHandlerTest.log` |
| RED 2b | `.\scripts\tdd-test.ps1 -Mode focus -Test VectorStorePersistenceBoundaryTest -Phase RED` | 专用 Mapper 与数据库投影尚不存在，RED 有效 | `20260827-180841-RED-focus-VectorStorePersistenceBoundaryTest.log` |
| GREEN 2b | `.\scripts\tdd-test.ps1 -Mode focus -Test VectorStorePersistenceBoundaryTest -Phase GREEN` | 2/2 通过 | `20260828-085832-GREEN-focus-VectorStorePersistenceBoundaryTest.log` |
| 单元回归 | `.\scripts\tdd-test.ps1 -Mode unit -Phase REGRESSION` | 最终目录结构下 30/30 通过 | `20260828-091500-REGRESSION-unit-suite.log` |
| 本机 PG 聚焦回归 | `RUN_DB_TEST=1` + `DatabaseConnectionTest#jsonbAndPgVectorMapperBoundaries` | JSONB、Mapper XML、1024 维 pgvector 写入与检索 1/1 通过，事务回滚 | `20260828-090528-REGRESSION-focus-DatabaseConnectionTest#jsonbAndPgVectorMapperBoundaries.log` |
| 可用集成回归 | `RUN_DB_TEST=1` + `.\scripts\tdd-test.ps1 -Mode integration -Phase REGRESSION` | 5 个测试：3 通过、2 个 Testcontainers 测试按条件跳过；覆盖 JSONB 新增/更新和向量 upsert/检索/删除 | `20260828-091724-REGRESSION-integration-suite.log` |

Docker Testcontainers 通道已尝试执行，因本机 Docker 未运行而在容器启动前失败，证据为 `20260828-090050-REGRESSION-integration-suite.log`。这不是代码断言失败；待 Docker 可用后仍需补跑该通道。

## 实施结果

- `KnowledgeBaseMapper` 不再维护手写增改 SQL，JSONB 方言由字段 TypeHandler 处理；
- `PgVectorStore`、`JdbcArrayVectorStore` 不再持有 `JdbcTemplate` 或内联 SQL；
- 向量 SQL 统一放在 `mapper/xml/VectorChunkMapper.xml`；
- pgvector 与 `real[]` 转换统一放在 `mapper/typehandler`；
- 修正 MyBatis Mapper 扫描包为 `com.xtsh.ragchunk.mapper`；
- API、Service 接口、数据库结构和 Flyway 迁移未改变。

## 剩余风险

- Docker 未运行，Testcontainers pgvector 通道本次未通过实跑；本机 PostgreSQL 等价边界已通过；
- `real[]` 模式完成 TypeHandler 与适配器单元验证，尚缺真实 `migration-local` 数据库集成回归；
- PostgreSQL 应用上下文仍存在既有 Jackson 2/3 兼容问题，测试使用专用 Bean 隔离，需独立工作项治理。
