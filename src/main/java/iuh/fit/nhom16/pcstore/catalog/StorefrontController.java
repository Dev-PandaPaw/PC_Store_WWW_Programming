package iuh.fit.nhom16.pcstore.catalog;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorefrontController {
    private final StorefrontCatalog catalog;
    public StorefrontController(StorefrontCatalog catalog) { this.catalog = catalog; }

    @GetMapping(value = "/api/catalog/storefront", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getStorefront() { return catalog.snapshot(); }
}
