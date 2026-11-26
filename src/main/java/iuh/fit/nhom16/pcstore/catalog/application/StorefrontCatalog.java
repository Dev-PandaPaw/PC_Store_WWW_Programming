package iuh.fit.nhom16.pcstore.catalog.application;

import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;

import java.util.List;

/** Application-facing read port for the products visible in the storefront. */
public interface StorefrontCatalog {
    List<StoreProduct> products();
}
