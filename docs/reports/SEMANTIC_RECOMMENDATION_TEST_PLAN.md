# Kế hoạch kiểm thử Semantic Recommendations

Ngày lập: 09/11/2026. Phạm vi: cosine, cá nhân hóa theo lịch sử xem, tồn kho/socket và API recommendation. Môi trường tích hợp là Supabase đã bật vector; không cần Docker.

## Môi trường và phạm vi dữ liệu

- Local: JDK 21/Maven và Node; không gọi Supabase hoặc Gemini.
- Supabase: đọc cấu hình `.env` hoặc `RECOMMENDATION_TEST_DB_*` nội bộ; không in credentials. DB role cần quyền tạo schema và dùng extension vector đã bật.
- Test tạo schema `recommendation_check_<UUID>`, chạy V15 tracking/V16 socket/V17 vector, seed 2 user giả và 5 linh kiện giả rồi xóa schema trong teardown. Không sửa bảng người dùng, sản phẩm, tồn kho hoặc đơn hàng thật. Nếu mất kết nối lúc teardown, cần kiểm tra schema tạm còn lại trước khi chạy lại.
- Live embedding là bước opt-in riêng: một request gửi chuỗi thông số CPU giả sang Google, không gửi lịch sử user hoặc catalog thật. Request có thể dùng quota API.
- Tích hợp ứng dụng thật: chạy profile `local,semantic`, xác nhận database mục tiêu trước migration. Không sửa lịch sử Flyway để chạy lại migration đã áp dụng.

## Ma trận ca kiểm thử

| ID | Mức | Kịch bản | Tiêu chí đạt | Bộ test |
|---|---|---|---|---|
| COS01 | Unit | Tham số truy vấn | topK=5, threshold=0,70, filter danh mục/model signature | RecommendationServiceTest |
| COS02 | Unit | Điểm đúng và dưới ngưỡng | Nhận 0,70; loại 0,6999 | RecommendationServiceTest |
| COS03 | Unit | NaN/null/>1, ID sai/trùng | Không trả kết quả lỗi/trùng | RecommendationServiceTest |
| COS04 | Adapter | SQL strict `<` của Spring AI | Nhận biên đúng ngưỡng; hậu kiểm loại dưới ngưỡng | InclusiveCosinePgVectorStoreTest |
| COS05 | PostgreSQL | [4,3,0] so với [1,0,0] | Điểm 0,8 sai số ≤1e-6; loại vector điểm 0,6 | RecommendationVectorPostgresTest |
| COS06 | PostgreSQL | Biên và số lượng | Inclusive; tối đa 5 kết quả | RecommendationVectorPostgresTest |
| CAT01 | Unit | ID đã xóa/không ACTIVE | Bị loại khỏi kết quả | RecommendationServiceTest |
| INV01 | Unit | Tồn thực 0/âm hoặc đã giữ hết | Bị loại khỏi kết quả | RecommendationServiceTest |
| INV02 | Supabase | Tăng lượng giữ sau indexing | Đọc tồn mới và loại ngay | SupabaseRecommendationIntegrationTest |
| CMP01 | Unit | CPU/mainboard cùng LGA1700 | socket_match | RecommendationServiceTest |
| CMP02 | Unit | AM5 khác LGA1700 | Loại ứng viên không tương thích | RecommendationServiceTest |
| CMP03 | Unit | Context mâu thuẫn/thiếu socket | 400 INVALID_CONTEXT | RecommendationServiceTest |
| CMP04 | Unit | Không có context hoặc socket nguồn | Không tự xác nhận compatible | RecommendationServiceTest |
| CMP05 | Unit | Socket đổi trong lúc embedding | Kiểm tra dữ liệu mới | RecommendationServiceTest |
| SIM01 | Unit | Nguồn/trùng ID/sai danh mục | Không xuất hiện trong similar | RecommendationServiceTest |
| SIM02 | Unit | Hai cận giá ±20%/ngoài cận | Nhận tại cận; loại ngoài cận | RecommendationServiceTest |
| HIS01 | Unit | Truy vấn hành vi | Đúng user, 30 ngày, tối đa 3 danh mục | ProductViewHistoryTest + RecommendationServiceTest |
| HIS02 | HTTP | Client giả category | Danh mục gợi ý lấy từ Product thật, user-scoped | ProductViewHistoryTest + kiểm thử tích hợp |
| HIS03 | Supabase | A xem CPU, B xem mainboard | Không lẫn lịch sử A/B | SupabaseRecommendationIntegrationTest |
| HIS04 | Supabase | VIEW_PRODUCT và DWELL/FILTER khác nhau | Chỉ VIEW_PRODUCT được tính cho recommendation | SupabaseRecommendationIntegrationTest |
| HIS05 | Unit | Không có lịch sử | insufficient_history, products=[] | RecommendationServiceTest |
| HIS06 | Unit | Đổi behavior-source=external | Adapter mặc định nhường contract cho provider khác | ProductViewHistoryTest |
| API01 | HTTP | Personalized không đăng nhập | 401 | RecommendationControllerTest |
| API02 | HTTP | Tài khoản bị khóa | 403, không đọc/ghi lịch sử | RecommendationControllerTest |
| API03 | HTTP | Similar public/UUID hợp lệ | Response đúng contract | RecommendationControllerTest |
| API04 | HTTP | UUID/context sai | 400 | RecommendationControllerTest |
| API05 | Unit | Nguồn không tồn tại | 404 | RecommendationServiceTest |
| API06 | HTTP | POST recommendation reindex thiếu CSRF | 403 | RecommendationControllerTest |
| API07 | HTTP | Customer reindex | 403 | RecommendationControllerTest |
| API08 | HTTP | Response cá nhân hóa | Cache-Control: no-store | RecommendationControllerTest |
| FAIL01 | Unit | Vector provider lỗi | 503, không lộ upstream/key | RecommendationServiceTest |
| IDX01 | Unit/Supabase | Index hai lần | Upsert theo ID; lần 2 không embed lại | SemanticIndexServiceTest + SupabaseRecommendationIntegrationTest |
| IDX02 | Unit | Sửa nội dung/socket hoặc ngừng bán | Đổi hash/vector; xóa stale ID | SemanticIndexServiceTest |
| IDX03 | Unit | Khóa đã được giữ | busy=true, không gọi model | SemanticIndexServiceTest |
| CFG01 | Local | Tắt semantic | Không cần key/không tạo vector bean | SemanticRecommendationConfigurationTest |
| CFG02 | Supabase | vector ở schema extensions | Migration và toán tử cosine chạy được | SupabaseRecommendationIntegrationTest |
| EMB01 | Live | Một request Google embedding | 768 giá trị hữu hạn, norm>0 | SupabaseRecommendationIntegrationTest + recommendation.live=true |
| UI01 | Thủ công | Login, xem chi tiết linh kiện | POST /api/tracking/events; tracking lỗi không chặn xem hàng | DevTools + DB |
| UI02 | Thủ công | Guest/logout/đổi trang nhanh | Không trộn lịch sử guest vào user; hủy timer cũ | DevTools |
| E2E01 | Thủ công | Xem CPU rồi gọi personalized | Đúng danh mục quan tâm, score≥0,70, còn khả dụng | HTTP JSON + DB |
| E2E02 | Thủ công | Similar CPU với mainboard đang chọn | Cùng socket; không cam kết BIOS/RAM/PSU/case | HTTP JSON + DB |

