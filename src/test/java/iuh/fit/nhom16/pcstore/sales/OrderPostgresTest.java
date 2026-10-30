package iuh.fit.nhom16.pcstore.sales;

import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import iuh.fit.nhom16.pcstore.sales.service.OrderService;
import iuh.fit.nhom16.pcstore.identity.model.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL locks/constraints/rollback. Each invocation owns an isolated, random schema. */
@SpringBootTest
@ActiveProfiles({"order-fixtures", "cart-demo"})
@EnabledIfEnvironmentVariable(named="ORDER_TEST_DB_URL", matches=".+")
class OrderPostgresTest {
    static final String SCHEMA = "order_test_" + UUID.randomUUID().toString().replace("-", "");
    static final UUID CUSTOMER = UUID.fromString("a0000000-0000-0000-0000-000000000002");
    static final UUID SECOND = UUID.fromString("a0000000-0000-0000-0000-000000000003");
    static final UUID ADMIN = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    static final UUID PRODUCT = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("ORDER_TEST_DB_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("ORDER_TEST_DB_USER", "postgres"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("ORDER_TEST_DB_PASSWORD", "postgres"));
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        properties.add("app.orders.expiry-delay-ms", () -> "3600000");
    }
    @Autowired OrderService service;
    @Autowired CartRepository carts;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper json;
    MockMvc mvc;
    @BeforeEach void reset() {
        jdbc.execute("TRUNCATE sales_payment_events,sales_idempotency,sales_history,sales_payment_attempts,sales_order_items,sales_orders,sales_quote_items,sales_quotes,cart_merge_receipts,cart_items,carts RESTART IDENTITY");
        jdbc.update("UPDATE products SET stock_on_hand=10,reserved_quantity=0");
        jdbc.update("UPDATE users SET status='ACTIVE' WHERE id=?", CUSTOMER);
        jdbc.update("DELETE FROM user_roles WHERE user_id=?", TestUser.ID);
        jdbc.update("DELETE FROM users WHERE id=?", TestUser.ID);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    Quote quote(UUID customer) {
        transactions.executeWithoutResult(status -> {
            var cart = carts.lock(customer); cart.quantities().put(PRODUCT, 1); cart.incrementVersion(); carts.save(cart);
        });
        String phone = customer.equals(SECOND) ? "0923456789" : "0912345678";
        return service.quote(customer, new QuoteRequest(1, new Shipping("Customer", phone, "Hồ Chí Minh", "Phường 1", "123 Nguyễn Huệ")));
    }
    Order place(Quote quote, String key) { return service.place(quote.customerId(), key, new PlaceRequest(quote.id(), PaymentMethod.COD)); }
    @Test void deployedCartItemIdWithoutDefaultStillSavesAndUpdatesOneLine() {
        jdbc.execute("ALTER TABLE cart_items ALTER COLUMN id DROP DEFAULT");
        try {
            Quote quote = quote(CUSTOMER);
            assertNotNull(jdbc.queryForObject("SELECT id FROM cart_items", UUID.class));
            transactions.executeWithoutResult(status -> {
                var cart = carts.lock(CUSTOMER); cart.quantities().put(PRODUCT, 2); cart.incrementVersion(); carts.save(cart);
            });
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM cart_items", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT quantity FROM cart_items", Integer.class));
        } finally { jdbc.execute("ALTER TABLE cart_items ALTER COLUMN id SET DEFAULT gen_random_uuid()"); }
    }
    @Test void fixedLoginMergesGuestCartAndPersistsItAcrossLogoutAndNewSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/cart/items").session(session).with(csrf()).contentType("application/json")
                .content("{\"productId\":\"" + PRODUCT + "\",\"quantity\":1,\"cartVersion\":0}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").session(session).contentType("application/json").content("{\"username\":\"test\",\"password\":\"1234\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType("application/json").content("{\"username\":\"test\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        String previousId = session.getId();
        mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType("application/json").content("{\"username\":\"test\",\"password\":\"1234\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("test")).andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.cart.totalQuantity").value(1)).andExpect(jsonPath("$.cart.cartVersion").value(1));
        assertNotEquals(previousId, session.getId());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(TestUser.ID.toString()));
        mvc.perform(get("/api/v1/cart").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(1));
        mvc.perform(patch("/api/v1/cart/items/" + PRODUCT).session(session).with(csrf()).contentType("application/json").content("{\"quantity\":2,\"cartVersion\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(2));
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        MockHttpSession nextSession = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(nextSession).with(csrf()).contentType("application/json").content("{\"username\":\"test\",\"password\":\"1234\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cart.totalQuantity").value(2));
        mvc.perform(get("/api/v1/cart").session(nextSession)).andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(2));
    }
    @Test void sameKeyConcurrentRequestsCreateOneAggregate() throws Exception {
        Quote quote = quote(CUSTOMER);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var first = pool.submit(() -> { start.await(); return place(quote, "same-key-123"); });
            var second = pool.submit(() -> { start.await(); return place(quote, "same-key-123"); });
            start.countDown();
            assertEquals(first.get(15, TimeUnit.SECONDS).id(), second.get(15, TimeUnit.SECONDS).id());
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM sales_orders", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM sales_payment_attempts", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM sales_history", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT reserved_quantity FROM products WHERE id=?", Integer.class, PRODUCT));
    }
    @Test void differentKeysCannotConsumeSameQuoteTwice() throws Exception {
        Quote quote = quote(CUSTOMER);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var tasks = pool.invokeAll(List.of(
                    () -> { try { place(quote, "different-111"); return true; } catch (OrderException ex) { return false; } },
                    () -> { try { place(quote, "different-222"); return true; } catch (OrderException ex) { return false; } }));
            int success = 0; for (var task : tasks) if (Boolean.TRUE.equals(task.get())) success++;
            assertEquals(1, success);
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM sales_orders", Integer.class));
    }
    @Test void twoCustomersCompeteForLastUnitWithoutOverselling() throws Exception {
        jdbc.update("UPDATE users SET phone_verified_at=now() WHERE id=?", SECOND);
        jdbc.update("UPDATE products SET stock_on_hand=1 WHERE id=?", PRODUCT);
        Quote first = quote(CUSTOMER), second = quote(SECOND);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var tasks = pool.invokeAll(List.of(
                    () -> { try { place(first, "customer-111"); return true; } catch (OrderException ex) { return false; } },
                    () -> { try { place(second, "customer-222"); return true; } catch (OrderException ex) { return false; } }));
            int success = 0; for (var task : tasks) if (Boolean.TRUE.equals(task.get())) success++;
            assertEquals(1, success);
        }
        assertEquals(1, jdbc.queryForObject("SELECT reserved_quantity FROM products WHERE id=?", Integer.class, PRODUCT));
    }
    @Test void failureAfterReservingRollsBackOrderStockQuoteAndCart() {
        Quote quote = quote(CUSTOMER);
        // Force persistence to fail after inventory is reserved, inside the same transaction.
        jdbc.execute("ALTER TABLE sales_orders ADD CONSTRAINT reject_test_order CHECK (false) NOT VALID");
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> place(quote, "rollback-123"));
        } finally {
            jdbc.execute("ALTER TABLE sales_orders DROP CONSTRAINT reject_test_order");
        }
        assertEquals(0, jdbc.queryForObject("SELECT reserved_quantity FROM products WHERE id=?", Integer.class, PRODUCT));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM sales_orders", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM cart_items", Integer.class));
        assertFalse(jdbc.queryForObject("SELECT consumed FROM sales_quotes WHERE id=?", Boolean.class, quote.id()));
        assertNotNull(place(quote, "rollback-123").id());
    }
    @Test void codLifecycleAndStaleVersionAreEnforced() {
        Order order = place(quote(CUSTOMER), "lifecycle-123");
        order = service.quantity(ADMIN, order.id(), PRODUCT, new QuantityRequest(order.version(), 2, "Customer asked"));
        assertEquals(2, jdbc.queryForObject("SELECT reserved_quantity FROM products WHERE id=?", Integer.class, PRODUCT));
        UUID id = order.id();
        assertThrows(OrderException.class, () -> service.transition(ADMIN, id, true, new TransitionRequest(0, OrderStatus.CONFIRMED, "Stale")));
        order = service.transition(ADMIN, id, true, new TransitionRequest(order.version(), OrderStatus.CONFIRMED, "Confirm"));
        order = service.transition(ADMIN, id, true, new TransitionRequest(order.version(), OrderStatus.SHIPPING, "Ship"));
        assertEquals("UNPAID", order.paymentStatus());
        assertEquals(8, jdbc.queryForObject("SELECT stock_on_hand FROM products WHERE id=?", Integer.class, PRODUCT));
        assertEquals(0, jdbc.queryForObject("SELECT reserved_quantity FROM products WHERE id=?", Integer.class, PRODUCT));
        order = service.transition(ADMIN, id, true, new TransitionRequest(order.version(), OrderStatus.COMPLETED, "Delivered"));
        assertEquals("PAID", order.paymentStatus());
    }
    @Test void httpRequiresIdentityCsrfAndAdminRoleAndReturnsQuoteTotals() throws Exception {
        MockHttpSession session = new MockHttpSession(); session.setAttribute(CartOwnerResolver.DEMO_OWNER, CUSTOMER);
        mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/orders").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/orders/account")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/orders/account").session(session)).andExpect(status().isForbidden());
        MockHttpSession admin = new MockHttpSession(); admin.setAttribute(CartOwnerResolver.DEMO_OWNER, ADMIN);
        mvc.perform(get("/api/v1/admin/orders/account").session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ADMIN.toString()));
        var request = new QuoteRequest(0, new Shipping("Customer", "0912345678", "City", "Ward", "123 Street"));
        mvc.perform(post("/api/v1/orders/quotes").session(session).contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        quote(CUSTOMER);
        request = new QuoteRequest(1, request.shipping());
        mvc.perform(post("/api/v1/orders/quotes").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(33550000)).andExpect(jsonPath("$.shippingFee").value(50000));
    }
}
