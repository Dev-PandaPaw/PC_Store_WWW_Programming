# Đồng bộ development và giải quyết conflict

Ngày: 09/11/2026. Nền local ban đầu: cf47f3b. Đã fast-forward tới development/GitHub: **6662256**. Commit remote được kiểm tra lại sau xử lý và vẫn là 6662256.

## Bảo toàn dữ liệu

Lệnh pull/fetch chạy từ 21:44 bị kẹt và giữ development.lock. Đã xác minh command line, dừng đúng nhóm tiến trình đó và xóa lock tương ứng. Toàn bộ thay đổi ban đầu, gồm file untracked, được giữ trong stash `codex/pre-sync-semantic-20261109`, commit **e331ae477012a7004366e8e828b18c52c30435d5**. Stash chưa bị xóa.

## Quyết định giữ và hợp nhất

| Vị trí | Quyết định | Lý do |
|---|---|---|
| ProductDetailPage.jsx | Giữ useProductViewTracker từ development; bỏ collector cục bộ thứ hai | Đã hỗ trợ view/dwell và có service backend tương ứng; tránh ghi hai luồng |
| useUserTracker.js | Giữ hook hiện có, trì hoãn view một tick và hủy khi cleanup | Tránh view trùng từ effect bị StrictMode loại bỏ, giữ dwell timer |
| pom.xml | Giữ embedding/vector-store/Surefire của development; thêm pgvector | Không khai báo trùng dependency, giữ test runner của nhóm |
| Tracking | Dùng UserTrackingService/user_activity_logs; bỏ ProductViewHistoryService/product_view_events riêng | Nối trực tiếp phần của Nhân, không duy trì hai nguồn lịch sử |
| Cá nhân hóa | Thêm truy vấn VIEW_PRODUCT theo đúng user/window/limit, category từ Product thật | Không trộn session khác hoặc tin categorySlug client |
| Embedding | Dùng chung VectorStoreConfig/ProductEmbeddingModel của development | Có kiểm tra key/vector/chiều/quota/timeout, không sinh vector giả |
| VectorStore | Một bean theo profile: memory khi thường, PgVector khi semantic | Không lỗi NoUniqueBeanDefinition; giữ demo cũ và hỗ trợ Supabase |
| Reindex | Hai endpoint admin dùng cùng persistent index khi semantic bật | Không có hai chỉ mục/schema metadata mâu thuẫn |
| Auto sync | Mặc định tắt, opt-in RECOMMENDATION_AUTO_SYNC=true | Không gọi model hoặc dùng quota ngoài ý muốn lúc khởi động |
| Flyway | V15 tracking của development; V16 socket; V17 vector theo semantic | Không trùng version; có test kiểm tra resource đóng gói |
| Guardrails | Giữ cosine >=0,70, tồn khả dụng, giá và socket đọc lại sau inference | Kết quả model/index không thay thế dữ liệu nghiệp vụ hiện tại |

Không đổi lịch sử migration trên Supabase, không chạy migration/live inference trong tác vụ đồng bộ này. Nếu môi trường đã chạy file migration thử cũ, cần đối chiếu flyway_schema_history trước triển khai.

## Kiểm chứng

- Đã chạy `mvn clean test` để loại resource migration cũ khỏi target. Sau bổ sung bài test chống trùng migration, chạy lại toàn bộ suite: **189 test, 152 đạt, 37 skip, 0 failure/error**.
- Test cấu hình xác nhận đúng một EmbeddingModel và một VectorStore ở cả profile thường/semantic.
- Test reindex xác nhận endpoint cũ dùng persistent index chung và trả conflict khi khóa bận.
- Frontend build thành công; catalog **2/2**, tracking **4/4**, gồm test thực thi hook cho StrictMode và đổi sản phẩm.
- Không chạy Supabase/Gemini thật; các nhóm yêu cầu môi trường ngoài được skip có chủ đích. Đây là regression local sau đồng bộ, không phải nghiệm thu live.
- Tại thời điểm giải quyết conflict, phần tính năng được giữ trong working tree để review; commit/push được thực hiện theo yêu cầu riêng của người dùng sau đó.
