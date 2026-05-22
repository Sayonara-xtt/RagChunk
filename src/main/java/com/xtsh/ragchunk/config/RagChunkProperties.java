package com.xtsh.ragchunk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ragchunk")
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
    private Upload upload = new Upload();
    private Oss oss = new Oss();

    public int getPhase() { return phase; }
    public void setPhase(int phase) { this.phase = phase; }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
    public Dashscope getDashscope() { return dashscope; }
    public void setDashscope(Dashscope dashscope) { this.dashscope = dashscope; }
    public Chunking getChunking() { return chunking; }
    public void setChunking(Chunking chunking) { this.chunking = chunking; }
    public Rule getRule() { return rule; }
    public void setRule(Rule rule) { this.rule = rule; }
    public Quality getQuality() { return quality; }
    public void setQuality(Quality quality) { this.quality = quality; }
    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
    public Retrieval getRetrieval() { return retrieval; }
    public void setRetrieval(Retrieval retrieval) { this.retrieval = retrieval; }
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    public Qa getQa() { return qa; }
    public void setQa(Qa qa) { this.qa = qa; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public Oss getOss() { return oss; }
    public void setOss(Oss oss) { this.oss = oss; }

    public static class Storage {
        /** postgres：PostgreSQL 持久化；inmemory：内存，重启丢失 */
        private String mode = "postgres";
        /** pgvector：PostgreSQL 向量扩展（推荐）；array：本机 PG real[] 回退 */
        private String vectorStore = "pgvector";
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getVectorStore() { return vectorStore; }
        public void setVectorStore(String vectorStore) { this.vectorStore = vectorStore; }
    }

    public static class Dashscope {
        /** dashscope：阿里云；ollama：本地 OpenAI 兼容接口（/v1/chat/completions） */
        private String provider = "dashscope";
        private String apiKey = "";
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String embeddingUrl = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getEmbeddingUrl() { return embeddingUrl; }
        public void setEmbeddingUrl(String embeddingUrl) { this.embeddingUrl = embeddingUrl; }
        public boolean isOllama() { return "ollama".equalsIgnoreCase(provider); }
        /** 千问切片 / RAG 问答是否可走远程 LLM */
        public boolean isLlmConfigured() {
            if (isOllama()) {
                return baseUrl != null && !baseUrl.isBlank();
            }
            return apiKey != null && !apiKey.isBlank();
        }
        /** @deprecated 使用 {@link #isLlmConfigured()} */
        public boolean isConfigured() { return isLlmConfigured(); }
    }

    public static class Chunking {
        private String mode = "hybrid";
        private String aiMode = "auto";
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getAiMode() { return aiMode; }
        public void setAiMode(String aiMode) { this.aiMode = aiMode; }
    }

    public static class Rule {
        private int maxChars = 1200;
        private int minChars = 80;
        private int overlap = 80;
        private List<String> plainSeparators = List.of("\n\n", "\n");
        private List<String> markdownSeparators = List.of("\n## ", "\n\n", "\n");
        private boolean sentenceBoundaryFallback = true;
        public int getMaxChars() { return maxChars; }
        public void setMaxChars(int maxChars) { this.maxChars = maxChars; }
        public int getMinChars() { return minChars; }
        public void setMinChars(int minChars) { this.minChars = minChars; }
        public int getOverlap() { return overlap; }
        public void setOverlap(int overlap) { this.overlap = overlap; }
        public List<String> getPlainSeparators() { return plainSeparators; }
        public void setPlainSeparators(List<String> plainSeparators) { this.plainSeparators = plainSeparators; }
        public List<String> getMarkdownSeparators() { return markdownSeparators; }
        public void setMarkdownSeparators(List<String> markdownSeparators) { this.markdownSeparators = markdownSeparators; }
        public boolean isSentenceBoundaryFallback() { return sentenceBoundaryFallback; }
        public void setSentenceBoundaryFallback(boolean sentenceBoundaryFallback) { this.sentenceBoundaryFallback = sentenceBoundaryFallback; }
    }

    public static class Quality {
        private int scoreThreshold = 70;
        public int getScoreThreshold() { return scoreThreshold; }
        public void setScoreThreshold(int scoreThreshold) { this.scoreThreshold = scoreThreshold; }
    }

    public static class Ai {
        private String chunkModel = "qwen-plus";
        private int maxCallsPerDoc = 1;
        private int maxInputTokens = 8000;
        private int retryOnParseError = 1;
        public String getChunkModel() { return chunkModel; }
        public void setChunkModel(String chunkModel) { this.chunkModel = chunkModel; }
        public int getMaxCallsPerDoc() { return maxCallsPerDoc; }
        public void setMaxCallsPerDoc(int maxCallsPerDoc) { this.maxCallsPerDoc = maxCallsPerDoc; }
        public int getMaxInputTokens() { return maxInputTokens; }
        public void setMaxInputTokens(int maxInputTokens) { this.maxInputTokens = maxInputTokens; }
        public int getRetryOnParseError() { return retryOnParseError; }
        public void setRetryOnParseError(int retryOnParseError) { this.retryOnParseError = retryOnParseError; }
    }

    public static class Embedding {
        private String model = "text-embedding-v3";
        private int dimensions = 1024;
        /** false 时始终用本地 hash 向量（适合仅 Ollama 对话、未部署 embed 模型） */
        private boolean remoteEnabled = true;
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
        public boolean isRemoteEnabled() { return remoteEnabled; }
        public void setRemoteEnabled(boolean remoteEnabled) { this.remoteEnabled = remoteEnabled; }
    }

    public static class Retrieval {
        private int topK = 3;
        private double scoreThreshold = 0.5;
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getScoreThreshold() { return scoreThreshold; }
        public void setScoreThreshold(double scoreThreshold) { this.scoreThreshold = scoreThreshold; }
    }

    public static class Chat {
        private String model = "qwen-plus";
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    /** 智能问答默认方案（创建知识库未传 qa 时合并） */
    public static class Qa {
        private int scheme = 1;
        private double rewriteMinScore = 0.35;
        private int maxRewriteQueries = 2;
        private int maxLlmCalls = 2;
        private int maxSearchRounds = 2;
        private int agentMaxIterations = 3;
        private int agentMaxToolCallsPerRound = 1;
        private boolean agentAllowRelaxThreshold = true;

        public int getScheme() { return scheme; }
        public void setScheme(int scheme) { this.scheme = scheme; }
        public double getRewriteMinScore() { return rewriteMinScore; }
        public void setRewriteMinScore(double rewriteMinScore) { this.rewriteMinScore = rewriteMinScore; }
        public int getMaxRewriteQueries() { return maxRewriteQueries; }
        public void setMaxRewriteQueries(int maxRewriteQueries) { this.maxRewriteQueries = maxRewriteQueries; }
        public int getMaxLlmCalls() { return maxLlmCalls; }
        public void setMaxLlmCalls(int maxLlmCalls) { this.maxLlmCalls = maxLlmCalls; }
        public int getMaxSearchRounds() { return maxSearchRounds; }
        public void setMaxSearchRounds(int maxSearchRounds) { this.maxSearchRounds = maxSearchRounds; }
        public int getAgentMaxIterations() { return agentMaxIterations; }
        public void setAgentMaxIterations(int agentMaxIterations) { this.agentMaxIterations = agentMaxIterations; }
        public int getAgentMaxToolCallsPerRound() { return agentMaxToolCallsPerRound; }
        public void setAgentMaxToolCallsPerRound(int agentMaxToolCallsPerRound) {
            this.agentMaxToolCallsPerRound = agentMaxToolCallsPerRound;
        }
        public boolean isAgentAllowRelaxThreshold() { return agentAllowRelaxThreshold; }
        public void setAgentAllowRelaxThreshold(boolean agentAllowRelaxThreshold) {
            this.agentAllowRelaxThreshold = agentAllowRelaxThreshold;
        }
    }

    /** 异步上传线程池 */
    public static class Upload {
        private boolean asyncEnabled = true;
        private int corePoolSize = 4;
        private int maxPoolSize = 8;
        private int queueCapacity = 500;

        public boolean isAsyncEnabled() { return asyncEnabled; }
        public void setAsyncEnabled(boolean asyncEnabled) { this.asyncEnabled = asyncEnabled; }
        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    }

    /**
     * 文档原件 OSS 归档（默认本地目录模拟；endpoint 为展示用默认 MinIO 地址）。
     */
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

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getLocalRoot() { return localRoot; }
        public void setLocalRoot(String localRoot) { this.localRoot = localRoot; }
        public String getPublicBaseUrl() { return publicBaseUrl; }
        public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
        public boolean isLocal() { return provider == null || "local".equalsIgnoreCase(provider); }
    }
}
