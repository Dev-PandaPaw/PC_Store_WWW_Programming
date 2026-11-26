# Task 7 — Build PC và xác nhận thay đổi giỏ

Màn hình mới: `http://localhost:5173/build-pc`. Trong giỏ hàng có liên kết
**Hoàn thiện cấu hình từ giỏ hàng**. Trang tư vấn AI cũ vẫn ở `/advisory`.

## Cấu hình và khởi động

Đã cấu hình một khóa nội bộ riêng, giống nhau trong `.env` Python và Spring Boot.
Khóa không được in ra báo cáo hoặc đưa vào frontend. Nếu đang chạy hai dịch vụ,
khởi động lại để đọc cấu hình mới.

Python, tại `E:\SimpleAIChatBox`:

```powershell
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Spring Boot, tại `E:\PC_Store_WWW_Programming` (JDK 21):

```powershell
mvn spring-boot:run
```

Frontend, tại `E:\PC_Store_WWW_Programming\frontend`:

```powershell
npm.cmd run dev
```

Biến Spring Boot:

```dotenv
PC_BUILD_BASE_URL=http://127.0.0.1:8000
CART_BACKEND_API_KEY=<giống CART_BACKEND_API_KEY của Python>
PC_BUILD_READ_TIMEOUT_SECONDS=210
```

Đây là cấu hình server; không dùng tiền tố VITE cho khóa. Frontend chỉ gọi
Spring Boot cùng origin `/api/v1/pc-build/*`, dùng session và CSRF hiện có.
Python và Spring Boot phải dùng cùng catalog sản phẩm; nếu ID, giá hoặc
version khác nhau, bước xác nhận sẽ từ chối thay vì ghi sản phẩm sai.
Task 7 không thêm migration và không chạy migration/ghi database thật khi test.

## Luồng sử dụng

1. Thêm linh kiện vào giỏ hoặc bắt đầu từ giỏ trống; mở `/build-pc`.
2. Nhập ngân sách toàn bộ cấu hình, nhu cầu, yêu cầu GPU rời. Có thể bỏ tùy
   chọn tài liệu để test nhanh mà không gọi DeepSeek.
3. Mặc định giữ nguyên mọi món. Muốn tìm thay một món, đánh dấu rõ món đó.
   Backend lấy giỏ từ tài khoản/session, không nhận cart ID/items/owner từ trình duyệt.
4. Chọn **Tìm cấu hình**. Bước này chỉ tư vấn; không đổi giỏ.
5. Xem từng phương án, giá, số lượng, món giữ lại, dữ liệu thiếu, giới hạn tìm
   kiếm và tài liệu cho phương án đầu tiên. Khi RAG bị rejected/unavailable,
   giao diện báo phần tài liệu chưa sẵn sàng; kết quả catalog vẫn hiển thị.
6. Chọn **Xem thay đổi giỏ** để xem danh sách bỏ/thêm/cập nhật và tổng giỏ.
   Phương án provisional yêu cầu đánh dấu đã đọc các điều kiện chưa xác minh.
7. Chọn **Xác nhận cập nhật giỏ**. Backend kiểm tra lại rồi cập nhật toàn bộ
   thay đổi một lần. Thêm giỏ không tự xác nhận tương thích hoặc giữ hàng.

Đổi tài khoản, phiên bản giỏ hoặc thông tin nhu cầu sẽ hủy kết quả cũ.
Đề xuất hết hạn sau 10 phút. Giỏ thay đổi từ tab khác, sản phẩm đổi giá/version,
ngừng bán hoặc hết tồn sẽ yêu cầu lấy và xác nhận đề xuất mới.

## API Spring Boot

### POST /api/v1/pc-build/advice

```json
{
  "question": "Hoàn thiện PC và giải thích những điểm cần kiểm tra",
  "budgetVnd": "45000000",
  "requireDedicatedGpu": true,
  "includeRag": false,
  "replaceProductIds": []
}
```

`replaceProductIds` chỉ được chứa ID có trong giỏ đang thuộc session/tài khoản.
Backend sao chép giỏ bằng `CartOwnerResolver` và `CartStorageService`, thả
lock trước khi gọi Python. Chỉ món được chỉ định tìm thay mới được bỏ khỏi
danh sách khóa gửi Task 5; các món còn lại bắt buộc giữ nguyên ID/quantity.

Response gồm `advice` nguyên contract Task 6, `cartVersion` và `proposals`.
Mỗi proposal có ID ngẫu nhiên, nhóm/index tương ứng kết quả Python, hạn dùng,
dòng sản phẩm/giá/version, món bị bỏ, tổng tiền và cờ cần xác minh.
Backend kiểm tra lại tổng tiền Decimal, giới hạn 20 SKU/99 quantity và sự
bảo toàn món giữ lại trước khi cấp proposal. Không tin tổng tiền do trình duyệt gửi.

Proposal được giữ trong session server, gắn owner, cart ID/version và toàn
bộ số lượng gốc. Mỗi lần tư vấn thay batch proposal trước; session phân tán
cần session replication/sticky session phù hợp. Proposal không sống qua mất session.

### POST /api/v1/pc-build/confirm

```json
{
  "proposalId": "<ID từ response>",
  "confirmed": true,
  "acknowledgeUnverified": true
}
```

Không gửi target items, giá, user ID hoặc cart ID từ trình duyệt. Backend:

1. Kiểm tra proposal thuộc session/owner và chưa hết hạn.
2. Đọc giỏ hiện tại, so ID/version/toàn bộ quantity với bản chụp gốc.
3. Gọi `/api/internal/cart/check` để kiểm tra lại cấu hình/khả dụng, không gọi LLM.
   Xung đột kỹ thuật bị từ chối; unknown cần acknowledgement riêng.
4. Trong transaction, khóa lại giỏ theo owner, kiểm tra phiên bản lần nữa,
   khóa sản phẩm bằng `SELECT ... ORDER BY id FOR UPDATE`, đối chiếu giá,
   product version, ACTIVE và tồn sau khi trừ giữ chỗ.
5. Chỉ sau khi mọi dòng hợp lệ mới thay danh sách, tăng cart version một lần
   và lưu. Lỗi không làm xóa/thêm dở dang. Giỏ khách chưa đăng nhập chỉ được
   cập nhật trong session sau khi transaction kiểm tra sản phẩm commit thành công.
6. Xóa proposal đã dùng. Retry không tăng quantity thêm; version cũ bị chặn.

HTTP 409 cho giỏ/giá/tồn thay đổi, đề xuất hết hạn hoặc cần acknowledgement;
401 khi resolver từ chối danh tính; 403 khi thiếu CSRF; 400 khi DTO sai;
502/503 khi dịch vụ Python/config không dùng được. Response thành công là
CartResponse hiện có, được CartContext áp dụng qua cùng hàng đợi mutation.

Đây là chỉnh giỏ, không đặt hàng hoặc giữ tồn. Tương thích được đọc qua Python
trước transaction sản phẩm, chưa phải một transaction phân tán với bảng thông
số/quy tắc. Giá/tồn/product version và cart version được kiểm tra dưới lock;
checkout vẫn phải thực hiện quy tắc đặt hàng/giữ hàng hiện có.

## Kiểm thử đã thực hiện

- Toàn bộ Maven: 113 test, 94 PASS, 19 skip theo điều kiện sẵn có, 0 lỗi.
  Có 30 test mới cho client HTTP cục bộ, service, controller/CSRF, stock/giá,
  stale/replay/owner, thay giỏ nguyên khối và lỗi commit giỏ khách.
- Frontend: 19 test PASS, gồm 4 test mới cho preview/acknowledgement/expiry.
- Vite production build thành công.
- Edge headless desktop/mobile: render, lấy đề xuất không đổi giỏ, preview
  không đổi giỏ, xác nhận unknown bắt buộc, chỉ một request commit, cập nhật
  giỏ và bỏ đề xuất cũ; không có runtime exception. API trong test trình duyệt
  được giả lập; không coi đây là kiểm thử ghi giỏ database thật.
- Test backend cũ đã sửa import catalog theo package đang dùng; không thay
  assertion và không sửa ProductCard đang được người dùng chuyển TSX → JSX.

Lệnh test tại root Spring Boot:

```powershell
mvn -o test
```

Lệnh test frontend:

```powershell
node --test scripts/pc-build.test.mjs scripts/optimistic-cart.test.mjs scripts/login-cart.test.mjs scripts/cart-stock.test.mjs
npm.cmd run build
```

Ảnh và báo cáo trình duyệt nằm trong `E:\SimpleAIChatBox\reports\task7`.
Chưa chạy nghiệm thu ghi giỏ khách hàng/database thật; việc đó thuộc Task 8
với tài khoản và dữ liệu kiểm thử riêng. Hạn chế dẫn chứng DeepSeek ở Task 6
chưa được sửa trong Task 7; fallback vẫn giữ nguyên.


## Cập nhật nghiệm thu Task 8

Đã nghiệm thu ghi/rollback giỏ và concurrency trên PostgreSQL riêng, controller/
service Spring thật gọi HTTP sang Python thật, dùng principal và LLM fixture
kiểm soát. 11 test tích hợp mới PASS, 174 Python PASS, Maven 105 PASS/19 skip,
21 frontend PASS. UI có kiểm tra phục hồi sau HTTP 409 bằng API giả lập.

DeepSeek thật cũng đã trả 4 dẫn chứng hợp lệ sau sửa whitespace, vẫn giữ
needs_verification khi catalog thiếu dữ liệu. Không ghi giỏ khách hàng thật.
Hướng dẫn chạy lại và ranh giới test: [TASK8_ACCEPTANCE.md](TASK8_ACCEPTANCE.md).
