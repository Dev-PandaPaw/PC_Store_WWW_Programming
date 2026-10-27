package iuh.fit.nhom16.pcstore.sales;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class TestLoginConfigurationTest {
    @Test void normalStartupEnablesTestLoginWithoutReplacingCatalog() {
        // Load configuration only: do not connect to or mutate the configured database.
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var environment = context.getEnvironment();
                    assertTrue(Arrays.asList(environment.getDefaultProfiles()).contains("local"));
                    assertFalse(Arrays.asList(environment.getActiveProfiles()).contains("order-fixtures"));
                    assertEquals("classpath:db/migration,classpath:db/orders", environment.getProperty("spring.flyway.locations"));
                    assertNotNull(environment.getProperty("spring.datasource.url"));
                    assertEquals("true", environment.getProperty("app.test-login.enabled"));
                });
    }
}
