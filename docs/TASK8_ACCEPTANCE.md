# Task 8 — Nghiệm thu tự động

Báo cáo máy đọc: `reports/task8/acceptance.json`. Bộ chạy lặp lại nằm ở
`integration/task8/run_acceptance.py`. Kết quả hiện tại: PASS.

## Phạm vi đã kiểm tra

Luồng backend dùng controller, identity resolver, giỏ hàng, transaction và
inventory guard thật của Spring, gọi HTTP sang các route Python thật và bộ
tìm/kiểm tra tương thích thật. Catalog thử nghiệm nằm trong PostgreSQL 16
chạy container riêng, chỉ bind loopback, cổng ngẫu nhiên, tài khoản/mật khẩu
ngẫu nhiên. LLM được điều khiển cục bộ để kiểm tra cả đầu ra hợp lệ và lỗi.

Không sử dụng database Supabase/giỏ khách hàng khi chạy bộ tự động. Mỗi lần
chạy tạo một container riêng và dọn đúng container đó cùng process Python
fixture sau khi kết thúc. Không đổi cấu hình DB của website.

Adapter projection sản phẩm trong test đọc PostgreSQL bằng JDBC; không
khởi động toàn bộ JPA/production login của website. Request MockMvc dùng
principal tổng hợp đúng loại của ứng dụng, resolver kiểm tra ACTIVE từ DB.
Test trình duyệt riêng dùng API giả lập, không gọi UI để ghi giỏ khách thật.

## Ma trận nghiệm thu

| Tình huống | Kết quả |
|---|---|
| Tư vấn với tài liệu hợp lệ | Nguồn/trích đoạn được kiểm tra, chưa tự đổi giỏ |
| Xác nhận cấu hình | 8 dòng được lưu vào PostgreSQL, cart version tăng đúng một lần |
| Gửi lại cùng xác nhận | 409, không tăng quantity hoặc ghi lại |
| Dùng proposal của owner/session khác | 409 trước khi đọc/ghi giỏ tài khoản kia |
| Thiếu CSRF / principal không còn ACTIVE | 403 / 401, không gọi tư vấn để thay giỏ |
| Giỏ thay đổi sau tư vấn | 409, giữ nguyên dữ liệu mới |
| Giá/product version thay đổi | 409, yêu cầu xem và xác nhận lại |
| Tồn khả dụng bị giữ hết | Không commit |
| CPU/mainboard có socket xung đột | Không commit |
| Thông số draft/thiếu bằng chứng | needs_verification; phải acknowledge, không nâng thành compatible |
| Ngân sách thấp hơn món giữ lại | over_budget, không có proposal ghi giỏ |
| LLM tạo product ID lạ | rejected, không dùng nguồn sai hoặc đổi giá |
| LLM timeout | unavailable, giữ kết quả tính toán và giỏ |
| Constraint DB lỗi khi đang ghi nhiều món | Rollback toàn bộ dòng và version, giữ CPU ban đầu |
| Hai request xác nhận đồng thời | Một 200, một 409; không cộng trùng hoặc mất cập nhật |
| Tồn đổi đúng lúc xác nhận chờ product row lock | Quan sát wait_event_type=Lock trên PostgreSQL; sau commit writer, xác nhận trả 409 và không sửa giỏ |
| UI nhận 409 sau bước xem trước | Đồng bộ giỏ, bỏ đề xuất cũ; người dùng lấy và xác nhận đề xuất mới |

## Kết quả kiểm thử

- Spring/Maven: 124 test, 105 PASS, 19 skip theo điều kiện cũ, 0 lỗi.
  Trong đó **11 test nghiệm thu PostgreSQL–Python mới chạy thật, không skip**.
- Python: **174 PASS**, gồm 5 test mới về khoảng trắng và xuất xứ trích dẫn.
- Frontend: **21 PASS**.
- Edge/Vite development: PASS, không runtime exception; kiểm tra phiên bản
  giỏ thay đổi giữa preview và confirm, phục hồi sau 409, acknowledgement và
  chỉ xác nhận sau hành động người dùng. Báo cáo là API giả lập ở phía UI.

19 test skip không được tính là PASS; chúng gồm các test login/order DB và
AI HTTP tùy chọn đã có từ trước. Không dùng kết quả này để tuyên bố mọi test
production login hoặc mọi dịch vụ AI đều đã được chạy.

## Sửa lỗi trích đoạn RAG trong nghiệm thu

PDF thường xuống dòng giữa một câu; mô hình trả cùng câu với dấu cách. Hàm
`resolve_source_quote()` chỉ cho phép khác biệt về whitespace, sau đó trả
**đúng span từ nội dung nguồn**, bao gồm newline/khoảng trắng gốc.

