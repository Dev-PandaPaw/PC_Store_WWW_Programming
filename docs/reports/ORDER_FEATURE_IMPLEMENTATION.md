# Đặt hàng ORD01–ORD15

## Chạy demo

Yêu cầu Java 21, Maven, Node.js và PostgreSQL với schema UUID đang dùng. Đăng nhập/đăng ký dùng AuthService và AuthController từ nhánh development; không dùng tài khoản giả lập mặc định.

```powershell
mvn spring-boot:run
```

Terminal khác:

```powershell
cd frontend
npm ci
npm run dev
```

Mở `http://localhost:5173/login`, đăng nhập bằng email/mật khẩu tài khoản trong database, hoặc dùng `/register` để tạo tài khoản. Luồng checkout, giỏ hàng, trạng thái tồn kho và thông báo 0,5 giây được giữ từ ngan.

Login xác thực bằng BCrypt của development, sau đó gộp giỏ khách trước khi ghi phiên đăng nhập. Response trả tài khoản và `cart` đã gộp trong cùng phản hồi; frontend không gộp lần hai. Session chứa UUID và email qua `AuthenticatedAccount`, tương thích CartOwnerResolver và OrderActorResolver. Nếu gộp giỏ thất bại, login không tạo phiên thành công.

`TEST_LOGIN_ENABLED=false` là mặc định: chỉ AuthController thật nhận `/api/auth/*`. Chỉ đặt `true` khi cần chạy controller tài khoản mẫu cũ (`test / 1234`, `test2 / 1234`); hai controller không cùng hoạt động. Các endpoint ghi vẫn cần CSRF. Tài khoản đăng ký mới phải có số điện thoại được xác minh để đặt hàng, giữ nguyên quy tắc của checkout.

Migration V11 thêm các cột hồ sơ cần cho User của development và bảng `identity_verification_challenges`, không sửa/xóa bảng challenge cũ. Migration áp dụng khi backend khởi động. Không tự đánh dấu số điện thoại của tài khoản mới là đã xác minh.

Kết nối dùng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; giữ cấu hình database hiện có. Không chạy integration tests vào database ứng dụng: test đơn hàng yêu cầu `ORDER_TEST_DB_URL`, test auth yêu cầu `AUTH_TEST_DB_URL`; mỗi nhóm tạo schema ngẫu nhiên riêng.

Lỗi giỏ trong ảnh được xác định bằng kiểm tra schema đang dùng: `cart_items.id` là UUID NOT NULL, không có giá trị mặc định; code trước đây chỉ insert cart_id/product_id/quantity nên vi phạm ràng buộc. `CartRepository` hiện cấp UUID khi insert; migration V10 bổ sung default ID và bảng cart_merge_receipts nếu thiếu, giữ nguyên giỏ/số lượng cũ. Khởi động lại ứng dụng để Flyway áp dụng V10.

## Cấu trúc MVC

```text
src/main/java/iuh/fit/nhom16/pcstore/
  identity/
    web/            AuthController: login/register/me/logout và khôi phục mật khẩu
    model/          AuthenticatedAccount: UUID/email trong session; TestUser chỉ dùng khi bật test-login
    application/    AuthService: đăng nhập bằng BCrypt, đăng ký và khôi phục mật khẩu
  sales/
    controller/     OrderController, AdminOrderController: HTTP + validation
    dto/            Request/response, snapshot dòng hàng và quote
    model/          OrderStatus: luật chuyển trạng thái
    service/        OrderService: transaction, nghiệp vụ, thông báo, hết hạn
    repository/     OrderRepository: SQL có tham số, khóa hàng
    security/       OrderActorResolver: ACTIVE + role từ database
    exception/      OrderException: mã lỗi nghiệp vụ
    configuration/ Clock UTC, scheduling, async email
frontend/src/
  pages/            CheckoutPage.jsx, OrdersPage.jsx (customer/admin)
  services/         orders.js: HTTP API
  styles/           orders.css
```

React là View của Spring MVC REST. Controller không chứa SQL; service không nhận session/HTTP; repository không quyết định quyền hay vòng đời đơn. Clock được inject để kiểm tra chính xác mốc hết hạn.

## Ánh xạ nghiệp vụ

