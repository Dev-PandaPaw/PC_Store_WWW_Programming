# Gợi ý linh kiện bằng cosine similarity

## Kiến trúc sau đồng bộ development

Đã đồng bộ commit `6662256`. Giữ hook `useProductViewTracker`, endpoint `/api/tracking/events`, bảng `user_activity_logs` và `UserTrackingService` từ development. Không tạo collector/bảng lịch sử riêng.

`RecommendationController` → `ProductSemanticRecommendationService` → `ProductVectorStore` → PgVector khi bật profile `semantic`.

- Chỉ một `EmbeddingModel`, dùng `VectorStoreConfig`/`ProductEmbeddingModel` của development: kiểm tra key, số chiều, vector hữu hạn/khác 0, lỗi quota và timeout, không sinh vector giả.
- Chỉ một bean `VectorStore`: profile thường dùng SimpleVectorStore để giữ luồng demo 3.3; profile semantic dùng PgVector được bọc trong cùng ProductVectorStore.
- Recommendation yêu cầu profile semantic. Ngưỡng cosine **≥0,70**, topK=5, không hạ ngưỡng. InclusiveCosinePgVectorStore xử lý biên strict SQL của Spring AI 2.0.2.
- Đọc lại catalog sau embedding: ACTIVE, tồn thực>0, lượng khả dụng sau giữ>0, socket hiện tại. Không dùng giá/tồn trong metadata làm nguồn sự thật.
- Similar cùng danh mục, loại sản phẩm nguồn và giá hiện tại trong ±20%; cấu hình RECOMMENDATION_PRICE_TOLERANCE. Similarity mô tả không phải benchmark.

## Supabase, không cần Docker

Supabase phải bật extension vector. Backend dùng DB_URL/DB_USERNAME/DB_PASSWORD và GEMINI_API_KEY trong .env; không đưa key vào frontend. Profile semantic tự thêm namespace thật của extension vào search_path cho kết nối, hỗ trợ schema extensions.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,semantic"
```

Dùng chung `GEMINI_EMBEDDING_MODEL` (mặc định gemini-embedding-001), `GEMINI_EMBEDDING_DIMENSIONS=768` với development. PgVector yêu cầu đúng 768 chiều; cấu hình khác sẽ fail sớm. Model dùng SEMANTIC_SIMILARITY cho cả document/query.

Migration không trùng số:

- V15__create_user_activity_logs.sql: giữ nguyên từ development.
- V16__component_socket_specs.sql: thêm socket CPU/mainboard có cấu trúc, chưa rõ để NULL.
- V17__semantic_vector_store.sql: chỉ bật trong profile semantic, tạo product_semantic_vectors với 768 chiều/HNSW cosine và RLS.

Không đổi lịch sử Flyway của database đã triển khai. Nếu đã tự chạy bản migration cũ trước lần đồng bộ này, kiểm tra flyway_schema_history trước khi áp bản mới.

## Một pipeline đồng bộ trên mỗi profile

- Profile thường giữ chunking, snapshot in-memory và endpoint reindex hiện có của development.
- Profile semantic: cả POST /api/admin/vector/reindex và POST /api/v1/admin/recommendations/reindex cùng gọi ProductSemanticIndexService, dùng chung bảng/vector/signature và khóa PostgreSQL.
- Index một document ngắn/mỗi linh kiện; ID là productId; hash giúp chỉ embed nội dung thay đổi. Xóa vector stale. Giao dịch DB rollback nếu đồng bộ lỗi.
- Không tự gọi model khi khởi động mặc định. Muốn bật lịch đồng bộ: RECOMMENDATION_AUTO_SYNC=true; bắt đầu sau 30 giây, lặp mỗi RECOMMENDATION_SYNC_DELAY_MS (mặc định 300000).
- Tồn kho được kiểm tra lại mỗi request. Mô tả trong chỉ mục chỉ mới sau lần đồng bộ thành công; đổi model cần reindex, signature cũ không được phục vụ.

## Cá nhân hóa dùng tracking của Nhân

`RecentCategoryConfiguration` nối `RecentCategoryProvider.topCategories(userId,since,limit)` vào `UserTrackingService.getTopViewedCategories`.

Truy vấn chỉ lấy VIEW_PRODUCT trong 30 ngày của đúng user hiện tại; tối đa 3 nhóm, xếp theo số lượt xem rồi thời gian gần nhất. Danh mục lấy từ Product thật khi truy vấn, không tin categorySlug client, không trộn session/user khác. Guest/dwell/filter/search vẫn được tracking cũ hỗ trợ nhưng không tự coi là lượt xem của user đăng nhập.

Mặc định RECOMMENDATION_BEHAVIOR_SOURCE=tracking. Có thể chọn external và cung cấp provider khác. Không có lịch sử trả insufficient_history; thiếu provider external trả 503. Không có lịch sử giả.

## API và guardrails

- GET /api/v1/recommendations/personalized: user từ session, kiểm tra ACTIVE, không nhận userId client làm danh tính.
- GET /api/v1/recommendations/similar/{productId}: public; nguồn phải là linh kiện ACTIVE thuộc storefront.
- Cả hai nhận selectedProductIds, tối đa 20 UUID. Server đọc lại socket/giá; context mâu thuẫn hoặc thiếu socket trả 400.
- CPU/mainboard phải khớp socket khi có context; thiếu dữ liệu không xác nhận compatible. Similar không có build thì giữ socket nguồn.
- socket_match chỉ là kiểm tra socket; chưa xác nhận BIOS/DDR/PSU/kích thước case. Không có context trả needs_verification, nhóm khác not_checked.
- Kết quả có thể ít hơn 5 sau guardrails; response no-store. Không giữ hàng/đặt hàng, checkout vẫn kiểm tra lại.

## Kiểm thử

Xem [kế hoạch kiểm thử](reports/SEMANTIC_RECOMMENDATION_TEST_PLAN.md), [báo cáo đồng bộ](reports/DEVELOPMENT_SYNC_RESOLUTION.md) và script scripts/test-recommendations.ps1.

Mode Local không gọi Supabase/Gemini. Mode Supabase tạo/xóa schema tạm, kiểm tra migrations/cosine/index/history/guardrails với embedding cố định. Mode LiveEmbedding thêm đúng một request model thật. Kiểm tra từ xa chưa được thực hiện vì chưa có phê duyệt cho tác động từ xa; không dùng test mock thay bằng bằng chứng live.
