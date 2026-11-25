package iuh.fit.nhom16.pcstore.cart;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import iuh.fit.nhom16.pcstore.cart.controller.CartController;
import iuh.fit.nhom16.pcstore.cart.entity.CartEntity;
import iuh.fit.nhom16.pcstore.cart.repository.CartJpaRepository;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import iuh.fit.nhom16.pcstore.configuration.web.RestExceptionHandler;
import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real Supabase PostgreSQL + Spring Data JPA concurrency verification.
 * Reads DB_URL / DB_USERNAME / DB_PASSWORD from .env (no Docker needed),
 * creates an isolated temporary schema, validates JPA entities (ddl-auto=validate),
 * runs concurrent lock/idempotency tests, and drops the temporary schema afterwards.
 */
@EnabledIfSystemProperty(named = "cart.supabase", matches = "true")
@SpringJUnitConfig(CartPostgresConcurrencyTest.JpaSliceConfig.class)
class CartPostgresConcurrencyTest {
    static final String SCHEMA = "cart_pg_check_" + UUID.randomUUID().toString().replace("-", "");
    static final UUID CUSTOMER_ONE = UUID.fromString("a0000000-0000-0000-0000-000000000002");
    static final UUID CUSTOMER_TWO = UUID.fromString("a0000000-0000-0000-0000-000000000003");
    static final UUID CATEGORY = UUID.fromString("c0000000-0000-0000-0000-000000000001");
    static final UUID PRODUCT = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    static String dbUrl;
    static String dbUser;
    static String dbPassword;

