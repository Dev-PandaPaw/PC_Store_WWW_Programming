# PC Store — Đặc tả 12 chức năng chính

**Phiên bản 2.1 — 26/09/2026.** Phạm vi giữ nguyên 7 module, 24 màn hình. Tài liệu nhóm thao tác nhỏ thành 12 use case phục vụ báo cáo; số use case không phải số màn hình hay số API.

Các thao tác thêm/sửa/xóa, OTP, callback và kiểm tra dữ liệu là **luồng con**, không phải tính năng độc lập cần tách thành hàng chục use case. Nguồn: phiếu đề tài SRC01, yêu cầu môn học SRC02 và PLAN/UI đã thu gọn SRC03; xem [quy tắc nghiệp vụ](BUSINESS_RULES.md).

## Danh mục

| Mã | Chức năng chính | Actor | Module | Màn hình |
|---|---|---|---|---|
| [UC01](#uc01) | Tài khoản và xác thực | Guest, Customer | identity | A01–A05, C01 |
| [UC02](#uc02) | Tìm và xem PC | Guest, Customer | catalog | S01–S03 |
| [UC03](#uc03) | Tư vấn chọn PC | Guest, Customer | advisory | S04 |
| [UC04](#uc04) | Quản lý giỏ hàng | Guest, Customer | cart | S05; nút thêm tại S02–S04 |
| [UC05](#uc05) | Đặt hàng và thanh toán | Customer | sales, payment | C02–C03 |
| [UC06](#uc06) | Theo dõi và hủy đơn của tôi | Customer | sales | C04–C05 |
| [UC07](#uc07) | Quản lý PC và loại sản phẩm | Admin | catalog | M01–M03 |
| [UC08](#uc08) | Xử lý đơn hàng | Admin | sales, payment | M04–M05 |
| [UC09](#uc09) | Quản lý tài khoản khách hàng | Admin | identity | M06 |
| [UC10](#uc10) | Tra cứu bảo hành và tiến độ | Customer | aftersales | C05–C07 |
| [UC11](#uc11) | Gửi hoặc hủy yêu cầu bảo hành | Customer | aftersales | C07 |
| [UC12](#uc12) | Tiếp nhận và xử lý bảo hành | Admin | aftersales | M07 |

## Quy ước đọc và xử lý lỗi chung

Mỗi bước Bn dùng cùng số trong activity. Nhánh E-Bn là nhánh lỗi/thay thế tại bước đó; khi kết thúc nhánh lỗi, người dùng có thể sửa và gửi lại thao tác từ đầu. Quyền và dữ liệu riêng được kiểm phía server; không có side effect trước khi kiểm điều kiện. Mutation lỗi trước commit rollback toàn bộ. Lỗi media/email sau commit không biến kết quả đã lưu thành thất bại giả.

REST: 401 thiếu phiên, 403 thiếu quyền/CSRF, 404 tài nguyên không có hoặc không thuộc chủ, 422 sai form, 409 sai trạng thái/version/trùng/thiếu tồn, 503 dịch vụ ngoài không khả dụng. MVC hiện cùng lỗi theo field hoặc flash message. GET chỉ đọc; thao tác ghi dùng POST/PATCH/DELETE và CSRF, ngoại trừ IPN có chữ ký.

Đối với nhóm chức năng, hậu điều kiện áp dụng cho nhánh được chọn; không bắt người dùng thực hiện mọi thao tác trong nhóm ở một lần tương tác. Đăng nhập là tiền điều kiện của checkout/quản trị, không vẽ `include Đăng nhập` cho mọi use case. Không ép dùng include/extend khi không có nhu cầu tái sử dụng riêng.

<a id="uc01"></a>
## UC01 — Tài khoản và xác thực

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-01 — Tạo và sử dụng tài khoản để mua hàng; quản lý thông tin liên hệ của chính mình. |
| Tác nhân / module | Guest, Customer / identity |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Chọn đăng ký, đăng nhập, khôi phục hoặc cập nhật hồ sơ |
| Tiền điều kiện | Các chức năng công khai không cần login. Sửa hồ sơ/liên kết cần phiên ACTIVE; thay liên hệ cần xác thực lại. |
| Đầu vào / kiểm tra | Email hợp lệ ≤254 ký tự; mật khẩu 8–72 byte UTF-8; tên 2–100 ký tự; phone E.164 Việt Nam; OTP 6 số. Profile: tên, một địa chỉ mặc định; không nhận role/status từ form. |
| Hậu điều kiện / đầu ra | Đúng tài khoản được xác thực/cập nhật; lỗi không cấp quyền. Không lộ mật khẩu/OTP/token. |
| Màn hình | A01–A05, C01; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `users`, `user_roles`, `external_identities`, `verification_challenges` |
| Quy tắc | BR01 BR02 BR14; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Chọn đăng ký, đăng nhập, khôi phục hoặc cập nhật hồ sơ |
| B2 | PC Store | Xác định thao tác; kiểm định dạng và quyền thao tác |
| B3 | PC Store | Xác minh bằng mật khẩu, OTP hoặc Google theo nhánh đã chọn |
| B4 | PC Store | Kiểm email/phone duy nhất, tài khoản ACTIVE và phương thức đăng nhập còn lại |
| B5 | PC Store | Tạo/cập nhật tài khoản hoặc cấp phiên; tiêu thụ challenge dùng một lần |
| B6 | PC Store | Đổi session ID khi login, giữ giỏ; trả hồ sơ hoặc màn hình đích |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Dữ liệu và quyền hợp lệ? | Báo lỗi field hoặc yêu cầu đăng nhập; không lưu; kết thúc lần thao tác này. |
| E-B3 | Không đạt: Bằng chứng xác thực hợp lệ? | Báo xác thực thất bại/hết hạn; không cấp phiên, không đổi liên hệ; kết thúc lần thao tác này. |
| E-B4 | Không đạt: Không trùng và tài khoản dùng được? | Báo trùng/khóa/không thể bỏ phương thức cuối; giữ dữ liệu cũ; kết thúc lần thao tác này. |
| A1 tại B1 | Đăng ký | Email/password tạo Customer; phone đăng ký chỉ tạo sau OTP đúng; Google mới tạo identity theo issuer+subject. |
| A2 tại B3 | Quên mật khẩu | Form luôn trả thông báo trung tính. Link reset hạn 15 phút, dùng một lần; đặt mật khẩu mới vô hiệu phiên cũ rồi về login. |
| A3 tại B3 | Google trùng email | Không tự gộp. Đăng nhập tài khoản hiện hữu rồi link Google trong hồ sơ; identity thuộc user khác bị từ chối. |
| A4 tại B3 | Đổi liên hệ | Phone/email chỉ có hiệu lực sau xác minh; giữ liên hệ cũ trước khi hoàn tất. OTP hạn 5 phút, 5 lần sai, resend ≥60 giây. |
| A5 tại B5 | Hồ sơ và logout | Sửa tên/địa chỉ không đổi snapshot đơn. Logout hủy session, giỏ và quote; gỡ Google bị chặn nếu không còn cách đăng nhập. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC01-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Đúng tài khoản được xác thực/cập nhật; lỗi không cấp quyền. Không lộ mật khẩu/OTP/token. |
| AC-UC01-E2 | Làm sai điều kiện tại B2: Dữ liệu và quyền hợp lệ? | Báo lỗi field hoặc yêu cầu đăng nhập; không lưu; không ghi dữ liệu thành công một phần. |
| AC-UC01-E3 | Làm sai điều kiện tại B3: Bằng chứng xác thực hợp lệ? | Báo xác thực thất bại/hết hạn; không cấp phiên, không đổi liên hệ; không ghi dữ liệu thành công một phần. |
| AC-UC01-E4 | Làm sai điều kiện tại B4: Không trùng và tài khoản dùng được? | Báo trùng/khóa/không thể bỏ phương thức cuối; giữ dữ liệu cũ; không ghi dữ liệu thành công một phần. |
| AC-UC01-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC01

![ACT-UC01 — Tài khoản và xác thực](../architecture/diagrams/report-01/png/ACT-UC01.png)

[Nguồn sơ đồ ACT-UC01](../architecture/diagrams/report-01/source/ACT-UC01.puml)

<a id="uc02"></a>
## UC02 — Tìm và xem PC

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-02 — Tìm một PC đang bán và đọc cấu hình, giá, bảo hành trước khi lựa chọn. |
| Tác nhân / module | Guest, Customer / catalog |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Mở trang chủ hoặc danh sách PC; nhập từ khóa/bộ lọc |
| Tiền điều kiện | Không cần đăng nhập. Chỉ PC ACTIVE được công bố. |
| Đầu vào / kiểm tra | Từ khóa tên/SKU ≤100; loại, hãng, giá min/max, RAM, GPU; sort theo allowlist; page ≥0, size ≤100; slug PC. |
| Hậu điều kiện / đầu ra | Hiển thị thông tin hiện tại; chưa giữ hàng và chưa phát sinh đơn. |
| Màn hình | S01–S03; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `products`, `categories`, `product_specs`, `product_images`, `product_suitability` |
| Quy tắc | BR03 BR15; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Mở trang chủ hoặc danh sách PC; nhập từ khóa/bộ lọc |
| B2 | PC Store | Kiểm bộ lọc, khoảng giá và phân trang |
| B3 | PC Store | Tìm PC ACTIVE, phân trang và sắp xếp ổn định |
| B4 | Người dùng | Chọn PC cần xem |
| B5 | PC Store | Đọc lại PC theo slug, cấu hình, ảnh và bảo hành |
| B6 | PC Store | Hiển thị chi tiết và available; khóa nút thêm giỏ nếu hết hàng |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Bộ lọc hợp lệ? | Giữ form, thông báo điều kiện sai; kết thúc lần thao tác này. |
| E-B3 | Không đạt: Có kết quả? | Hiện danh sách rỗng và nút xóa bộ lọc; kết thúc lần thao tác này. |
| E-B5 | Không đạt: PC vẫn ACTIVE? | Trả 404; gợi ý quay danh sách; kết thúc lần thao tác này. |
| A1 tại B1 | Trang chủ | Hiển thị tối đa 4 PC ACTIVE còn hàng; nếu không có vẫn giữ CTA catalog/tư vấn. |
| A2 tại B3 | Thay filter/sort | Quay lại bước 2; lưu query trên URL để back/refresh giữ điều kiện. |
| A3 tại B6 | Hết hàng | Vẫn xem cấu hình nhưng không thêm giỏ. Mở URL PC INACTIVE/DISCONTINUED trả 404; đơn cũ vẫn dùng snapshot. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC02-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Hiển thị thông tin hiện tại; chưa giữ hàng và chưa phát sinh đơn. |
| AC-UC02-E2 | Làm sai điều kiện tại B2: Bộ lọc hợp lệ? | Giữ form, thông báo điều kiện sai; không ghi dữ liệu thành công một phần. |
| AC-UC02-E3 | Làm sai điều kiện tại B3: Có kết quả? | Hiện danh sách rỗng và nút xóa bộ lọc; không ghi dữ liệu thành công một phần. |
| AC-UC02-E5 | Làm sai điều kiện tại B5: PC vẫn ACTIVE? | Trả 404; gợi ý quay danh sách; không ghi dữ liệu thành công một phần. |
| AC-UC02-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC02

![ACT-UC02 — Tìm và xem PC](../architecture/diagrams/report-01/png/ACT-UC02.png)

[Nguồn sơ đồ ACT-UC02](../architecture/diagrams/report-01/source/ACT-UC02.puml)

<a id="uc03"></a>
## UC03 — Tư vấn chọn PC

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-03 — Nhận tối đa ba PC phù hợp nhu cầu và ngân sách, kèm lý do và hạn chế rõ ràng. |
| Tác nhân / module | Guest, Customer / advisory |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Chọn nhu cầu và nhập ngân sách, RAM, dung lượng tối thiểu |
| Tiền điều kiện | Không cần đăng nhập; Admin đã nhập dữ liệu phù hợp theo nhu cầu trong form PC. |
| Đầu vào / kiểm tra | usageProfile: OFFICE_STUDY/PROGRAMMING/GAMING/CONTENT_CREATION; maxBudget nguyên 1..1 tỷ VND; minRamGb 0..1024, minStorageGb 0..100000, mặc định 0. |
| Hậu điều kiện / đầu ra | Kết quả có thể giải thích; ngân sách chỉ tính giá PC, chưa phí giao; không reserve. |
| Màn hình | S04; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `products`, `product_specs`, `product_suitability`, `product_images` |
| Quy tắc | BR10; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Chọn nhu cầu và nhập ngân sách, RAM, dung lượng tối thiểu |
| B2 | PC Store | Kiểm enum và giới hạn số |
| B3 | PC Store | Lọc PC ACTIVE, còn hàng, có profile, không vượt ngân sách và đủ cấu hình |
| B4 | PC Store | Xếp score giảm, giá tăng, SKU tăng; lấy tối đa 3 |
| B5 | PC Store | Hiển thị lý do, hạn chế, giá và phần ngân sách còn lại |
| B6 | Người dùng | Mở chi tiết hoặc chỉnh tiêu chí để tư vấn lại |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Tiêu chí hợp lệ? | Báo lỗi theo field; giữ form; kết thúc lần thao tác này. |
| E-B3 | Không đạt: Có PC đạt tất cả điều kiện? | Trả rỗng, chỉ ra bộ lọc làm hết ứng viên; không tự tăng ngân sách; kết thúc lần thao tác này. |
| A1 tại B3 | Không phù hợp | Cho chỉnh tiêu chí và quay bước 1; không đề xuất PC vượt trần như thể đã đạt yêu cầu. |
| A2 tại B4 | Đồng điểm | Giá thấp hơn đứng trước, cuối cùng SKU tăng để kết quả ổn định. |
| A3 tại B5 | Diễn giải điểm | Score5: Rất phù hợp; 3–4: Phù hợp; 1–2: Cân nhắc. Đây là rule có dữ liệu Admin, không phải AI/FPS/benchmark. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC03-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Kết quả có thể giải thích; ngân sách chỉ tính giá PC, chưa phí giao; không reserve. |
| AC-UC03-E2 | Làm sai điều kiện tại B2: Tiêu chí hợp lệ? | Báo lỗi theo field; giữ form; không ghi dữ liệu thành công một phần. |
| AC-UC03-E3 | Làm sai điều kiện tại B3: Có PC đạt tất cả điều kiện? | Trả rỗng, chỉ ra bộ lọc làm hết ứng viên; không tự tăng ngân sách; không ghi dữ liệu thành công một phần. |
| AC-UC03-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC03

![ACT-UC03 — Tư vấn chọn PC](../architecture/diagrams/report-01/png/ACT-UC03.png)

[Nguồn sơ đồ ACT-UC03](../architecture/diagrams/report-01/source/ACT-UC03.puml)

<a id="uc04"></a>
## UC04 — Quản lý giỏ hàng

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-04 — Tập hợp PC và số lượng trước khi checkout bằng giỏ HTTP Session. |
| Tác nhân / module | Guest, Customer / cart |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Xem giỏ hoặc chọn thêm, đổi số lượng, xóa dòng |
| Tiền điều kiện | Có session; không yêu cầu đăng nhập. Giỏ tối đa 20 SKU. |
| Đầu vào / kiểm tra | productId UUID; quantity nguyên 1..99; cartVersion; CSRF cho mutation. Không nhận giá có thẩm quyền. |
| Hậu điều kiện / đầu ra | Giỏ session cập nhật, không có cart table và không giữ tồn. |
| Màn hình | S05; nút thêm tại S02–S04; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `products`, `product_specs`, `product_images` |
| Quy tắc | BR04; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Xem giỏ hoặc chọn thêm, đổi số lượng, xóa dòng |
| B2 | PC Store | Đọc session cart và kiểm version cho thao tác ghi |
| B3 | PC Store | Đọc catalog; thêm/đổi lượng cần PC ACTIVE và đủ available, xóa được cả dòng không còn bán |
| B4 | PC Store | Cập nhật dòng: thêm trùng SKU cộng lượng, đổi lượng thay thế, xóa bỏ dòng; tăng version khi thay đổi |
| B5 | PC Store | Tính lại giá hiện tại và subtotal; đánh dấu dòng không mua được |
| B6 | PC Store | Hiện giỏ mới, empty state hoặc CTA checkout nếu hợp lệ |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Version và số lượng hợp lệ? | 409 nếu hai tab xung đột hoặc lỗi lượng; tải lại, giữ giỏ mới hơn; kết thúc lần thao tác này. |
| E-B3 | Không đạt: Thao tác được phép? | Giữ giỏ; báo thiếu hàng/ngừng bán/giới hạn 20 SKU; kết thúc lần thao tác này. |
| A1 tại B1 | Chỉ xem | Không mutate version; đọc lại giá và stock. Dòng PC đã xóa vẫn có cảnh báo và nút bỏ. |
| A2 tại B4 | Quantity bằng 0 | Không dùng 0 để xóa; yêu cầu action xóa rõ ràng. |
| A3 tại B6 | Login/logout | Login đổi session ID nhưng giữ giỏ; logout hủy giỏ. Sau đặt thành công chỉ dọn giỏ nếu version vẫn khớp. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC04-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Giỏ session cập nhật, không có cart table và không giữ tồn. |
| AC-UC04-E2 | Làm sai điều kiện tại B2: Version và số lượng hợp lệ? | 409 nếu hai tab xung đột hoặc lỗi lượng; tải lại, giữ giỏ mới hơn; không ghi dữ liệu thành công một phần. |
| AC-UC04-E3 | Làm sai điều kiện tại B3: Thao tác được phép? | Giữ giỏ; báo thiếu hàng/ngừng bán/giới hạn 20 SKU; không ghi dữ liệu thành công một phần. |
| AC-UC04-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC04

![ACT-UC04 — Quản lý giỏ hàng](../architecture/diagrams/report-01/png/ACT-UC04.png)

[Nguồn sơ đồ ACT-UC04](../architecture/diagrams/report-01/source/ACT-UC04.puml)

<a id="uc05"></a>
## UC05 — Đặt hàng và thanh toán

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-05 — Tạo một đơn từ giỏ và chọn COD hoặc VNPAY sandbox, không bán vượt tồn hoặc nhân đôi đơn. |
| Tác nhân / module | Customer / sales, payment |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Nhập nơi nhận và chọn COD hoặc VNPAY |
| Tiền điều kiện | User ACTIVE đã đăng nhập, có phone verified; giỏ hợp lệ; địa chỉ đủ thông tin. |
| Đầu vào / kiểm tra | Người nhận 2–100; province/ward 1–100; district tùy chọn; addressLine 5–255; note ≤500; COD/VNPAY; quoteId, cartVersion, idempotencyKey. |
| Hậu điều kiện / đầu ra | Đơn và tồn nhất quán; thanh toán online chỉ xác nhận qua IPN hợp lệ. Không có hoàn tiền/đối soát trong scope. |
| Màn hình | C02–C03; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `users`, `products`, `orders`, `order_items`, `payment_attempts`, `payment_events`, `order_status_history`, `idempotency_records` |
| Quy tắc | BR05 BR06 BR07 BR08 BR09 BR14; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Nhập nơi nhận và chọn COD hoặc VNPAY |
| B2 | PC Store | Kiểm tài khoản, phone, giỏ, địa chỉ; tính giá + phí 50.000 và quote hạn 10 phút |
| B3 | Người dùng | Xem tổng và xác nhận đặt hàng |
| B4 | PC Store | Kiểm idempotency, khóa user và products theo ID; đọc lại quote/giá/stock |
| B5 | PC Store | Trong một transaction: tạo order/items snapshot/attempt/history, tăng reserved, lưu idempotency |
| B6 | PC Store | Commit, dọn giỏ đúng version và gửi mail sau commit |
| B7 | PC Store | COD: chờ xác nhận; VNPAY: redirect URL ký cùng reference và hạn 15 phút |
| B8 | PC Store | VNPAY: IPN xác minh cập nhật; return chỉ đọc trạng thái; hiển thị kết quả server |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Đủ điều kiện checkout? | Yêu cầu xác minh phone, sửa giỏ/địa chỉ; chưa tạo đơn; kết thúc lần thao tác này. |
| E-B4 | Không đạt: Key mới và quote/stock còn hợp lệ? | Cùng key/payload trả đơn cũ; mismatch hoặc thay đổi trả 409, không tạo đơn mới; kết thúc lần thao tác này. |
| A1 tại B4 | Cạnh tranh tồn | Khóa SKU theo ID; không đủ lượng rollback toàn bộ, yêu cầu preview lại. Một key không tạo hai đơn. |
| A2 tại B7 | COD | Order AWAITING_CONFIRMATION, payment PENDING; chỉ ghi PAID cùng lúc Admin xác nhận DELIVERED. |
| A3 tại B7 | Mở lại VNPAY | Chỉ PENDING dùng cùng reference/deadline, không tạo attempt mới. FAILED: hủy/đặt lại hoặc chờ hết hạn. |
| A4 tại B8 | IPN không hợp lệ/trùng | Sai chữ ký/ref/amount không thay tài chính. Trùng không xử lý lần hai. Return query không được ép PAID. |
| A5 tại B8 | Hết hạn/thành công muộn | Chưa trả sau 15 phút: EXPIRED và release một lần. Thành công sau hạn/hủy: REVIEW_REQUIRED, không hồi sinh order. |
| A6 tại B6 | Lỗi email | Không rollback đơn đã commit; khách xem kết quả tại tài khoản. Không có durable outbox trong scope. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC05-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Đơn và tồn nhất quán; thanh toán online chỉ xác nhận qua IPN hợp lệ. Không có hoàn tiền/đối soát trong scope. |
| AC-UC05-E2 | Làm sai điều kiện tại B2: Đủ điều kiện checkout? | Yêu cầu xác minh phone, sửa giỏ/địa chỉ; chưa tạo đơn; không ghi dữ liệu thành công một phần. |
| AC-UC05-E4 | Làm sai điều kiện tại B4: Key mới và quote/stock còn hợp lệ? | Cùng key/payload trả đơn cũ; mismatch hoặc thay đổi trả 409, không tạo đơn mới; không ghi dữ liệu thành công một phần. |
| AC-UC05-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC05

![ACT-UC05 — Đặt hàng và thanh toán](../architecture/diagrams/report-01/png/ACT-UC05.png)

[Nguồn sơ đồ ACT-UC05](../architecture/diagrams/report-01/source/ACT-UC05.puml)

<a id="uc06"></a>
## UC06 — Theo dõi và hủy đơn của tôi

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-06 — Xem đơn đã đặt, tiến trình xử lý và tự hủy khi còn cho phép. |
| Tác nhân / module | Customer / sales |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Mở danh sách và chọn đơn của mình |
| Tiền điều kiện | User ACTIVE; mọi truy vấn giới hạn theo principal. |
| Đầu vào / kiểm tra | Mã đơn, status filter, page/size; hủy có version và reason 5–500. |
| Hậu điều kiện / đầu ra | Đọc đúng đơn; nếu hủy thành công giải phóng tồn đúng một lần; lịch sử không bị xóa. |
| Màn hình | C04–C05; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `orders`, `order_items`, `order_revisions`, `order_status_history`, `payment_attempts`, `products` |
| Quy tắc | BR02 BR06 BR07 BR09; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Mở danh sách và chọn đơn của mình |
| B2 | PC Store | Tra đơn theo code + currentUser |
| B3 | PC Store | Hiển thị snapshots, tổng tiền, timeline, trạng thái payment và allowedActions |
| B4 | Người dùng | Nếu cần, chọn hủy và xác nhận lý do |
| B5 | PC Store | Khóa order/attempt, so version và điều kiện hủy |
| B6 | PC Store | Giảm reserved, đổi RELEASED/CANCELLED và ghi history trong cùng transaction |
| B7 | PC Store | Trả trạng thái mới; đơn DELIVERED có liên kết tới bảo hành |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Đơn thuộc người đang đăng nhập? | 404; không tiết lộ chủ đơn khác; kết thúc lần thao tác này. |
| E-B5 | Không đạt: COD chờ xác nhận chưa trả hoặc VNPAY chờ trả, HELD? | 409 không được hủy; giữ đơn và tồn; kết thúc lần thao tác này. |
| A1 tại B3 | Chỉ theo dõi | Kết thúc sau hiển thị chi tiết, không bắt buộc chọn hủy. |
| A2 tại B5 | Đã thanh toán online/đã xác nhận COD | Không tự hủy; UI không đưa action ngoài điều kiện. |
| A3 tại B6 | Hủy và IPN đồng thời | Cùng khóa order/attempt; thao tác đến sau đọc lại trạng thái. Nếu hủy trước mà tiền đến sau thì REVIEW_REQUIRED. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC06-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Đọc đúng đơn; nếu hủy thành công giải phóng tồn đúng một lần; lịch sử không bị xóa. |
| AC-UC06-E2 | Làm sai điều kiện tại B2: Đơn thuộc người đang đăng nhập? | 404; không tiết lộ chủ đơn khác; không ghi dữ liệu thành công một phần. |
| AC-UC06-E5 | Làm sai điều kiện tại B5: COD chờ xác nhận chưa trả hoặc VNPAY chờ trả, HELD? | 409 không được hủy; giữ đơn và tồn; không ghi dữ liệu thành công một phần. |
| AC-UC06-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC06

![ACT-UC06 — Theo dõi và hủy đơn của tôi](../architecture/diagrams/report-01/png/ACT-UC06.png)

[Nguồn sơ đồ ACT-UC06](../architecture/diagrams/report-01/source/ACT-UC06.puml)

<a id="uc07"></a>
## UC07 — Quản lý PC và loại sản phẩm

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-07 — Duy trì PC, cấu hình, ảnh, giá, dữ liệu tư vấn, loại PC và tồn đơn giản. |
| Tác nhân / module | Admin / catalog |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Tìm PC/loại và chọn thêm, sửa, trạng thái, xóa hoặc điều chỉnh tồn |
| Tiền điều kiện | Admin ACTIVE. PC là cấu hình hoàn chỉnh; không có phân hệ linh kiện/đa kho. |
| Đầu vào / kiểm tra | SKU ≤50, slug ≤180, tên 2–180; categoryId; giá nguyên 1..1 tỷ; specs; warranty 0..60 tháng; version. Gallery ≤8 ảnh 5MiB. Delta tồn khác 0, reason 5–500. |
| Hậu điều kiện / đầu ra | Catalog cập nhật; snapshots đơn cũ không đổi, tồn có thể giải thích; không tạo ledger doanh nghiệp. |
| Màn hình | M01–M03; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `categories`, `products`, `product_specs`, `product_images`, `product_suitability`, `inventory_adjustments`, `order_items` |
| Quy tắc | BR03 BR06 BR10 BR13; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Tìm PC/loại và chọn thêm, sửa, trạng thái, xóa hoặc điều chỉnh tồn |
| B2 | PC Store | Kiểm quyền, field, ảnh, unique SKU/slug và version |
| B3 | PC Store | Kiểm điều kiện riêng: xóa không tham chiếu, tồn không dưới reserved, ACTIVE đủ cấu hình |
| B4 | PC Store | Lưu thay đổi catalog trong transaction; stock adjustment ghi actor, before/after và lý do |
| B5 | PC Store | Commit, dọn media bỏ sau commit; trả danh sách/chi tiết cập nhật |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Dữ liệu và version hợp lệ? | Báo field/trùng/stale; giữ form; kết thúc lần thao tác này. |
| E-B3 | Không đạt: Thỏa điều kiện nghiệp vụ? | Chặn xóa dữ liệu đã dùng hoặc giảm quá tồn; đề nghị ngừng bán; kết thúc lần thao tác này. |
| A1 tại B1 | Thêm/sửa PC | Tạo SKU mới cho cấu hình lớn; SKU sau tạo bất biến. Edit không được ghi stock/reserved bằng field thường. |
| A2 tại B1 | Gallery và tư vấn | Ảnh kiểm MIME/size, chọn cover và thứ tự; profile 4 nhu cầu score1–5 kèm lý do; nằm trong M02, không thêm màn hình. |
| A3 tại B3 | Xóa PC/loại | PC có order item không xóa; category có bất kỳ PC nào không xóa. Không cascade xóa đơn. |
| A4 tại B3 | Điều chỉnh tồn | Khóa product, newOnHand=old+delta; reserved≤newOnHand≤10000. Không chỉnh reserved trực tiếp. |
| A5 tại B4 | Ngừng bán | Đổi DISCONTINUED/INACTIVE, không hủy đơn đã có. Category không status, chỉ PC có status. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC07-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Catalog cập nhật; snapshots đơn cũ không đổi, tồn có thể giải thích; không tạo ledger doanh nghiệp. |
| AC-UC07-E2 | Làm sai điều kiện tại B2: Dữ liệu và version hợp lệ? | Báo field/trùng/stale; giữ form; không ghi dữ liệu thành công một phần. |
| AC-UC07-E3 | Làm sai điều kiện tại B3: Thỏa điều kiện nghiệp vụ? | Chặn xóa dữ liệu đã dùng hoặc giảm quá tồn; đề nghị ngừng bán; không ghi dữ liệu thành công một phần. |
| AC-UC07-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC07

![ACT-UC07 — Quản lý PC và loại sản phẩm](../architecture/diagrams/report-01/png/ACT-UC07.png)

[Nguồn sơ đồ ACT-UC07](../architecture/diagrams/report-01/source/ACT-UC07.puml)

<a id="uc08"></a>
## UC08 — Xử lý đơn hàng

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-08 — Xác nhận đơn, sửa lượng COD được phép, cập nhật giao hàng và xem ngoại lệ thanh toán. |
| Tác nhân / module | Admin / sales, payment |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Tra cứu và mở chi tiết đơn |
| Tiền điều kiện | Admin ACTIVE; order tồn tại; chỉ action hợp lệ được hiển thị. |
| Đầu vào / kiểm tra | Bộ lọc/mã đơn; action CONFIRM/REVISE/CANCEL/SHIP/DELIVER; version; reason khi sửa/hủy; itemId→quantity 0..99. |
| Hậu điều kiện / đầu ra | Chỉ chuyển trạng thái hợp lệ; lịch sử actor/thời gian/lý do được giữ; không phát sinh quản trị logistics. |
| Màn hình | M04–M05; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `orders`, `order_items`, `products`, `payment_attempts`, `payment_events`, `order_revisions`, `order_status_history` |
| Quy tắc | BR06 BR07 BR08 BR09 BR11; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Tra cứu và mở chi tiết đơn |
| B2 | PC Store | Đọc snapshots, timeline, payment summary và action hợp lệ |
| B3 | Người dùng | Chọn action và xác nhận thông tin |
| B4 | PC Store | Khóa order/attempt, so version và bảng chuyển trạng thái |
| B5 | PC Store | Sửa/hủy/giao: khóa products theo ID; kiểm và cập nhật lượng giữ/xuất |
| B6 | PC Store | Ghi order/revision/history/payment liên quan và commit |
| B7 | PC Store | Hiển thị timeline mới; DELIVERED bắt đầu quyền bảo hành |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Đơn tồn tại? | 404; trở về danh sách; kết thúc lần thao tác này. |
| E-B4 | Không đạt: Action hợp lệ? | 409; tải lại dữ liệu mới, không ép trạng thái; kết thúc lần thao tác này. |
| E-B5 | Không đạt: Đủ tồn và đơn còn ít nhất một item khi sửa? | 409 thiếu tồn hoặc 422 đơn rỗng; rollback; kết thúc lần thao tác này. |
| A1 tại B4 | Sửa lượng | Chỉ COD PENDING, AWAITING_CONFIRMATION. Giữ đơn giá/phí snapshot; không thêm SKU; lượng0 bỏ dòng, phải còn dòng dương; cập nhật cả payment amount. |
| A2 tại B4 | Xác nhận/hủy | CONFIRM từ chờ xác nhận nếu COD PENDING hoặc VNPAY PAID. Admin chỉ hủy COD chưa xác nhận/chưa trả. |
| A3 tại B5 | Giao hàng | CONFIRMED→SHIPPING: giảm onHand và reserved, HELD→CONSUMED. Không tích hợp vận đơn. |
| A4 tại B6 | Giao thành công | SHIPPING→DELIVERED: deliveredAt server; COD chuyển PAID cùng transaction. Không tác động tồn lần nữa. |
| A5 tại B2 | REVIEW_REQUIRED | Hiện cảnh báo để xử lý thủ công ngoài hệ thống; không có nút ép PAID, khôi phục đơn hay refund. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC08-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Chỉ chuyển trạng thái hợp lệ; lịch sử actor/thời gian/lý do được giữ; không phát sinh quản trị logistics. |
| AC-UC08-E2 | Làm sai điều kiện tại B2: Đơn tồn tại? | 404; trở về danh sách; không ghi dữ liệu thành công một phần. |
| AC-UC08-E4 | Làm sai điều kiện tại B4: Action hợp lệ? | 409; tải lại dữ liệu mới, không ép trạng thái; không ghi dữ liệu thành công một phần. |
| AC-UC08-E5 | Làm sai điều kiện tại B5: Đủ tồn và đơn còn ít nhất một item khi sửa? | 409 thiếu tồn hoặc 422 đơn rỗng; rollback; không ghi dữ liệu thành công một phần. |
| AC-UC08-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC08

![ACT-UC08 — Xử lý đơn hàng](../architecture/diagrams/report-01/png/ACT-UC08.png)

[Nguồn sơ đồ ACT-UC08](../architecture/diagrams/report-01/source/ACT-UC08.puml)

<a id="uc09"></a>
## UC09 — Quản lý tài khoản khách hàng

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-09 — Tra cứu, cập nhật thông tin cần thiết, khóa/mở hoặc xóa tài khoản đủ điều kiện. |
| Tác nhân / module | Admin / identity |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Tìm tài khoản và mở drawer |
| Tiền điều kiện | Admin ACTIVE; không có UI cấp role hoặc xem credential. |
| Đầu vào / kiểm tra | q ≤100; role/status/provider filter; tên/contact/địa chỉ; version; reason 5–500; action UPDATE/LOCK/UNLOCK/DELETE. |
| Hậu điều kiện / đầu ra | Tài khoản được quản lý đúng điều kiện; đơn/snapshot không bị sửa theo profile. |
| Màn hình | M06; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `users`, `user_roles`, `external_identities`, `verification_challenges`, `orders` |
| Quy tắc | BR01 BR02 BR15; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Tìm tài khoản và mở drawer |
| B2 | PC Store | Đọc thông tin được phép, số đơn và các phương thức đăng nhập |
| B3 | Người dùng | Chọn cập nhật, khóa, mở khóa hoặc xóa và nhập lý do |
| B4 | PC Store | Khóa user, kiểm version, unique liên hệ và ràng buộc Admin cuối/lịch sử |
| B5 | PC Store | Lưu thay đổi hoặc xóa user chưa có tham chiếu; đổi contact bỏ verified, tăng sessionVersion khi cần |
| B6 | PC Store | Trả drawer mới; phiên bị khóa/vô hiệu sẽ bị chặn ở request bảo vệ tiếp theo |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: User tồn tại? | 404; không hiện dữ liệu credential; kết thúc lần thao tác này. |
| E-B4 | Không đạt: Thao tác được phép? | 409 do stale, trùng, Admin cuối, user có giao dịch hoặc mất phương thức cuối; kết thúc lần thao tác này. |
| A1 tại B4 | Xóa | Không xóa user có đơn hoặc được tham chiếu làm actor lịch sử; đề nghị khóa. |
| A2 tại B4 | Admin cuối | Không khóa/xóa Admin ACTIVE cuối. Không nhận role/password từ form quản lý. |
| A3 tại B5 | Đổi contact | Không tự xác minh thay khách; không đổi phone-only sang phone chưa verified nếu không còn phương thức khác. |
| A4 tại B5 | Mở khóa | Chuyển ACTIVE; yêu cầu login mới, không khôi phục session cũ. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC09-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Tài khoản được quản lý đúng điều kiện; đơn/snapshot không bị sửa theo profile. |
| AC-UC09-E2 | Làm sai điều kiện tại B2: User tồn tại? | 404; không hiện dữ liệu credential; không ghi dữ liệu thành công một phần. |
| AC-UC09-E4 | Làm sai điều kiện tại B4: Thao tác được phép? | 409 do stale, trùng, Admin cuối, user có giao dịch hoặc mất phương thức cuối; không ghi dữ liệu thành công một phần. |
| AC-UC09-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC09

![ACT-UC09 — Quản lý tài khoản khách hàng](../architecture/diagrams/report-01/png/ACT-UC09.png)

[Nguồn sơ đồ ACT-UC09](../architecture/diagrams/report-01/source/ACT-UC09.puml)

<a id="uc10"></a>
## UC10 — Tra cứu bảo hành và tiến độ

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-10 — Biết mặt hàng đã mua còn bảo hành không và xem tình trạng yêu cầu đã gửi. |
| Tác nhân / module | Customer / aftersales |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Mở Bảo hành của tôi hoặc bảo hành từ chi tiết đơn |
| Tiền điều kiện | Đăng nhập ACTIVE; chỉ dữ liệu của chính Customer. |
| Đầu vào / kiểm tra | Tab còn hạn/hết hạn/yêu cầu đã gửi; orderItemId/unitIndex hoặc requestCode; page/size. |
| Hậu điều kiện / đầu ra | Khách thấy quyền lợi và tiến độ của mình; không có hồ sơ serial hay cam kết ngoài snapshot. |
| Màn hình | C05–C07; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `orders`, `order_items`, `warranty_requests`, `warranty_request_images`, `warranty_request_events` |
| Quy tắc | BR02 BR11 BR12 BR13; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Mở Bảo hành của tôi hoặc bảo hành từ chi tiết đơn |
| B2 | PC Store | Tra đơn DELIVERED của currentUser và item snapshot |
| B3 | PC Store | Tính thời hạn từ deliveredAt + tháng snapshot, theo tháng lịch giờ Việt Nam |
| B4 | PC Store | Hiển thị từng vị trí 1..quantity, trạng thái còn/hết hạn và yêu cầu đang mở |
| B5 | Người dùng | Mở chi tiết một yêu cầu đã gửi nếu có |
| B6 | PC Store | Kiểm owner, đọc ảnh và timeline công khai |
| B7 | PC Store | Hiển thị kết quả/từ chối và action được phép kể cả yêu cầu đã đóng |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Có mặt hàng đã giao? | Hiển thị empty; giải thích chỉ đơn đã giao có bảo hành; kết thúc lần thao tác này. |
| E-B6 | Không đạt: Yêu cầu thuộc Customer? | 404; không lộ chủ khác hoặc internal note; kết thúc lần thao tác này. |
| A1 tại B3 | Hết hạn | now≥end hoặc warrantyMonths=0 không tạo yêu cầu mới; vẫn xem lịch sử. |
| A2 tại B4 | Không có yêu cầu | Kết thúc tại quyền lợi; chọn gửi mới chuyển UC11 nếu đủ điều kiện. |
| A3 tại B6 | Ảnh private | Chỉ owner/Admin được xem qua endpoint kiểm quyền, không có public static URL. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC10-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Khách thấy quyền lợi và tiến độ của mình; không có hồ sơ serial hay cam kết ngoài snapshot. |
| AC-UC10-E2 | Làm sai điều kiện tại B2: Có mặt hàng đã giao? | Hiển thị empty; giải thích chỉ đơn đã giao có bảo hành; không ghi dữ liệu thành công một phần. |
| AC-UC10-E6 | Làm sai điều kiện tại B6: Yêu cầu thuộc Customer? | 404; không lộ chủ khác hoặc internal note; không ghi dữ liệu thành công một phần. |
| AC-UC10-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC10

![ACT-UC10 — Tra cứu bảo hành và tiến độ](../architecture/diagrams/report-01/png/ACT-UC10.png)

[Nguồn sơ đồ ACT-UC10](../architecture/diagrams/report-01/source/ACT-UC10.puml)

<a id="uc11"></a>
## UC11 — Gửi hoặc hủy yêu cầu bảo hành

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-11 — Đề nghị hỗ trợ cho một đơn vị PC đã mua, hoặc hủy khi cửa hàng chưa tiếp nhận. |
| Tác nhân / module | Customer / aftersales |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Chọn gửi mới hoặc hủy yêu cầu của mình |
| Tiền điều kiện | Đăng nhập; item thuộc đơn DELIVERED của mình; tạo mới còn hạn và không có yêu cầu mở cùng item/unit. |
| Đầu vào / kiểm tra | orderItemId, unitIndex 1..quantity; description 10–2000; ≤3 ảnh JPEG/PNG/WebP ≤5MiB; clientRequestId. Hủy: requestCode, version, reason5–500. |
| Hậu điều kiện / đầu ra | Một yêu cầu mở/item/unit; không đổi tồn, không tạo shipment/refund/replacement. |
| Màn hình | C07; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `orders`, `order_items`, `warranty_requests`, `warranty_request_images`, `warranty_request_events` |
| Quy tắc | BR11 BR12 BR13 BR14; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Chọn gửi mới hoặc hủy yêu cầu của mình |
| B2 | PC Store | Kiểm owner, đầu vào và ảnh; khóa item khi tạo hoặc request khi hủy |
| B3 | PC Store | Tạo: kiểm DELIVERED, còn hạn, unitIndex và không có yêu cầu mở; hủy: kiểm REQUESTED/version |
| B4 | PC Store | Tạo REQUESTED + snapshot hạn + metadata ảnh + event; hoặc chuyển CANCELLED và ghi lý do trong transaction |
| B5 | PC Store | Commit; gửi mail sau commit nếu có email verified |
| B6 | PC Store | Trả mã yêu cầu và timeline công khai |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Dữ liệu và quyền hợp lệ? | Báo lỗi field/ảnh hoặc 404; không ghi một phần; kết thúc lần thao tác này. |
| E-B3 | Không đạt: Đủ điều kiện thao tác? | 409 hết hạn/trùng/đã tiếp nhận; giữ dữ liệu cũ; kết thúc lần thao tác này. |
| A1 tại B3 | Submit lặp | Cùng clientRequestId/cùng payload trả yêu cầu cũ; khác payload trả409. Khóa item và unique active request chặn race. |
| A2 tại B3 | Hủy | Chỉ REQUESTED; nếu Admin đã RECEIVED thì không hủy. Không xóa yêu cầu khỏi lịch sử. |
| A3 tại B4 | Sau hết hạn | Yêu cầu đã được tạo hợp lệ vẫn được xử lý. Sau yêu cầu đóng có thể gửi mới nếu còn hạn. |
| A4 tại B5 | Mail lỗi | Không rollback request; Customer vẫn theo dõi tại UC10. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC11-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Một yêu cầu mở/item/unit; không đổi tồn, không tạo shipment/refund/replacement. |
| AC-UC11-E2 | Làm sai điều kiện tại B2: Dữ liệu và quyền hợp lệ? | Báo lỗi field/ảnh hoặc 404; không ghi một phần; không ghi dữ liệu thành công một phần. |
| AC-UC11-E3 | Làm sai điều kiện tại B3: Đủ điều kiện thao tác? | 409 hết hạn/trùng/đã tiếp nhận; giữ dữ liệu cũ; không ghi dữ liệu thành công một phần. |
| AC-UC11-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC11

![ACT-UC11 — Gửi hoặc hủy yêu cầu bảo hành](../architecture/diagrams/report-01/png/ACT-UC11.png)

[Nguồn sơ đồ ACT-UC11](../architecture/diagrams/report-01/source/ACT-UC11.puml)

<a id="uc12"></a>
## UC12 — Tiếp nhận và xử lý bảo hành

| Thuộc tính | Đặc tả |
|---|---|
| Yêu cầu | FR-12 — Cập nhật tiến độ và kết quả bảo hành để khách theo dõi được. |
| Tác nhân / module | Admin / aftersales |
| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |
| Kích hoạt | Tìm và mở yêu cầu bảo hành |
| Tiền điều kiện | Admin ACTIVE; request tồn tại. Tiếp nhận máy trực tiếp và trao đổi ngoài hệ thống. |
| Đầu vào / kiểm tra | Filter q/status; requestCode; version; action RECEIVE/PROCESS/COMPLETE/REJECT/NOTE; publicNote/internalNote; kết quả hoặc lý do10–2000 ký tự. |
| Hậu điều kiện / đầu ra | Khách thấy tiến độ/kết luận rõ ràng; lịch sử được giữ và ghi chú nội bộ không bị lộ. |
| Màn hình | M07; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |
| Dữ liệu liên quan | `warranty_requests`, `warranty_request_images`, `warranty_request_events`, `orders`, `order_items`, `users` |
| Quy tắc | BR11 BR12 BR14; [giải thích đầy đủ](BUSINESS_RULES.md) |

### Luồng chính

| Bước | Tác nhân | Hành động và phản hồi |
|---|---|---|
| B1 | Người dùng | Tìm và mở yêu cầu bảo hành |
| B2 | PC Store | Hiển thị thông tin đơn, item/unit, ảnh, thời hạn snapshot và timeline |
| B3 | Người dùng | Chọn tiếp nhận, xử lý, hoàn tất, từ chối hoặc thêm ghi chú |
| B4 | PC Store | Khóa request; so version, trạng thái và nội dung bắt buộc |
| B5 | PC Store | Cập nhật trạng thái và append event; tách publicNote khỏi internalNote |
| B6 | PC Store | Commit, gửi thông báo nếu có; Customer chỉ nhận phần công khai |

### Luồng thay thế và ngoại lệ

| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |
|---|---|---|
| E-B2 | Không đạt: Yêu cầu tồn tại? | 404; quay danh sách; kết thúc lần thao tác này. |
| E-B4 | Không đạt: Action và nội dung hợp lệ? | 409 chuyển sai/stale hoặc422 thiếu kết quả/lý do; giữ trạng thái; kết thúc lần thao tác này. |
| A1 tại B4 | Chuyển trạng thái | REQUESTED→RECEIVED→PROCESSING→COMPLETED. Từ chối từ RECEIVED/PROCESSING→REJECTED, cần lý do công khai. |
| A2 tại B4 | Ghi chú | Chỉ RECEIVED/PROCESSING; ít nhất một public/internal note; không sửa event cũ, không reopen yêu cầu đóng. |
| A3 tại B4 | Đã hết hạn hiện tại | Không từ chối chỉ vì hiện tại quá hạn nếu yêu cầu đã được tạo hợp lệ trước hạn. |
| A4 tại B5 | Hoàn tất | Ghi kết quả công khai và closedAt; không gia hạn bảo hành, không sửa tồn hoặc tự hoàn tiền. |

### Tiêu chí nghiệm thu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| AC-UC12-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | Khách thấy tiến độ/kết luận rõ ràng; lịch sử được giữ và ghi chú nội bộ không bị lộ. |
| AC-UC12-E2 | Làm sai điều kiện tại B2: Yêu cầu tồn tại? | 404; quay danh sách; không ghi dữ liệu thành công một phần. |
| AC-UC12-E4 | Làm sai điều kiện tại B4: Action và nội dung hợp lệ? | 409 chuyển sai/stale hoặc422 thiếu kết quả/lý do; giữ trạng thái; không ghi dữ liệu thành công một phần. |
| AC-UC12-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |

### Activity UC12

![ACT-UC12 — Tiếp nhận và xử lý bảo hành](../architecture/diagrams/report-01/png/ACT-UC12.png)

[Nguồn sơ đồ ACT-UC12](../architecture/diagrams/report-01/source/ACT-UC12.puml)
