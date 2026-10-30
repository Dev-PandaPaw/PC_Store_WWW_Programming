package iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence;

import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

/** PostgreSQL JSON projections for the storefront, executed through Spring Data JPA. */
public interface StorefrontDataRepository extends Repository<Product, UUID> {

    @Query(value = """
            SELECT COALESCE(jsonb_agg(filtered.product ORDER BY filtered.position), '[]'::jsonb)::text
            FROM (
              SELECT jsonb_build_object(
                'id', p.id, 'catalogKey', s.catalog_key, 'slug', p.slug, 'name', p.name,
                'category', c.slug, 'brand', p.brand, 'description', p.description,
                'price', p.price, 'originalPrice', GREATEST(s.original_price, p.price),
                'specs', s.specs, 'stockOnHand', GREATEST(p.stock_on_hand - p.reserved_quantity, 0),
                'image', (SELECT i.storage_key FROM product_images i
                          WHERE i.product_id = p.id ORDER BY i.is_cover DESC, i.sort_order, i.id LIMIT 1)
              ) AS product,
              ROW_NUMBER() OVER (ORDER BY
                CASE WHEN :sort = 'price-asc' THEN p.price END ASC,
                CASE WHEN :sort = 'price-desc' THEN p.price END DESC,
                CASE WHEN :sort = 'name' THEN p.name END ASC,
                s.sort_order ASC, p.id
              ) AS position
              FROM storefront_products s
              JOIN products p ON p.id = s.product_id
              JOIN categories c ON c.id = p.category_id
              WHERE p.status = 'ACTIVE'
                AND (
                  CAST(:category AS text) IS NULL
                  OR CASE
                      WHEN :category = 'pc' THEN c.slug IN ('pc-gaming', 'pc-office', 'pc-workstation')
                      WHEN :category = 'components' THEN c.slug IN ('cpu', 'mainboard', 'vga', 'ram', 'storage', 'cooling', 'case', 'psu')
                      WHEN :category = 'memory' THEN c.slug IN ('ram', 'storage')
                      WHEN :category = 'build' THEN c.slug IN ('cooling', 'case', 'psu')
                      WHEN :category = 'gear' THEN c.slug IN ('keyboard', 'mouse')
                      ELSE LOWER(c.slug) = LOWER(:category)
                    END
                )
                AND (CAST(:keyword AS text) IS NULL OR
                     CONCAT_WS(' ', p.name, p.sku, p.brand, s.specs::text) ILIKE CONCAT('%', :keyword, '%'))
                AND (CAST(:brand AS text) IS NULL OR COALESCE(p.brand, '') ILIKE CONCAT('%', :brand, '%'))
                AND (CAST(:minPrice AS numeric) IS NULL OR p.price >= :minPrice)
                AND (CAST(:maxPrice AS numeric) IS NULL OR p.price <= :maxPrice)
                AND (NOT :dealsOnly OR s.original_price > p.price)
              ORDER BY
                CASE WHEN :sort = 'price-asc' THEN p.price END ASC,
                CASE WHEN :sort = 'price-desc' THEN p.price END DESC,
                CASE WHEN :sort = 'name' THEN p.name END ASC,
                s.sort_order ASC, p.id
            ) filtered
            """, nativeQuery = true)
    String findStorefrontProductsJson(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("dealsOnly") boolean dealsOnly,
            @Param("sort") String sort
    );

    @Query(value = """
            SELECT COALESCE(jsonb_agg(jsonb_build_object('id', c.id, 'slug', c.slug, 'name', c.name)
                                     ORDER BY c.name), '[]'::jsonb)::text
            FROM categories c
            WHERE EXISTS (
              SELECT 1 FROM products p JOIN storefront_products s ON s.product_id = p.id
              WHERE p.category_id = c.id AND p.status = 'ACTIVE'
            )
            """, nativeQuery = true)
    String findStorefrontCategoriesJson();

    @Query(value = """
            SELECT COALESCE(jsonb_agg(a.content - 'sources' ORDER BY a.sort_order, a.slug), '[]'::jsonb)::text
            FROM storefront_articles a
            WHERE a.published
            """, nativeQuery = true)
    String findPublishedArticlesJson();
}
