package iuh.fit.nhom16.pcstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class PcStoreApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(PcStoreApplication.class, args);
    }

    private static void loadDotenv() {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // Fallback to application.yml defaults
        }
    }
}
