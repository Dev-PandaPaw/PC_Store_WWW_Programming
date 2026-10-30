package iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence;

import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import iuh.fit.nhom16.pcstore.catalog.application.StorefrontCatalog;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Public presentation data backed by the same products used for orders and inventory. */
@Repository
public class StorefrontCatalogRepository implements StorefrontCatalog {
    private final StorefrontDataRepository repository;
    private final JsonMapper json;

    public StorefrontCatalogRepository(StorefrontDataRepository repository, JsonMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    public List<StoreProduct> products() {
        String productsJson = repository.findStorefrontProductsJson(null, null, null, null, null, false, "featured");
        return json.convertValue(json.readTree(productsJson),
                new TypeReference<List<StoreProduct>>() {});
    }
}
