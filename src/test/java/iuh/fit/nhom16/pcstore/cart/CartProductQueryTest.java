package iuh.fit.nhom16.pcstore.cart;

import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationEntity;
import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationId;
import iuh.fit.nhom16.pcstore.cart.entity.CartEntity;
import iuh.fit.nhom16.pcstore.cart.entity.CartItemEntity;
import iuh.fit.nhom16.pcstore.cart.entity.CartItemId;
import iuh.fit.nhom16.pcstore.cart.entity.CartMergeReceiptEntity;
import iuh.fit.nhom16.pcstore.catalog.api.dto.CartProductSnapshot;
import iuh.fit.nhom16.pcstore.catalog.domain.Category;
import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import iuh.fit.nhom16.pcstore.catalog.domain.ProductSpec;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartProductQueryTest {
    @Test
    void cartProjectionAndJpaEntitiesCompileWithHibernateMappingsWithoutDatabaseAccess() throws Exception {
        String hql = ProductRepository.class.getMethod("findCartProducts", Collection.class)
                .getAnnotation(Query.class).value();
        String lockSql = ProductRepository.class.getMethod("lockCartProductRows", Collection.class)
                .getAnnotation(Query.class).value();
        assertTrue(lockSql.toUpperCase().contains("FOR SHARE"));
        assertTrue(lockSql.toUpperCase().contains("ORDER BY"));

        // No connection is configured; metadata and DDL access are explicitly disabled.
        try (var factory = new Configuration()
                .addAnnotatedClass(Product.class)
                .addAnnotatedClass(Category.class)
                .addAnnotatedClass(ProductSpec.class)
                .addAnnotatedClass(CartEntity.class)
                .addAnnotatedClass(CartItemEntity.class)
                .addAnnotatedClass(CartItemId.class)
                .addAnnotatedClass(CartMergeReceiptEntity.class)
                .addAnnotatedClass(CartAddOperationEntity.class)
                .addAnnotatedClass(CartAddOperationId.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .setProperty("hibernate.connection.provider_class",
                        "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
                .buildSessionFactory(); var session = factory.openSession()) {
            assertDoesNotThrow(() -> session.createSelectionQuery(hql, CartProductSnapshot.class)
                    .setParameter("ids", List.of(UUID.randomUUID())));
            assertDoesNotThrow(() -> session.createSelectionQuery("SELECT c FROM CartEntity c WHERE c.userId = :uid", CartEntity.class)
                    .setParameter("uid", UUID.randomUUID()));
        }
    }
}
