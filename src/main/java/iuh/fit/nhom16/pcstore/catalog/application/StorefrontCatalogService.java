package iuh.fit.nhom16.pcstore.catalog.application;

import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.StorefrontDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class StorefrontCatalogService {

    private final StorefrontDataRepository storefrontDataRepository;
    private final JsonMapper jsonMapper;

    public StorefrontCatalogService(
            StorefrontDataRepository storefrontDataRepository,
            JsonMapper jsonMapper) {
        this.storefrontDataRepository = storefrontDataRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public StorefrontResponse getStorefront(
            String category,
            String keyword,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean dealsOnly,
            String sort) {

        String normalizedCategory = normalize(category);
        String normalizedKeyword = normalize(keyword);
        String normalizedBrand = normalize(brand);

        String productsJson =
                storefrontDataRepository.findStorefrontProductsJson(
                        normalizedCategory,
                        normalizedKeyword,
                        normalizedBrand,
                        minPrice,
                        maxPrice,
                        dealsOnly,
                        sort);

        return new StorefrontResponse(
                parseJson(productsJson),
                parseJson(
                        storefrontDataRepository
                                .findStorefrontCategoriesJson()),
                parseJson(
                        storefrontDataRepository
                                .findPublishedArticlesJson())
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.equalsIgnoreCase("ALL")) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private JsonNode parseJson(String json) {
        try {
            return jsonMapper.readTree(json);
        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "Storefront repository returned invalid JSON", ex);
        }
    }

    public record StorefrontResponse(
            JsonNode products,
            JsonNode categories,
            JsonNode articles) {
    }
}
