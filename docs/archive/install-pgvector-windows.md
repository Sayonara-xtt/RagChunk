> **已归档**。请以 [开发进度.md](../开发进度.md) 与 [docs/README.md](../README.md) 为准。

# 本机 PostgreSQL 17 安装 pgvector（Windows）

## 一键脚本（推荐）

以 **管理员** 打开 **cmd** 或 PowerShell，在项目根目录执行：

```cmd
cd /d d:\code\RagChunk
scripts\install-pgvector-windows.cmd -PgRoot "D:\AnZhuang\PostgreSQL17" -SuperPassword "123"
```

若直接运行 `.ps1` 报「在此系统上禁止运行脚本」，任选其一：

- 使用上面的 **`.cmd`**（推荐，不改系统策略）
- 或：`powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-pgvector-windows.ps1 -PgRoot "D:\AnZhuang\PostgreSQL17" -SuperPassword "123"`
- 或（仅当前用户、长期生效）：`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

脚本会：

1. 检测安装目录（默认尝试 `D:\AnZhuang\PostgreSQL17`，可用 `-PgRoot` 指定）
2. 下载预编译包 [vector.v0.8.2-pg17.zip](https://github.com/andreiramani/pgvector_pgsql_windows/releases/download/0.8.2_17.6/vector.v0.8.2-pg17.zip)
3. 复制 `vector.dll` 到 `lib`，扩展 SQL 到 `share/extension`
4. 重启服务 `postgresql-x64-17`
5. 创建数据库 `ragchunk` 并执行 `CREATE EXTENSION vector`

默认超级用户密码与 `application-local.yaml` 一致：`postgres` / `123`（可用 `-SuperPassword` 修改）。

## 手动安装

1. 从 [pgvector_pgsql_windows Releases](https://github.com/andreiramani/pgvector_pgsql_windows/releases) 下载与 **PostgreSQL 17** 对应版本 zip  
2. 解压后将 `vector.dll` 复制到 `<PG安装目录>\lib\`  
3. 将 `vector.control`、`vector--*.sql` 复制到 `<PG安装目录>\share\extension\`  
4. 重启 PostgreSQL 服务  
5. 在 `ragchunk` 库执行：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
SELECT extversion FROM pg_extension WHERE extname = 'vector';
```

## 应用连接

| 项 | 值 |
|----|-----|
| URL | `jdbc:postgresql://localhost:5432/ragchunk` |
| 用户 | `postgres` |
| 密码 | `123`（环境变量 `RAGCHUNK_DB_PASSWORD` 可覆盖） |
| Profile | `local`（默认） |

```powershell
.\mvnw.cmd spring-boot:run
```

## 验证

```powershell
.\scripts\test-db-connection.ps1
# 需设置: $env:RAGCHUNK_DB_PORT="5432"; $env:RAGCHUNK_DB_USER="postgres"; $env:RAGCHUNK_DB_PASSWORD="123"
```

或：

```powershell
& "D:\AnZhuang\PostgreSQL17\bin\psql.exe" -U postgres -d ragchunk -c "SELECT extname, extversion FROM pg_extension WHERE extname='vector';"
```
