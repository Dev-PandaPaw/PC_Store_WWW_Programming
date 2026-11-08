package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.Map;

@Component
public class PcBuildClient {
    private final RestClient client;
    private final RestClient checkoutClient;
    private final String key;
    public PcBuildClient(@Value("${app.pc-build.base-url:http://127.0.0.1:8000}") String url,
            @Value("${app.pc-build.backend-key:}") String key,
            @Value("${app.pc-build.read-timeout-seconds:210}") int seconds) {
        this.key = key;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(seconds));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        var checkoutFactory = new SimpleClientHttpRequestFactory();
        checkoutFactory.setConnectTimeout(Duration.ofSeconds(3));
        checkoutFactory.setReadTimeout(Duration.ofSeconds(Math.min(seconds, 15)));
        checkoutClient = RestClient.builder().baseUrl(url).requestFactory(checkoutFactory).build();
    }
    public Map<String,Object> checkout(Map<String,Object> body) {
        return post(checkoutClient, "/api/internal/cart/checkout-check", body);
    }
    public Map<String,Object> post(String path, Map<String,Object> body) {
        return post(client, path, body);
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> post(RestClient transport, String path, Map<String,Object> body) {
        if (key.isBlank()) throw new CartException(HttpStatus.SERVICE_UNAVAILABLE,
                "PC_BUILD_NOT_CONFIGURED", "Dịch vụ build PC chưa được cấu hình.");
        try {
            Map<String,Object> result = transport.post().uri(path).header("X-Cart-Backend-Key", key)
                    .body(body).retrieve().body(Map.class);
            if (result == null) throw new IllegalStateException();
            return result;
        } catch (Exception ex) {
            // Never forward a Python error body, secret or document context to logs.
            throw new CartException(HttpStatus.BAD_GATEWAY, "PC_BUILD_UNAVAILABLE",
                    "Dịch vụ build PC chưa phản hồi. Vui lòng thử lại.");
        }
    }
}
