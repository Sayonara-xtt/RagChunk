-- 在 PostgreSQL 超级用户下执行，创建 RagChunk 所需库与用户
-- 示例: psql -U postgres -f scripts/init-postgres.sql

CREATE USER ragchunk WITH PASSWORD 'ragchunk';
CREATE DATABASE ragchunk OWNER ragchunk ENCODING 'UTF8';
\connect ragchunk
CREATE EXTENSION IF NOT EXISTS vector;
GRANT ALL PRIVILEGES ON DATABASE ragchunk TO ragchunk;