Không cho phép fuzzy match chữ, số, dấu câu hoặc đổi hoa/thường. Các kiểm tra
ID, source reference, tiền tệ, key/schema và link trùng vẫn giữ nguyên. Trích
đoạn URL/danh mục References/Bibliography bị từ chối để không dùng một mục
liệt kê tài liệu như bằng chứng kỹ thuật. Prompt cũng yêu cầu tránh chúng.

Unit test xác nhận đổi 650 thành 750, CPU thành GPU, dấu câu hoặc case vẫn
bị từ chối. Test PostgreSQL–Python xác nhận model gộp whitespace vẫn trả lại
nguyên văn có newline; kết luận tương thích và tổng tiền vẫn do code quyết định.

Validator chứng minh xuất xứ và định dạng, chưa tự chứng minh mọi dẫn chứng
hỗ trợ check về ngữ nghĩa. Trạng thái grounded không thay cho đánh giá chuyên
môn hoặc chứng minh một build bất kỳ sẽ chạy tốt ngoài các phép kiểm tra đã có.

## Chạy lại

Yêu cầu Docker Desktop, image `postgres:16-alpine`, JDK 21, Maven/dependency
đã có, Node, Edge và các thư viện Python hiện có. Cài thêm driver chỉ cho test:

```powershell
.\.venv\Scripts\python.exe -m pip install -r integration/task8/requirements-test.txt
```

Nếu Docker chưa chạy, khởi động Docker Desktop. Bộ chạy không pull image tự
động; dùng image PostgreSQL 16 cục bộ đã có trong lần nghiệm thu này.

```powershell
.\.venv\Scripts\python.exe -u integration/task8/run_acceptance.py
```

Bộ chạy tạo fixture từ thông số tổng hợp trong test, chạy Maven (bao gồm lớp
`PcBuildAcceptanceTest` được đồng bộ sang dự án Spring), chạy unittest,
test frontend và trình duyệt. Không chạy `fixture_api.py` như server sản phẩm;
nó yêu cầu TASK8_FIXTURE=1 và chỉ chấp nhận DB local có tên task8_acceptance.
`main.py` không import fixture này.

Sau mỗi lần chạy xem `acceptance.json` và các log cùng thư mục. Khi không có
TASK8_FIXTURE, `PcBuildAcceptanceTest` được skip để `mvn test` bình thường
không tự yêu cầu container hoặc vô tình dùng DB sản phẩm.

## Kiểm thử DeepSeek thật

`integration/task8/live_deepseek.py` là lệnh tách riêng, không được runner
tự động gọi. Lệnh này gửi câu hỏi mẫu, mã check/ID sản phẩm và đoạn PDF từ
Supabase đến DeepSeek; chỉ chạy trong phạm vi người dùng đã cho phép.
Không gửi bản chụp giỏ khách hàng hoặc ghi database. Báo cáo riêng là
`reports/task8/deepseek-live.json`; không đánh đồng với LLM giả lập.

Đã chạy lại DeepSeek thật trong phạm vi được người dùng cho phép: HTTP 200,
rag_status=grounded, **4 dẫn chứng từ 4 đoạn nguồn thuộc 2 PDF**, không cảnh
báo. Tất cả evidence_quote được đối chiếu là substring nguyên văn của đúng
chunk trả về; newline của PDF được giữ. Build vẫn needs_verification và
không có recommendation đã xác nhận vì catalog chưa đủ thông số verified.
Lần đo này model mất khoảng 108 giây, toàn API khoảng 144 giây; UI vẫn mặc
định tắt RAG để không buộc mọi lần tìm cấu hình chờ mô hình.

Smoke test live chỉ đọc catalog/tài liệu Supabase, không ghi giỏ/database.
`acceptance.json` mô tả runner cô lập (không gọi LLM ngoài); báo cáo live ở
file riêng. Tổng hợp cả hai phạm vi nằm trong `reports/task8/summary.json`.

## Giới hạn nghiệm thu

Đây là nghiệm thu tự động trên dữ liệu tổng hợp và PostgreSQL riêng. Chưa
chạy hành động đặt hàng, thanh toán, gửi email, deployment, hoặc ghi giỏ tài
khoản khách hàng thật. Cần giữ quy trình checkout kiểm tra giá/tồn như cũ.
Catalog sản phẩm hiện thiếu thông số verified vẫn phải trả provisional;
test không tự xác minh hoặc điền thông số giả vào Supabase.
