package iuh.fit.nhom16.pcstore.catalog.web;

import iuh.fit.nhom16.pcstore.catalog.application.StorefrontCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

@RestController
public class StorefrontController {
    private final StorefrontCatalogService storefrontCatalogService;

    public StorefrontController(StorefrontCatalogService storefrontCatalogService) {
        this.storefrontCatalogService = storefrontCatalogService;
    }

    @GetMapping(value = "/api/catalog/storefront", produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontCatalogService.StorefrontResponse getStorefront(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String deals,
            @RequestParam(defaultValue = "featured") String sort
    ) {
        validate(minPrice, maxPrice, sort, deals);

        return storefrontCatalogService.getStorefront(
                category, keyword, brand, minPrice, maxPrice,
                isTrue(deals), sort);
    }

    private void validate(BigDecimal minPrice, BigDecimal maxPrice, String sort, String deals) {
        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng giá không hợp lệ");
        }
        if (!Set.of("featured", "price-asc", "price-desc", "name").contains(sort)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sort chỉ nhận featured, price-asc, price-desc hoặc name");
        }
        if (deals != null && !Set.of("1", "0", "true", "false").contains(deals.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deals chỉ nhận 1, 0, true hoặc false");
        }
    }

    private boolean isTrue(String value) {
        return value != null && (value.equals("1") || value.equalsIgnoreCase("true"));
    }
}
