package iuh.fit.nhom16.pcstore.catalog.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Only the current price and inventory fields required by cart rules. */
public record CartProductSnapshot(UUID id, String sku, String name, BigDecimal price,
                                  String status, Integer stockOnHand, Integer reservedQuantity) { }