    @BeforeAll
    static void initIsolatedSupabaseSchema() throws Exception {
        Properties dotenv = new Properties();
        if (Files.exists(Path.of(".env"))) {
            try (var reader = Files.newBufferedReader(Path.of(".env"))) {
                dotenv.load(reader);
            }
        }
        dbUrl = setting(dotenv, "CART_TEST_DB_URL", setting(dotenv, "DB_URL", ""));
        dbUser = setting(dotenv, "CART_TEST_DB_USER", setting(dotenv, "DB_USERNAME", "postgres"));
        dbPassword = setting(dotenv, "CART_TEST_DB_PASSWORD", setting(dotenv, "DB_PASSWORD", ""));
        assertTrue(dbUrl.startsWith("jdbc:postgresql:"), "Configure DB_URL in .env for Supabase PostgreSQL test");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA " + SCHEMA);
            stmt.execute("SET search_path TO " + SCHEMA);
            stmt.execute("""
                    CREATE TABLE users (
                        id UUID PRIMARY KEY,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        phone VARCHAR(50),
                        full_name VARCHAR(150) NOT NULL,
                        recipient_name VARCHAR(150),
                        province VARCHAR(100),
                        district VARCHAR(100),
                        ward VARCHAR(100),
                        address_line VARCHAR(255),
                        status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                        status_reason VARCHAR(255),
                        session_version BIGINT NOT NULL DEFAULT 1,
                        version BIGINT NOT NULL DEFAULT 0,
                        email_verified_at TIMESTAMPTZ,
                        phone_verified_at TIMESTAMPTZ,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
                    );
                    CREATE TABLE user_roles (
                        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        role VARCHAR(50) NOT NULL,
                        PRIMARY KEY (user_id, role)
                    );
                    CREATE TABLE categories (
                        id UUID PRIMARY KEY,
                        name VARCHAR(150) NOT NULL,
                        slug VARCHAR(150) NOT NULL UNIQUE,
                        description TEXT,
                        version BIGINT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
                    );
                    CREATE TABLE products (
                        id UUID PRIMARY KEY,
                        category_id UUID NOT NULL REFERENCES categories(id),
                        sku VARCHAR(100) NOT NULL UNIQUE,
                        slug VARCHAR(255) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        brand VARCHAR(100),
                        description TEXT,
                        price NUMERIC(15,2) NOT NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                        warranty_months SMALLINT DEFAULT 12,
                        stock_on_hand INTEGER NOT NULL DEFAULT 0,
                        reserved_quantity INTEGER NOT NULL DEFAULT 0,
                        version BIGINT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
                    );
                    CREATE TABLE product_specs (
                        product_id UUID PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
                        cpu VARCHAR(255),
                        gpu VARCHAR(255),
                        ram_gb INTEGER,
                        storage_gb INTEGER,
                        storage_type VARCHAR(255),
                        motherboard VARCHAR(255),
                        psu VARCHAR(255),
                        case_name VARCHAR(255),
                        operating_system VARCHAR(255)
                    );
                    """);
            for (String migration : List.of(
                    "db/migration/V8__persistent_user_carts.sql",
                    "db/orders/V10__cart_storage_compatibility.sql",
                    "db/orders/V19__cart_jpa_and_add_idempotency.sql")) {
                try (InputStream in = CartPostgresConcurrencyTest.class.getClassLoader().getResourceAsStream(migration)) {
                    assertNotNull(in, "Missing migration: " + migration);
                    stmt.execute(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            stmt.execute("INSERT INTO categories(id, name, slug) VALUES ('" + CATEGORY + "', 'VGA', 'vga')");
            stmt.execute("INSERT INTO users(id, email, password_hash, full_name, status) VALUES "
                    + "('" + CUSTOMER_ONE + "', 'c1@example.com', 'hash', 'Customer 1', 'ACTIVE'),"
                    + "('" + CUSTOMER_TWO + "', 'c2@example.com', 'hash', 'Customer 2', 'ACTIVE')");
            stmt.execute("INSERT INTO products(id, category_id, sku, slug, name, price, status, stock_on_hand, reserved_quantity) VALUES "
                    + "('" + PRODUCT + "', '" + CATEGORY + "', 'GPU-5070', 'gpu-5070', 'RTX 5070', 18000000, 'ACTIVE', 5, 0)");
        }
    }

    @AfterAll
    static void dropIsolatedSupabaseSchema() throws Exception {
        if (dbUrl != null && dbUrl.startsWith("jdbc:postgresql:") && SCHEMA.matches("cart_pg_check_[a-f0-9]{32}")) {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 var stmt = conn.createStatement()) {
                stmt.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
            }
        }
    }

    static String setting(Properties props, String key, String fallback) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) return env.trim();
        return props.getProperty(key, fallback).trim();
    }

    @Configuration
    @EnableTransactionManagement
    @EntityScan(basePackageClasses = {CartEntity.class, Product.class, User.class})
    @EnableJpaRepositories(basePackageClasses = {CartJpaRepository.class, ProductRepository.class, UserRepository.class})
    static class JpaSliceConfig {
        @Bean(destroyMethod = "close")
        HikariDataSource dataSource() {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(dbUrl);
            cfg.setUsername(dbUser);
            cfg.setPassword(dbPassword);
            cfg.setMaximumPoolSize(4);
            cfg.setMinimumIdle(1);
            cfg.setConnectionInitSql("SET search_path TO " + SCHEMA);
            return new HikariDataSource(cfg);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
            emf.setDataSource(dataSource);
            emf.setPackagesToScan(
                    "iuh.fit.nhom16.pcstore.cart.entity",
                    "iuh.fit.nhom16.pcstore.catalog.domain",
                    "iuh.fit.nhom16.pcstore.identity.domain");
            emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            Properties jpaProps = new Properties();
            jpaProps.setProperty("hibernate.hbm2ddl.auto", "validate");
            jpaProps.setProperty("hibernate.default_schema", SCHEMA);
            emf.setJpaProperties(jpaProps);
            return emf;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
            return new TransactionTemplate(txManager);
        }

        @Bean
        CartRepository cartRepository(
                CartJpaRepository carts,
                iuh.fit.nhom16.pcstore.cart.repository.CartMergeReceiptJpaRepository merges,
                iuh.fit.nhom16.pcstore.cart.repository.CartAddOperationJpaRepository addOps,
                UserRepository users) {
            return new CartRepository(carts, merges, addOps, users);
        }

        @Bean
        CartService cartService(ProductRepository products) {
            return new CartService(products);
        }

        @Bean
        CartStorageService cartStorageService(CartRepository repo, TransactionTemplate tx, CartService rules) {
            return new CartStorageService(repo, tx, rules);
        }

        @Bean
        CartOwnerResolver cartOwnerResolver(CartRepository repo) {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("cart-demo");
            return new CartOwnerResolver(env, repo);
        }

        @Bean
        CartController cartController(CartService service, CartStorageService storage, CartOwnerResolver owners) {
            return new CartController(service, storage, owners);
        }
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired CartController cartController;

    final ObjectMapper json = new ObjectMapper();
    MockMvc mvc;

    @BeforeEach
    void reset() {
        jdbc.execute("TRUNCATE cart_add_operations, cart_merge_receipts, cart_items, carts RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE products SET stock_on_hand = 5, reserved_quantity = 0, status = 'ACTIVE' WHERE id = ?", PRODUCT);
        mvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private MockHttpSession sessionFor(UUID userId) {
        MockHttpSession session = new MockHttpSession();
        if (userId != null) {
            session.setAttribute(CartOwnerResolver.DEMO_OWNER, userId);
        }
        return session;
    }

    @Test
    void forShareLockWaitsForConcurrentStockUpdateAndReportsExactRemainingAddableQuantity() throws Exception {
        MockHttpSession session = sessionFor(CUSTOMER_ONE);
        mvc.perform(post("/api/v1/cart/items").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":1,\"cartVersion\":0,\"operationId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(1));

        try (ExecutorService pool = Executors.newSingleThreadExecutor();
             Connection writer = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            writer.setAutoCommit(false);
            try (var stmt = writer.createStatement()) {
                stmt.execute("SET search_path TO " + SCHEMA);
            }
            try (var lock = writer.prepareStatement("SELECT id FROM products WHERE id = ? FOR UPDATE")) {
                lock.setObject(1, PRODUCT);
                lock.executeQuery().close();
            }

            CountDownLatch dispatched = new CountDownLatch(1);
            var pendingAdd = pool.submit(() -> {
                dispatched.countDown();
                return mvc.perform(post("/api/v1/cart/items")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":3,\"cartVersion\":1,\"operationId\":\"" + UUID.randomUUID() + "\"}"))
                        .andReturn();
            });

            assertTrue(dispatched.await(5, TimeUnit.SECONDS));
            Thread.sleep(500);
            assertFalse(pendingAdd.isDone(), "FOR SHARE must wait while concurrent writer holds FOR UPDATE on products");

            // Writer reserves 3 units -> available drops from 5 to 2; cart already has 1 -> remainingAddableQuantity = 1
            try (var update = writer.prepareStatement("UPDATE products SET reserved_quantity = 3 WHERE id = ?")) {
                update.setObject(1, PRODUCT);
                update.executeUpdate();
            }
            writer.commit();

            MvcResult result = pendingAdd.get(20, TimeUnit.SECONDS);
            assertEquals(409, result.getResponse().getStatus());
            Map<?, ?> body = json.readValue(result.getResponse().getContentAsString(), Map.class);
            assertEquals("CART_INSUFFICIENT_STOCK", body.get("errorCode"));
            assertEquals(2, ((Number) body.get("availableQuantity")).intValue());
            assertEquals(1, ((Number) body.get("currentCartQuantity")).intValue());
            assertEquals(1, ((Number) body.get("remainingAddableQuantity")).intValue());

            assertEquals(1, jdbc.queryForObject("SELECT quantity FROM cart_items", Integer.class));
            assertEquals(1L, jdbc.queryForObject("SELECT version FROM carts WHERE user_id = ?", Long.class, CUSTOMER_ONE));
        }
    }

    @Test
    void twoCustomersCanConcurrentlyAddLastAvailableUnitBecauseCartDoesNotReserveStock() throws Exception {
        jdbc.update("UPDATE products SET stock_on_hand = 1, reserved_quantity = 0 WHERE id = ?", PRODUCT);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var first = pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/cart/items")
                                .session(sessionFor(CUSTOMER_ONE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":1,\"cartVersion\":0,\"operationId\":\"" + UUID.randomUUID() + "\"}"))
                        .andReturn().getResponse().getStatus();
            });
            var second = pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/cart/items")
                                .session(sessionFor(CUSTOMER_TWO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":1,\"cartVersion\":0,\"operationId\":\"" + UUID.randomUUID() + "\"}"))
                        .andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertEquals(List.of(200, 200), List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)));
        }

        // Subsequent stock depletion after adding is normal since cart does not reserve stock
        jdbc.update("UPDATE products SET reserved_quantity = 1 WHERE id = ?", PRODUCT);
        mvc.perform(get("/api/v1/cart").session(sessionFor(CUSTOMER_ONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].availableQuantity").value(0))
                .andExpect(jsonPath("$.items[0].purchasable").value(false));
    }

    @Test
    void lostResponseAndReloadRetryWithNewVersionOrConcurrentDuplicateNeverDoubleAdds() throws Exception {
        UUID opId = UUID.randomUUID();
        MockHttpSession session = sessionFor(CUSTOMER_ONE);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var a = pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/cart/items")
                                .session(sessionFor(CUSTOMER_ONE))
                                .header("Idempotency-Key", opId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":2,\"cartVersion\":0,\"operationId\":\"" + opId + "\"}"))
                        .andReturn();
            });
            var b = pool.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/cart/items")
                                .session(sessionFor(CUSTOMER_ONE))
                                .header("Idempotency-Key", opId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":2,\"cartVersion\":0,\"operationId\":\"" + opId + "\"}"))
                        .andReturn();
            });
            start.countDown();
            assertEquals(200, a.get(20, TimeUnit.SECONDS).getResponse().getStatus());
            assertEquals(200, b.get(20, TimeUnit.SECONDS).getResponse().getStatus());
        }

        assertEquals(2, jdbc.queryForObject("SELECT quantity FROM cart_items", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM cart_add_operations", Integer.class));

        // Client reloads cart (receives cartVersion = 1), then retries the same operationId with cartVersion = 1
        mvc.perform(get("/api/v1/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartVersion").value(1))
                .andExpect(jsonPath("$.totalQuantity").value(2));

        mvc.perform(post("/api/v1/cart/items").session(session)
                        .header("Idempotency-Key", opId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":2,\"cartVersion\":1,\"operationId\":\"" + opId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.cartVersion").value(1));

        // Decimal quantity returns 400 and is never coerced to integer
        mvc.perform(post("/api/v1/cart/items").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":1.5,\"cartVersion\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CART_INVALID_QUANTITY"));
    }
}