| Mã | Hiện thực |
|---|---|
| ORD01 | Chặn guest; kiểm tra users.status=ACTIVE và role CUSTOMER trong transaction. Admin cần role ADMIN, không tin role từ client. |
| ORD02 | Khóa cart; kiểm tra version, giỏ không trống, tối đa 100 dòng, số lượng 1–99 và cart khớp quote. |
| ORD03 | Quote lưu DB, ràng buộc chủ sở hữu, hiệu lực đúng 10 phút; tạo quote không giữ tồn. |
| ORD04 | Snapshot tên, SKU, đơn giá, số lượng, bảo hành, địa chỉ; phí 50.000đ; BigDecimal. Giá snapshot được giữ trong hiệu lực quote, kể cả catalog đổi giá. |
| ORD05 | Validate tên/địa chỉ, số điện thoại Việt Nam; chuẩn hóa E.164 và so với phone_verified_at của tài khoản ở cả quote và đặt đơn. |
| ORD06 | Giữ reserved_quantity khi tạo đơn, khóa product theo UUID tăng dần; giao hàng trừ stock_on_hand và reserved_quantity; hủy/hết hạn giải phóng giữ tồn. |
| ORD07 | Order, items, history, idempotency, consumed quote, tồn kho và xóa giỏ nằm cùng transaction. |
| ORD08 | Idempotency-Key theo customer, so quoteId + paymentMethod; khóa cart serialize request cùng customer trên nhiều instance; unique quote_id bảo vệ một quote/một order. Retry trả đơn đã tạo, key dùng cho nội dung khác trả 409. |
| ORD09 | COD bắt đầu PENDING_CONFIRMATION/UNPAID; chỉ PAID khi Admin đánh dấu giao thành công COMPLETED. |
| ORD10 | Thanh toán online đã bỏ khỏi phạm vi. Đặt hàng chỉ nhận COD và kết thúc ở màn hình đặt hàng thành công. Không có gateway, ký giao dịch hoặc endpoint IPN. |
| ORD11 | PENDING_PAYMENT/PENDING_CONFIRMATION → CANCELLED hoặc EXPIRED; PENDING_CONFIRMATION → CONFIRMED → SHIPPING → COMPLETED. COD chờ xác nhận tối đa 24 giờ. Scheduler quét mỗi phút; đơn đã PAID không hết hạn. |
| ORD12 | Admin xác nhận/giao/hoàn tất; chỉ sửa lượng COD UNPAID/PENDING_CONFIRMATION. Điều chỉnh delta tồn và tổng đơn bằng giá snapshot; ghi lượng cũ/mới và lý do vào history. |
| ORD13 | Customer chỉ thấy đơn của mình (404 nếu UUID thuộc người khác); Admin có API riêng; phân trang 20 đơn, lịch sử theo thứ tự ghi. |
| ORD14 | Event phát trong transaction, listener AFTER_COMMIT gửi async; lỗi SMTP được bắt, không rollback. Không lưu secret/PII vào log gửi email. |
| ORD15 | CSRF cookie/header giữ nguyên cho mọi API ghi. 401/403/404/409 và lỗi validation; lỗi SQL không lộ ra response. Kiểm tra version khi sửa đơn; rollback lỗi transaction. |

Email gửi theo best effort sau commit, chưa có outbox/retry bền vững khi process dừng. Hệ thống chặn hủy đơn đã PAID.

## API

Gọi `GET /api/csrf` trước các API ghi, giữ session cookie và gửi `X-XSRF-TOKEN` từ cookie `XSRF-TOKEN`. Đăng nhập bằng `POST /api/auth/login` với email/mật khẩu tài khoản thật trong body `{ "username": "customer@example.com", "password": "<password>" }`, kiểm tra phiên bằng `GET /api/auth/me`, đăng xuất bằng `POST /api/auth/logout`. Bộ chọn session cũ `POST /api/v1/cart/demo` chỉ dành cho profile `cart-demo`, không cần cho luồng đăng nhập mẫu.

| Method | Endpoint | Nội dung |
|---|---|---|
| GET | /api/v1/orders/account | Tài khoản đang chọn |
| POST | /api/v1/orders/quotes | `{cartVersion, shipping:{recipient,phone,province,ward,addressLine}}` |
| POST | /api/v1/orders | Header `Idempotency-Key`, body `{quoteId,paymentMethod:"COD"}` |
| GET | /api/v1/orders?page=0 | Đơn của customer |
| GET | /api/v1/orders/{id} | Chi tiết + history |
| POST | /api/v1/orders/{id}/cancel | `{version,status:"CANCELLED",reason}` |
| GET | /api/v1/admin/orders?page=0 | Danh sách quản trị |
| GET | /api/v1/admin/orders/{id} | Chi tiết quản trị |
| PATCH | /api/v1/admin/orders/{id}/status | `{version,status,reason}` |
| PATCH | /api/v1/admin/orders/{id}/items/{productId} | `{version,quantity,reason}` |

