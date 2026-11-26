# Kiểm tra lưu vector trên Supabase — 11/11/2026

Đã chuyển backend đang chạy sang profile `local,semantic`, gọi API đồng bộ với Gemini thật và kiểm tra dữ liệu sau khi transaction hoàn tất.

## Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| POST /api/admin/vector/reindex | SUCCESS, productCount=30, documentCount=30 |
| GET /api/admin/vector/documents | HTTP 200, storage=pgvector, total=30 |
| Bảng Supabase | public.product_semantic_vectors |
| Số dòng kiểm tra trực tiếp bằng JDBC | 30 |
| Embedding không null | 30 |
| Số chiều nhỏ nhất / lớn nhất | 768 / 768 |
| Số productId phân biệt | 30 |
| Kiểm tra các giá trị qua API | Toàn bộ hữu hạn, mỗi vector không toàn 0 |
| Text và metadata.productId | Có trên toàn bộ 30 document |
| Backend / frontend | Đang chạy ở 8080 / 5173; frontend trả HTTP 200 |

Catalog có 44 sản phẩm ACTIVE, nhưng nhánh semantic hiện có 30 sản phẩm đủ điều kiện danh mục và liên kết storefront để lập chỉ mục. Vì vậy số dòng persistent khác với 44 document của nhánh RAM trước đó.

## Sửa lỗi cần thiết để chạy đồng bộ

Schema Supabase thực tế lưu thông số ở `product_component_specs.specs` dạng JSON; code cũ đọc trực tiếp cột `socket` nên đồng bộ dừng với SQL error trước khi gọi model.

Đã sửa [JdbcRecommendationCatalog.java](../../src/main/java/iuh/fit/nhom16/pcstore/catalog/infrastructure/persistence/JdbcRecommendationCatalog.java) để hỗ trợ cả hai cấu trúc:

```sql
COALESCE(to_jsonb(cs)->>'socket',
         to_jsonb(cs)->'specs'->>'socket') AS socket
```

Không thay cấu trúc bảng thông số và không sửa lịch sử Flyway của Supabase.

Đã chạy hai test trong [SupabaseRecommendationIntegrationTest.java](../../src/test/java/iuh/fit/nhom16/pcstore/advisory/SupabaseRecommendationIntegrationTest.java):

- Luồng history/index/cosine/guardrails với schema socket cũ.
- Đọc catalog với schema socket JSON, bao gồm trường hợp chưa có giá trị socket.

**2 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS.** Các test dùng schema riêng và embedding kiểm thử; kết quả dữ liệu public 30 dòng ở trên được xác minh riêng từ lượt đồng bộ thật.

## Tự xem lại trên Supabase

```sql
SELECT count(*) AS document_count,
       min(json_array_length(embedding::text::json)) AS min_dimensions,
       max(json_array_length(embedding::text::json)) AS max_dimensions
FROM public.product_semantic_vectors;

SELECT id, content, metadata, embedding
FROM public.product_semantic_vectors
ORDER BY id
LIMIT 5;
```

## Chạy lại backend

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,semantic"
```

Chạy profile local đơn lẻ sẽ chọn kho RAM; dữ liệu đã lưu trong Supabase vẫn còn. Không cần reindex khi chỉ muốn đọc dữ liệu đã có. Nếu thay đổi sản phẩm thì gọi đồng bộ lại để cập nhật chỉ mục.

Log và snapshot đầy đủ nằm trong thư mục tmp của workspace:

- `tmp/vector-supabase-sync.log`
- `tmp/vector-supabase-database-proof.log`
- `tmp/vector-supabase-catalog-tests.log`
- `tmp/vector-supabase-live.json` — dữ liệu đọc thực tế từ API, gồm đủ 768 số mỗi vector.
