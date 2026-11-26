package iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence;

import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

@Repository
public class JdbcRecommendationCatalog implements RecommendationCatalog {
    private static final String SELECT = """
            SELECT p.id,p.slug,p.name,p.brand,p.description,p.price,p.status,p.stock_on_hand,p.reserved_quantity,
                   c.slug AS category,s.catalog_key,s.original_price,s.specs::text,
                   COALESCE(s.provenance->'technicalSpecs','[]'::jsonb)::text AS technical_specs,
                   COALESCE((s.provenance->>'demoCatalog')::boolean,false) AS demo_catalog,
                   COALESCE(to_jsonb(cs)->>'socket',to_jsonb(cs)->'specs'->>'socket') AS socket,
                   to_jsonb(cs)::text AS component_data,
                   (SELECT storage_key FROM product_images WHERE product_id=p.id
                    ORDER BY is_cover DESC,sort_order,id LIMIT 1) AS image
            FROM products p JOIN categories c ON c.id=p.category_id
            JOIN storefront_products s ON s.product_id=p.id
            LEFT JOIN product_component_specs cs ON cs.product_id=p.id
            """;
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json;
    public JdbcRecommendationCatalog(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbc); this.json = json;
    }
    @Override public Map<UUID, Item> findAll(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<UUID, Item> found = new LinkedHashMap<>();
        query(" WHERE p.id IN (:ids)", Map.of("ids", ids)).forEach(item -> found.put(item.id(), item));
        return found;
    }
    @Override public List<Item> indexableProducts() {
        return query(" WHERE p.status='ACTIVE' AND c.slug IN "
                + "('cpu','mainboard','vga','ram','storage','cooling','case','psu','monitor','mouse','keyboard') ORDER BY p.id", Map.of());
    }
    private List<Item> query(String where, Map<String, ?> parameters) {
        return jdbc.query(SELECT + where, parameters, (rs, row) -> {
            UUID id = rs.getObject("id", UUID.class);
            int stock = rs.getInt("stock_on_hand"), reserved = rs.getInt("reserved_quantity");
            long price = rs.getBigDecimal("price").longValueExact();
            var product = new StoreProduct(id.toString(), rs.getString("catalog_key"), rs.getString("slug"),
                    rs.getString("name"), rs.getString("category"), rs.getString("brand"), rs.getString("description"),
                    price, Math.max(price, rs.getBigDecimal("original_price").longValueExact()),
                    json.readValue(rs.getString("specs"), new TypeReference<List<StoreProduct.Spec>>() {}),
                    Math.max(0, stock-reserved), rs.getString("image"),
                    json.readValue(rs.getString("technical_specs"),new TypeReference<List<StoreProduct.TechnicalSpec>>() {}),rs.getBoolean("demo_catalog"));
            String raw=rs.getString("component_data");
            Map<String,Object> data=raw==null ? Map.of() : json.readValue(raw,new TypeReference<Map<String,Object>>() {});
            Map<String,Object> facts=data.get("specs") instanceof Map<?,?> values
                    ? json.convertValue(values,new TypeReference<Map<String,Object>>() {}) : Map.of();
            return new Item(id, product, rs.getString("status"), stock, reserved, rs.getString("socket"),
                    facts, Objects.toString(data.get("verification_status"),"unknown"));
        });
    }
}