## Thứ tự và lệnh chạy

1. Unit/HTTP/configuration local; sửa mọi failure.
2. Build frontend và test catalog.
3. Supabase trong schema tạm với embedding cố định: migration, index, cosine, lịch sử và guardrails, không dùng quota model.
4. Live embedding: một request xác minh model/key; bước 3 không chứng minh quyền gọi model thật.
5. Khi migration ứng dụng được triển khai và index đồng bộ, chạy UI/E2E bằng tài khoản thử. Không đổi tồn sản phẩm thật để dựng tình huống.

```powershell
# JAVA_HOME phải trỏ tới JDK 21 nếu Java mặc định là 17.
powershell -File scripts/test-recommendations.ps1 -Mode Local
cd frontend
npm run build
npm run test:catalog
```

Các lệnh sau có tác động từ xa, chỉ chạy sau khi cho phép đúng môi trường:

```powershell
powershell -File scripts/test-recommendations.ps1 -Mode Supabase
powershell -File scripts/test-recommendations.ps1 -Mode LiveEmbedding
```

COS05/COS06: đặt `RECOMMENDATION_TEST_DB_URL`, `RECOMMENDATION_TEST_DB_USER`, `RECOMMENDATION_TEST_DB_PASSWORD` cho database test có pgvector, chạy `mvn test -Dtest=RecommendationVectorPostgresTest`.

## Nghiệm thu và trạng thái

Hoàn thành nghiệm thu khi local/build đều đạt, test Supabase schema tạm đạt, live embedding đạt, personalized đọc lịch sử thật đúng user, guardrails giữ nguyên ngưỡng/tồn/socket và UI/E2E có bằng chứng riêng. Không lấy build hoặc mock test thay bằng kiểm chứng live.

Supabase/Gemini live **chưa chạy**: cơ chế duyệt tự động từ chối tạo schema tạm từ xa và gửi nội dung thử sang Gemini bằng credentials hiện có. Cần người dùng phê duyệt bước này sau khi xem phạm vi và script; không có kết quả live nào được coi là đạt.

Kết quả sau đồng bộ development được ghi tại [báo cáo đồng bộ](DEVELOPMENT_SYNC_RESOLUTION.md). Những kết quả trước đồng bộ không thay cho regression trên commit mới.
