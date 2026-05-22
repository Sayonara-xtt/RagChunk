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
}
