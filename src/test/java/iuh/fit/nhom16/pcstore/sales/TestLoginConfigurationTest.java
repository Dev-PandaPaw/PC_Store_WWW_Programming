package iuh.fit.nhom16.pcstore.sales;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class TestLoginConfigurationTest {
    @Test void onlyOneLoginControllerIsEnabledAtATime() {
        var runner = new ApplicationContextRunner()
                .withUserConfiguration(iuh.fit.nhom16.pcstore.identity.web.AuthController.class,
                        iuh.fit.nhom16.pcstore.identity.controller.TestLoginController.class)
                .withBean(iuh.fit.nhom16.pcstore.identity.application.AuthService.class,
                        () -> org.mockito.Mockito.mock(iuh.fit.nhom16.pcstore.identity.application.AuthService.class))
                .withBean(iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository.class,
                        () -> org.mockito.Mockito.mock(iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository.class))
                .withBean(iuh.fit.nhom16.pcstore.identity.service.TestLoginService.class,
                        () -> org.mockito.Mockito.mock(iuh.fit.nhom16.pcstore.identity.service.TestLoginService.class))
                .withBean(iuh.fit.nhom16.pcstore.cart.service.CartStorageService.class,
                        () -> org.mockito.Mockito.mock(iuh.fit.nhom16.pcstore.cart.service.CartStorageService.class));
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(iuh.fit.nhom16.pcstore.identity.web.AuthController.class).size());
            assertTrue(context.getBeansOfType(iuh.fit.nhom16.pcstore.identity.controller.TestLoginController.class).isEmpty());
        });
        runner.withPropertyValues("app.test-login.enabled=true").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(iuh.fit.nhom16.pcstore.identity.web.AuthController.class).isEmpty());
            assertEquals(1, context.getBeansOfType(iuh.fit.nhom16.pcstore.identity.controller.TestLoginController.class).size());
        });
    }
    @Test void normalStartupUsesDevelopmentLoginWithoutReplacingCatalog() {
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
                    assertEquals("false", environment.getProperty("app.test-login.enabled"));
                });
    }
}
