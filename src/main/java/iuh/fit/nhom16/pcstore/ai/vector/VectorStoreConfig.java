package iuh.fit.nhom16.pcstore.ai.vector;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import iuh.fit.nhom16.pcstore.ai.AiSettings;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.embedding.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.net.URI;
import java.util.List;
import java.util.Set;

/** Hạ tầng mục 3.3. Không gọi Gemini hoặc tự reindex khi khởi động ứng dụng. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiSettings.class)
public class VectorStoreConfig {
    @Bean(destroyMethod = "close")
    Client productEmbeddingClient(AiSettings settings) {
        URI uri = URI.create(settings.baseUrl());
        boolean google = "https".equals(uri.getScheme())
                && "generativelanguage.googleapis.com".equals(uri.getHost());
        boolean local = settings.allowLocalMocks() && "http".equals(uri.getScheme())
                && Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
        if ((!google && !local) || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Invalid embedding base URL");
        if (settings.timeoutMs() < 100 || settings.timeoutMs() > 25000)
            throw new IllegalArgumentException("Invalid embedding timeout");
        // Khi thiếu key, model bên dưới sẽ chặn trước mọi HTTP request.
        return Client.builder().apiKey(settings.configured() ? settings.apiKey().trim() : "not-configured")
                .httpOptions(HttpOptions.builder()
                        .baseUrl(settings.baseUrl().replaceFirst("/v1beta/?$", "")).apiVersion("v1beta")
                        .timeout(settings.timeoutMs())
                        // SDK 1.65 vẫn sleep ở lỗi retryable dù attempts=1; danh sách rỗng tránh delay đó.
                        .retryOptions(HttpRetryOptions.builder().attempts(1).httpStatusCodes(List.of()).build()).build()).build();
    }

    @Bean
    EmbeddingModel productEmbeddingModel(Client productEmbeddingClient, AiSettings settings,
            @Value("${app.ai.vector.model:gemini-embedding-001}") String model,
            @Value("${app.ai.vector.dimensions:768}") int dimensions) {
        if (!model.matches("[a-z0-9][a-z0-9.-]{0,99}") || dimensions < 1 || dimensions > 3072)
            throw new IllegalArgumentException("Invalid embedding model or dimensions");
        var connection = GoogleGenAiEmbeddingConnectionDetails.builder().genAiClient(productEmbeddingClient).build();
        var options = GoogleGenAiTextEmbeddingOptions.builder().model(model).dimensions(dimensions).build();
        var delegate = new GoogleGenAiTextEmbeddingModel(connection, options,
                new RetryTemplate(RetryPolicy.withMaxRetries(0)));
        return new ProductEmbeddingModel(delegate, settings.configured(), dimensions);
    }

    @Bean
    ProductVectorStore vectorStore(EmbeddingModel productEmbeddingModel) {
        return new ProductVectorStore(productEmbeddingModel);
    }
}
