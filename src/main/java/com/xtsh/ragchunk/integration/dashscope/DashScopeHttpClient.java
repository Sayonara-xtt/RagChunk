package com.xtsh.ragchunk.integration.dashscope;



import lombok.RequiredArgsConstructor;
import com.xtsh.ragchunk.config.RagChunkProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 缁熶竴 LLM / Embedding HTTP 瀹㈡埛绔€傚垏鐗囦笌闂瓟鍧囪蛋 OpenAI 鍏煎 {@code /v1/chat/completions}銆? * <p>provider=ollama 鏃惰姹傛湰鍦?Ollama锛堟棤闇€ Authorization锛夛紱鍚﹀垯璧?DashScope銆? */
@RequiredArgsConstructor
@Component
public class DashScopeHttpClient {

    private final RagChunkProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    /** Ollama锛歜aseUrl 闈炵┖鍗冲彲锛汥ashScope锛氶渶 DASHSCOPE_API_KEY */
    public boolean isConfigured() {
        return properties.getDashscope().isLlmConfigured();
    }

    /**
     * 璇箟鍒囩墖涓?RAG 闂瓟鍏辩敤銆傛ā鍨嬪悕鏉ヨ嚜鐭ヨ瘑搴?config.ai.chunkModel 鎴?ragchunk.chat.model銆?     */
    public String chat(String model, String systemPrompt, String userContent) throws Exception {
        return postChat(model, systemPrompt, userContent, false);
    }

    /**
     * 璇箟鍒囩墖涓撶敤锛氶檷浣庨殢鏈烘€у苟璇锋眰 JSON 杈撳嚭锛圤llama {@code format=json} / DashScope response_format锛夈€?     */
    public String chatJson(String model, String systemPrompt, String userContent) throws Exception {
        return postChat(model, systemPrompt, userContent, true);
    }

    /**
     * Agent 闂瓟锛氭敮鎸?tools / tool_calls锛圤penAI 鍏煎锛夈€?     */
    public ChatCompletionResult chatWithTools(String model, List<Map<String, Object>> messages,
                                              List<Map<String, Object>> tools) throws Exception {
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("temperature", 0);
        return postChatBody(body);
    }

    private String postChat(String model, String systemPrompt, String userContent, boolean jsonMode) throws Exception {
        var messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        );
        var body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("messages", messages);
        if (jsonMode) {
            body.put("temperature", 0);
            if (properties.getDashscope().isOllama()) {
                body.put("format", "json");
            } else {
                body.put("response_format", Map.of("type", "json_object"));
            }
        }
        ChatCompletionResult result = postChatBody(body);
        String content = result.content();
        return content != null ? content : "";
    }

    private ChatCompletionResult postChatBody(LinkedHashMap<String, Object> body) throws Exception {
        var bodyJson = mapper.writeValueAsString(body);
        var ds = properties.getDashscope();
        var req = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBase(ds.getBaseUrl()) + "/chat/completions"))
                .header("Content-Type", "application/json");
        if (!ds.isOllama() && ds.getApiKey() != null && !ds.getApiKey().isBlank()) {
            req = req.header("Authorization", "Bearer " + ds.getApiKey());
        }
        req = req.POST(HttpRequest.BodyPublishers.ofString(bodyJson)).timeout(Duration.ofMinutes(5));
        String chatUrl = normalizeBase(ds.getBaseUrl()) + "/chat/completions";
        HttpResponse<String> resp;
        try {
            resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException(providerLabel() + " chat 杩炴帴澶辫触: " + chatUrl + " 鈥?"
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
        }
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException(providerLabel() + " chat error: " + resp.statusCode() + " " + resp.body());
        }
        return parseCompletion(mapper.readTree(resp.body()));
    }

    private ChatCompletionResult parseCompletion(JsonNode root) {
        JsonNode message = root.path("choices").path(0).path("message");
        String content = message.path("content").isMissingNode() || message.path("content").isNull()
                ? null
                : message.path("content").asText();
        JsonNode toolCallsNode = message.path("tool_calls");
        List<ChatCompletionResult.ToolCall> toolCalls = new ArrayList<>();
        if (toolCallsNode.isArray()) {
            for (JsonNode tc : toolCallsNode) {
                JsonNode fn = tc.path("function");
                toolCalls.add(new ChatCompletionResult.ToolCall(
                        tc.path("id").asText(""),
                        fn.path("name").asText(""),
                        fn.path("arguments").asText("{}")));
            }
        }
        return new ChatCompletionResult(content, toolCalls);
    }

    public float[] embed(String model, String text, int dimensions) throws Exception {
        var ds = properties.getDashscope();
        if (ds.isOllama()) {
            return embedOllama(model, text, dimensions);
        }
        return embedDashScope(model, text, dimensions);
    }

    private float[] embedDashScope(String model, String text, int dimensions) throws Exception {
        var input = Map.of("model", model, "input", Map.of("texts", List.of(text)), "parameters", Map.of("dimension", dimensions));
        var body = mapper.writeValueAsString(input);
        var ds = properties.getDashscope();
        var req = HttpRequest.newBuilder()
                .uri(URI.create(ds.getEmbeddingUrl()))
                .header("Authorization", "Bearer " + ds.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofMinutes(1))
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException("dashscope embed error: " + resp.statusCode() + " " + resp.body());
        }
        JsonNode arr = mapper.readTree(resp.body()).path("output").path("embeddings").path(0).path("embedding");
        return toVector(arr, dimensions);
    }

    private float[] embedOllama(String model, String text, int dimensions) throws Exception {
        var body = mapper.writeValueAsString(Map.of("model", model, "input", text));
        var url = normalizeBase(properties.getDashscope().getBaseUrl()) + "/embeddings";
        var req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofMinutes(1))
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IllegalStateException("ollama embed error: " + resp.statusCode() + " " + resp.body());
        }
        JsonNode arr = mapper.readTree(resp.body()).path("data").path(0).path("embedding");
        return toVector(arr, dimensions);
    }

    private static float[] toVector(JsonNode arr, int expectedDim) {
        float[] vec = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            vec[i] = (float) arr.get(i).asDouble();
        }
        if (expectedDim > 0 && vec.length != expectedDim) {
            throw new IllegalStateException("embedding dimension mismatch: got " + vec.length + ", expected " + expectedDim);
        }
        return vec;
    }

    private static String normalizeBase(String baseUrl) {
        if (baseUrl == null) return "";
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String providerLabel() {
        return properties.getDashscope().isOllama() ? "ollama" : "dashscope";
    }
}
