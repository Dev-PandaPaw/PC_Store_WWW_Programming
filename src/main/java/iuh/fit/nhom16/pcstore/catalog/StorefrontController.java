package iuh.fit.nhom16.pcstore.catalog;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public presentation data backed by the same products used for orders and inventory. */
@RestController
public class StorefrontController {
    private final JdbcTemplate jdbc;

    public StorefrontController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping(value = "/api/catalog/storefront", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getStorefront() {
        // One statement gives products, categories and articles a consistent database snapshot.
        return jdbc.queryForObject("""
            SELECT jsonb_build_object(
              'products', COALESCE((
                SELECT jsonb_agg(jsonb_build_object(
                  'id', p.id, 'catalogKey', s.catalog_key, 'slug', p.slug, 'name', p.name,
                  'category', c.slug, 'brand', p.brand, 'description', p.description,
                  'price', p.price, 'originalPrice', GREATEST(s.original_price, p.price),
                  'specs', s.specs, 'stockOnHand', GREATEST(p.stock_on_hand - p.reserved_quantity, 0),
                  'image', (SELECT i.storage_key FROM product_images i
                            WHERE i.product_id = p.id ORDER BY i.is_cover DESC, i.sort_order, i.id LIMIT 1)
                ) ORDER BY s.sort_order, p.id)
                FROM storefront_products s JOIN products p ON p.id = s.product_id
                JOIN categories c ON c.id = p.category_id WHERE p.status = 'ACTIVE'
              ), '[]'::jsonb),
              'categories', COALESCE((
                SELECT jsonb_agg(jsonb_build_object('id', c.id, 'slug', c.slug, 'name', c.name) ORDER BY c.name)
                FROM categories c WHERE EXISTS (
                  SELECT 1 FROM products p JOIN storefront_products s ON s.product_id = p.id
                  WHERE p.category_id = c.id AND p.status = 'ACTIVE'
                )
              ), '[]'::jsonb),
              'articles', COALESCE((
                SELECT jsonb_agg(a.content - 'sources' ORDER BY a.sort_order, a.slug)
                FROM storefront_articles a WHERE a.published
              ), '[]'::jsonb)
            )::text
            """, String.class);
    }
}
