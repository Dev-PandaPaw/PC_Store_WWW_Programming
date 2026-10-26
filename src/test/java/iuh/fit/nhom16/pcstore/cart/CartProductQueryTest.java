package iuh.fit.nhom16.pcstore.cart;

import iuh.fit.nhom16.pcstore.catalog.CartProductSnapshot;
import iuh.fit.nhom16.pcstore.catalog.Category;
import iuh.fit.nhom16.pcstore.catalog.Product;
import iuh.fit.nhom16.pcstore.catalog.ProductRepository;
import iuh.fit.nhom16.pcstore.catalog.ProductSpec;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CartProductQueryTest {
    @Test
    void cartProjectionCompilesWithEntityMappingsWithoutDatabaseAccess() throws Exception {
        String hql = ProductRepository.class.getMethod("findCartProducts", Collection.class)
                .getAnnotation(Query.class).value();
        // No connection is configured; metadata and DDL access are explicitly disabled.
        try (var factory = new Configuration()
                .addAnnotatedClass(Product.class)
                .addAnnotatedClass(Category.class)
                .addAnnotatedClass(ProductSpec.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .setProperty("hibernate.connection.provider_class",
                        "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
                .buildSessionFactory(); var session = factory.openSession()) {
            assertDoesNotThrow(() -> session.createSelectionQuery(hql, CartProductSnapshot.class)
                    .setParameter("ids", List.of(UUID.randomUUID())));
        }
    }
}
