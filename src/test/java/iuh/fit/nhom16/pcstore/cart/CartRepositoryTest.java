package iuh.fit.nhom16.pcstore.cart;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CartRepositoryTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CartRepository repository = new CartRepository(jdbc);

    @Test
    void savesLinesInBatchWithoutDeletingExistingProducts() throws Exception {
        UUID product = UUID.randomUUID();
        CartSession cart = new CartSession(UUID.randomUUID(), 2, Map.of(product, 4));
        when(jdbc.batchUpdate(anyString(), anyCollection(), anyInt(), any(ParameterizedPreparedStatementSetter.class)))
                .thenAnswer(call -> {
                    String sql = call.getArgument(0);
                    assertTrue(sql.contains("ON CONFLICT (cart_id, product_id)"));
                    assertTrue(sql.contains("IS DISTINCT FROM"));
                    Collection<Map.Entry<UUID, Integer>> entries = call.getArgument(1);
                    assertEquals(1, entries.size());
                    ParameterizedPreparedStatementSetter<Map.Entry<UUID, Integer>> setter = call.getArgument(3);
                    PreparedStatement statement = mock(PreparedStatement.class);
                    setter.setValues(statement, entries.iterator().next());
                    verify(statement).setObject(eq(1), any(UUID.class));
                    verify(statement).setObject(2, cart.mergeId());
                    verify(statement).setObject(3, product);
                    verify(statement).setInt(4, 4);
                    return new int[][] {{1}};
                });

        repository.save(cart);

        verify(jdbc).update(eq("DELETE FROM cart_items WHERE cart_id = ? AND product_id NOT IN (?)"),
                eq(cart.mergeId()), eq(product));
        verify(jdbc, never()).update(eq("DELETE FROM cart_items WHERE cart_id = ?"), any(UUID.class));
        verify(jdbc).batchUpdate(anyString(), anyCollection(), eq(20), any(ParameterizedPreparedStatementSetter.class));
    }

    @Test
    void clearingCartDeletesLinesWithoutEmptyInClauseOrInsertBatch() {
        CartSession cart = new CartSession();

        repository.save(cart);

        verify(jdbc).update("DELETE FROM cart_items WHERE cart_id = ?", cart.mergeId());
        verify(jdbc, never()).batchUpdate(anyString(), anyCollection(), anyInt(), any(ParameterizedPreparedStatementSetter.class));
    }
}
