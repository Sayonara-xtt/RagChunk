package com.xtsh.ragchunk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ragchunk")
@Data
public class RagChunkProperties {

    private int phase = 1;
    private Storage storage = new Storage();
    private Dashscope dashscope = new Dashscope();
    private Chunking chunking = new Chunking();
    private Rule rule = new Rule();
    private Quality quality = new Quality();
    private Ai ai = new Ai();
    private Embedding embedding = new Embedding();
    private Retrieval retrieval = new Retrieval();
    private Chat chat = new Chat();
    private Qa qa = new Qa();
    private Evaluation evaluation = new Evaluation();
    private Upload upload = new Upload();
    private Oss oss = new Oss();

    @Data
    public static class Storage {
        /** postgres：PostgreSQL 持久化；inmemory：内存，重启丢失 */
        private String mode = "postgres";
        /** pgvector：PostgreSQL 向量扩展（推荐）；array：本机 PG real[] 回退 */
        private String vectorStore = "pgvector";
    }    @Data
    public static class Dashscope {
        /** dashscope：阿里云；ollama：本地 OpenAI 兼容接口（/v1/chat/completions） */
        private String provider = "dashscope";
        private String apiKey = "";
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String embeddingUrl = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

        public boolean isOllama() {
            return "ollama".equalsIgnoreCase(provider);
        }

        /** 千问切片 / RAG 问答是否可走远程 LLM */
        public boolean isLlmConfigured() {
            if (isOllama()) {
                return baseUrl != null && !baseUrl.isBlank();
            }
            return apiKey != null && !apiKey.isBlank();
        }

        /** @deprecated 使用 {@link #isLlmConfigured()} */
        public boolean isConfigured() {
            return isLlmConfigured();
        }
    }

    @Data
    public static class Chunking {
        private String mode = "hybrid";
        private String aiMode = "auto";
    }

    @Data
    public static class Rule {
        private int maxChars = 1200;
        private int minChars = 80;
        private int overlap = 80;
        private List<String> plainSeparators = List.of("\n\n", "\n");
        private List<String> markdownSeparators = List.of("\n## ", "\n\n", "\n");
        private boolean sentenceBoundaryFallback = true;
    }

    @Data
    public static class Quality {
        private int scoreThreshold = 70;
    }

    @Data
    public static class Ai {
        private String chunkModel = "qwen-plus";
        private int maxCallsPerDoc = 1;
        private int maxInputTokens = 8000;
        private int retryOnParseError = 1;
    }

    @Data
    public static class Embedding {
        private String model = "text-embedding-v3";
        private int dimensions = 1024;
        /** false 时始终用本地 hash 向量（适合仅 Ollama 对话、未部署 embed 模型） */
        private boolean remoteEnabled = true;
    }

    @Data
    public static class Retrieval {
        private int topK = 3;
        private double scoreThreshold = 0.5;
    }

    @Data
    public static class Chat {
        private String model = "qwen-plus";
    }

    /** 智能问答默认方案（创建知识库未传 qa 时合并） */
    @Data
    public static class Qa {
        private int scheme = 1;
        private double rewriteMinScore = 0.35;
        private int maxRewriteQueries = 2;
        private int maxLlmCalls = 2;
        private int maxSearchRounds = 2;
        private int agentMaxIterations = 3;
        private int agentMaxToolCallsPerRound = 1;
        private boolean agentAllowRelaxThreshold = true;

    }

    @Data
    public static class Evaluation {
        private int corePoolSize = 1;
        private int maxPoolSize = 2;
        private int queueCapacity = 100;
        private int maxCasesPerRun = 500;
        private int shutdownWaitSeconds = 30;
        private boolean judgeEnabled = true;
        private String judgeModel = "";
        private String judgePromptVersion = "eval-judge-v1";
    }

    /** 异步上传线程池 */
    @Data
    public static class Upload {
        private boolean asyncEnabled = true;
        private int corePoolSize = 4;
        private int maxPoolSize = 8;
        private int queueCapacity = 500;

    }

    /**
     * 文档原件 OSS 归档（默认本地目录模拟；endpoint 为展示用默认 MinIO 地址）。
     */
    @Data
    public static class Oss {
        /** local：本地目录；s3：S3 兼容（MinIO） */
        private String provider = "local";
        private String endpoint = "http://127.0.0.1:9000";
        private String bucket = "ragchunk";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String region = "us-east-1";
        /** 本地归档根目录（provider=local） */
        private String localRoot = "./data/oss-archive";
        /** 对外展示 URL 前缀，如 http://127.0.0.1:9000/ragchunk */
        private String publicBaseUrl = "http://127.0.0.1:9000/ragchunk";


        public boolean isLocal() { return provider == null || "local".equalsIgnoreCase(provider); }
        }
}
