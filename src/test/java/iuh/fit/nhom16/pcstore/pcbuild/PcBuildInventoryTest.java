package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.*;
import static iuh.fit.nhom16.pcstore.pcbuild.PcBuildDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PcBuildInventoryTest {
    final UUID id = UUID.randomUUID();
    final Line line = new Line(id, "Synthetic", 2, new BigDecimal("10.25"), 4, false);
    PcBuildInventory.Stock stock(String price, String status, int available, long version) {
        return new PcBuildInventory.Stock(id, new BigDecimal(price), status, available, version);
    }
    @Test void exactCurrentQuoteAndStockPass() {
        assertDoesNotThrow(() -> PcBuildInventory.validate(List.of(line), Map.of(id, stock("10.250", "ACTIVE", 2, 4))));
    }
    @Test void changedPriceOrVersionMustBeReconfirmed() {
        for (var current : List.of(stock("10.26", "ACTIVE", 2, 4), stock("10.25", "ACTIVE", 2, 5))) {
            var ex = assertThrows(CartException.class, () -> PcBuildInventory.validate(List.of(line), Map.of(id, current)));
            assertEquals("PC_BUILD_PRICE_CHANGED", ex.getErrorCode());
        }
    }
    @Test void missingInactiveAndReservedStockCannotBePurchased() {
        for (Map<UUID,PcBuildInventory.Stock> products : List.of(Map.<UUID,PcBuildInventory.Stock>of(),
                Map.of(id, stock("10.25", "INACTIVE", 2, 4)), Map.of(id, stock("10.25", "ACTIVE", 1, 4)))) {
            var ex = assertThrows(CartException.class, () -> PcBuildInventory.validate(List.of(line), products));
            assertEquals("PC_BUILD_STOCK_CHANGED", ex.getErrorCode());
        }
    }
    @Test void cannotReadInventoryOutsideWriteTransaction() {
        var jdbc = mock(JdbcTemplate.class);
        assertThrows(IllegalStateException.class, () -> new PcBuildInventory(jdbc).validateLocked(List.of(line)));
        verifyNoInteractions(jdbc);
    }
}
