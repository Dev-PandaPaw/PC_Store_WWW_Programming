package iuh.fit.nhom16.pcstore.pcbuild;

import com.sun.net.httpserver.HttpServer;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class PcBuildClientTest {
    @Test void sendsServerKeyAndJsonToPythonContract() throws Exception {
        var header = new AtomicReference<String>();
        var body = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/pc-build/advice", exchange -> {
            header.set(exchange.getRequestHeaders().getFirst("X-Cart-Backend-Key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] response = "{\"rag_status\":\"not_requested\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var client = new PcBuildClient("http://127.0.0.1:" + server.getAddress().getPort(),"synthetic-secret",5);
            var result = client.post("/api/pc-build/advice",Map.of("budget_vnd","45000000","question","Synthetic"));
            assertEquals("not_requested",result.get("rag_status"));
            assertEquals("synthetic-secret",header.get());
            assertTrue(body.get().contains("45000000"));
        } finally { server.stop(0); }
    }
    @Test void remoteErrorsDoNotExposeResponseBody() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/check", exchange -> {
            byte[] response = "PRIVATE_DATABASE_DETAIL".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500,response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            var client = new PcBuildClient("http://127.0.0.1:" + server.getAddress().getPort(),"synthetic-secret",5);
            var error = assertThrows(CartException.class,() -> client.post("/check",Map.of()));
            assertEquals("PC_BUILD_UNAVAILABLE",error.getErrorCode());
            assertFalse(error.getMessage().contains("PRIVATE_DATABASE"));
        } finally { server.stop(0); }
    }
    @Test void missingConfigurationFailsBeforeNetworkCall() {
        var error = assertThrows(CartException.class,() -> new PcBuildClient("http://127.0.0.1:1","",1).post("/check",Map.of()));
        assertEquals("PC_BUILD_NOT_CONFIGURED",error.getErrorCode());
    }
}
