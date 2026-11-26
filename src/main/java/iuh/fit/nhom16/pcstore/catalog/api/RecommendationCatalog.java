package iuh.fit.nhom16.pcstore.catalog.api;

import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import java.util.*;

/** Current catalog facts, never supplied by an embedding or a language model. */
public interface RecommendationCatalog {
    record Item(UUID id, StoreProduct product, String status, int stockOnHand, int reservedQuantity, String socket,
                Map<String,Object> componentSpecs, String verificationStatus) {
        public Item(UUID id, StoreProduct product, String status, int stockOnHand, int reservedQuantity, String socket) {
            this(id,product,status,stockOnHand,reservedQuantity,socket,Map.of(),"unknown");
        }
        public Item { componentSpecs=componentSpecs==null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(componentSpecs)); }
        public boolean purchasable() {
            return "ACTIVE".equals(status) && stockOnHand > 0 && reservedQuantity >= 0
                    && (long) stockOnHand - reservedQuantity > 0;
        }
    }
    Map<UUID, Item> findAll(Set<UUID> ids);
    List<Item> indexableProducts();
}
