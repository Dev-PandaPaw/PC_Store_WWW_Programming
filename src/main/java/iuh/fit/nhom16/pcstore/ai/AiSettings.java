package iuh.fit.nhom16.pcstore.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.ai")
public record AiSettings(
        @DefaultValue("") String apiKey,
        @DefaultValue("gemini-3.5-flash-lite") String model,
        @DefaultValue("https://generativelanguage.googleapis.com") String baseUrl,
        @DefaultValue("20000") int timeoutMs,
        @DefaultValue("20") int perMinute,
        @DefaultValue("100") int perDay,
        @DefaultValue("1000") int globalPerDay,
        @DefaultValue("false") boolean allowLocalMocks) {
    public boolean configured() { return apiKey != null && !apiKey.isBlank(); }
}