`/api/v1/orders/**` và `/api/v1/admin/orders/**` cho qua security filter để hỗ trợ identity giả lập; mỗi service entry point kiểm tra quyền từ database. Không bỏ qua CSRF. Khi tích hợp đăng nhập thật, principal phải cung cấp `CartUserIdentity`; không nhận userId/customerId/role từ body đặt hàng.

## Phạm vi thanh toán

Module `payment` đã được gỡ bỏ cùng gateway, controller IPN, cấu hình merchant và biến môi trường VNPAY. OrderService không còn phụ thuộc gateway; tạo, xem và sửa đơn không đọc/ghi `sales_payment_attempts` hoặc `sales_payment_events`. Trạng thái thu tiền COD được lưu trực tiếp trên `sales_orders` khi Admin đánh dấu giao hàng hoàn tất.

`PaymentMethod.VNPAY` chỉ còn là giá trị tương thích khi đọc đơn lịch sử. Tạo đơn mới với phương thức này trả 400 `PAYMENT_METHOD_UNSUPPORTED` trước khi giữ tồn. Response đơn không còn trường URL hoặc mã tham chiếu giao dịch.

## Database hiện có

Migration chung cho module nằm tại `db/orders/V9__checkout_orders.sql`; ứng dụng mặc định thêm location `db/orders`. Module dùng các bảng `sales_*` để không ép đổi/xóa bảng orders/payment legacy đang khác schema. Migration yêu cầu users/products UUID như code catalog/cart hiện tại. Chưa tự migrate dữ liệu đơn legacy sang module này. Giữ nguyên các migration đã phát hành và bảng giao dịch cũ để không thay đổi checksum hoặc xóa dữ liệu; runtime đặt hàng không sử dụng các bảng giao dịch. Migration thêm `users.phone_verified_at` nếu thiếu; không tự đánh dấu số điện thoại thật là đã xác minh.

Profile riêng `order-fixtures` giữ bộ schema UUID mới và hai PC tại `db/order-demo` để phục vụ integration tests trên database trống. Không cần dùng profile này để đăng nhập kiểm thử. Test PostgreSQL dùng `order-fixtures,cart-demo` để kiểm tra guest, đổi chủ sở hữu và phân quyền.

## Kiểm thử

```powershell
mvn test
cd frontend
node --test --test-isolation=none scripts/optimistic-cart.test.mjs scripts/catalog.test.mjs
npm run build
```

Bộ test PostgreSQL là opt-in. Nó tạo schema ngẫu nhiên `order_test_<uuid>`, chạy Flyway/schema validation và kiểm tra concurrent checkout cùng key/khác key, tranh đơn vị tồn cuối, rollback sau giữ tồn, vòng đời COD, CSRF và role. Bổ sung kiểm tra cart_items.id bắt buộc không có default, đăng nhập sai, login CSRF, gộp giỏ khách, lưu session và giữ giỏ qua logout/login. Schema test được giữ lại để kiểm tra; không sửa dữ liệu trong schema ứng dụng. Chạy với database dành cho kiểm thử:

```powershell
$env:ORDER_TEST_DB_URL='jdbc:postgresql://localhost:5432/pc_store'
$env:ORDER_TEST_DB_USER='postgres'
$env:ORDER_TEST_DB_PASSWORD='postgres'
mvn '-Dtest=OrderPostgresTest' test
```

Kết quả lịch sử của bản đăng nhập test/1234 (trước khi gỡ module payment): 51 Java unit tests qua; 8 integration tests PostgreSQL qua trên schema test riêng, gồm login/session/logout, giữ giỏ sau đăng nhập lại, tương thích cột cart_items.id và cạnh tranh tồn kho/transaction. 7 frontend tests và Vite production build qua. Kiểm tra schema ứng dụng thực tế chỉ đọc, không sửa giỏ/dữ liệu hiện có; migration V10 sẽ được áp dụng khi khởi động lại backend.
