package iuh.fit.nhom16.pcstore.ai;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Set;
import java.util.List;

/** All inference goes through Spring AI's ChatClient and official Google GenAI ChatModel. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiSettings.class)
public class AiGateway implements DisposableBean {
    public record Source(String title, String url) {}
    public record Grounded<T>(T value, List<Source> sources) {}
    private final AiSettings settings;
    private final AiLimiter limiter;
    private final JsonMapper json;
    private final Client provider;
    private final ChatClient chat;

    public AiGateway(AiSettings settings, AiLimiter limiter, JsonMapper json) {
        this.settings = settings;
        this.limiter = limiter;
        this.json = json;
        URI uri = URI.create(settings.baseUrl());
        boolean google = "https".equals(uri.getScheme()) && "generativelanguage.googleapis.com".equals(uri.getHost());
        boolean local = settings.allowLocalMocks() && "http".equals(uri.getScheme())
                && Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
        if ((!google && !local) || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Invalid Gemini base URL");
        if (settings.timeoutMs() < 100 || settings.timeoutMs() > 25000 || !settings.model().matches("[a-z0-9][a-z0-9.-]{0,99}")
                || settings.perMinute() < 1 || settings.perDay() < 1 || settings.globalPerDay() < 1)
            throw new IllegalArgumentException("Invalid AI settings");
        if (settings.configured()) {
            // One attempt: avoid hidden retries multiplying free-tier usage and latency.
            provider = Client.builder().apiKey(settings.apiKey().trim()).httpOptions(HttpOptions.builder()
                    .baseUrl(settings.baseUrl().replaceFirst("/v1beta/?$", "")).apiVersion("v1beta").timeout(settings.timeoutMs())
                    .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build();
            var model = new SourcedGoogleChatModel(provider,GoogleGenAiChatOptions.builder().model(settings.model()).temperature(0.0)
                            .maxOutputTokens(2400).responseMimeType("application/json").build());
            chat = ChatClient.create(model);
        } else { provider = null; chat = null; }
    }

    public <T> T ask(String system, Object context, Class<T> type, String client) {
        return invoke(system,context,type,client,false).value();
    }
    public <T> Grounded<T> askGrounded(String system, Object context, Class<T> type, String client) {
        return invoke(system,context,type,client,true);
    }
    @SuppressWarnings("unchecked")
    private <T> Grounded<T> invoke(String system, Object context, Class<T> type, String client, boolean grounded) {
        if (chat == null) throw new AiFailure(503, "AI_NOT_CONFIGURED", "Trợ lý AI tạm thời chưa sẵn sàng.");
        try (var lease = limiter.acquire(client)) {
            var converter = new BeanOutputConverter<>(type);
            String schema = converter.getJsonSchema();
            if (type == ProductCriteria.class) {
                ObjectNode root = (ObjectNode) json.readTree(schema);
                for (String field : Set.of("purpose", "minBudget", "maxBudget")) {
                    ObjectNode property = (ObjectNode) root.get("properties").get(field);
                    String propertyType = property.remove("type").asText();
                    property.set("anyOf", json.valueToTree(java.util.List.of(
                            java.util.Map.of("type", propertyType), java.util.Map.of("type", "null"))));
                }
                schema = root.toString();
            }
            var response = chat.prompt().system(system).user(json.writeValueAsString(context))
                    .options(GoogleGenAiChatOptions.builder().responseSchema(schema).googleSearchRetrieval(grounded))
                    .call().chatResponse();
            if (response == null || response.getResult() == null) throw new IllegalArgumentException("Empty AI response");
            String output = response.getResult().getOutput().getText();
            if (output == null || output.length() > 100000) throw new IllegalArgumentException("Invalid AI response");
            T value = json.readerFor(type).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(output);
            List<Source> sources = (List<Source>)response.getResult().getOutput().getMetadata().getOrDefault("groundingSources",List.of());
            if (grounded && sources.isEmpty()) throw new IllegalArgumentException("No provider grounding sources");
            return new Grounded<>(value,sources);
        } catch (AiFailure error) { throw error;
        } catch (Exception error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof ApiException api) {
                    if (api.code() == 429) {
                        limiter.cooldown(60);
                        throw new AiFailure(429, "AI_QUOTA_EXCEEDED", "AI đã đạt giới hạn lượt sử dụng. Vui lòng thử lại sau.", 60);
                    }
                    if (Set.of(400,401,403,404).contains(api.code()))
                        throw new AiFailure(503, "AI_CONFIGURATION_ERROR", "Trợ lý AI chưa thể kết nối. Bạn vui lòng thử lại sau.");
                    throw new AiFailure(502, "AI_UPSTREAM_ERROR", "Dịch vụ AI đang gián đoạn. Bạn vui lòng thử lại sau.");
                }
                if (cause instanceof java.io.InterruptedIOException || cause instanceof java.net.http.HttpTimeoutException
                        || cause instanceof java.util.concurrent.TimeoutException)
                    throw new AiFailure(504, "AI_TIMEOUT", "Trợ lý cần thêm thời gian phản hồi. Bạn vui lòng thử lại.");
            }
            // Never expose provider messages, prompts or credentials.
            AiFailure failure = new AiFailure(502, "AI_INVALID_RESPONSE", "Chưa xác minh được câu trả lời của AI. Bạn vui lòng thử lại.");
            failure.initCause(error);
            throw failure;
        }
    }
    public boolean configured() { return settings.configured(); }
    public static boolean safeSource(String raw) {
        try { URI uri = URI.create(raw); return raw.length() <= 2000 && "https".equals(uri.getScheme())
                && uri.getHost() != null && uri.getUserInfo() == null && !uri.getHost().equals("localhost")
                && !uri.getHost().matches("[0-9.]+") && !uri.getHost().contains(":"); }
        catch (IllegalArgumentException e) { return false; }
    }
    public String model() { return settings.model(); }
    @Override public void destroy() { if (provider != null) provider.close(); }
}
