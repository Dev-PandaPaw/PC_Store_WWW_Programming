# Đặt hàng ORD01–ORD15

## Chạy demo

Yêu cầu Java 21, Maven, Node.js và kết nối PostgreSQL hiện có. Chạy ứng dụng bình thường; không cần đổi profile, database hay tạo Docker image. Tài khoản đăng nhập kiểm thử được gán cứng: **test / 1234**. Ứng dụng vẫn dùng danh mục sản phẩm hiện tại.

```powershell
mvn spring-boot:run
```

Terminal khác:

```powershell
cd frontend
npm ci
npm run dev
```

Mở `http://localhost:5173/login`, nhập tên đăng nhập **test**, mật khẩu **1234**. Thêm sản phẩm vào giỏ, tải lại trang hoặc đăng xuất rồi đăng nhập lại: giỏ được lưu trong database theo tài khoản. Nếu thêm giỏ trước khi đăng nhập, hệ thống gộp giỏ khách vào giỏ tài khoản sau khi đăng nhập thành công.

Response login trả luôn giỏ đã gộp để cập nhật giao diện cùng tài khoản, không gọi gộp lần hai. Xem [luồng cập nhật giỏ ngay khi đăng nhập](CART_LOGIN_REALTIME.md).

Tài khoản cố định nằm tại `src/main/java/iuh/fit/nhom16/pcstore/identity/model/TestUser.java`:

```java
public static final String USERNAME = "test";
public static final String PASSWORD = "1234";
```

Khi nhập đúng thông tin, backend tạo tài khoản kiểm thử riêng có UUID `a0000000-0000-0000-0000-000000000005` nếu chưa có, với số điện thoại mẫu đã xác minh **0987654321**, quyền CUSTOMER và ADMIN để thử các chức năng. Không ghi đè thông tin hay xác minh số của tài khoản khách hàng khác. Tài khoản bị khóa vẫn bị từ chối đăng nhập. Đây là đăng nhập mẫu, chưa có đăng ký, OTP hay xác thực mật khẩu người dùng bất kỳ.

Danh tính `TestUser` thực hiện `CartUserIdentity`, lưu trong Spring Security session sau khi đổi session ID. Không tự đăng nhập khi mở trang. `GET /api/auth/me` khôi phục trạng thái sau khi tải lại; `POST /api/auth/logout` kết thúc session, không xóa giỏ tài khoản trong database. Các API ghi, gồm login/logout, được bảo vệ CSRF. Tắt tài khoản mẫu khi triển khai công khai bằng `TEST_LOGIN_ENABLED=false`.

Kết nối dùng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` từ cấu hình mặc định `local`; Spring không tự đọc `.env`, cần đặt biến môi trường trong terminal hoặc IDE. Nếu đã dùng bộ database hai PC từ lần chạy cũ, chuyển kết nối về database chứa danh mục đầy đủ. Mailpit/Docker chỉ là lựa chọn chạy dịch vụ local, không phải cơ chế giả lập user. Xóa `order-demo` khỏi Active profiles/VM options cũ của IDE và khởi động lại backend.

Lỗi giỏ trong ảnh được xác định bằng kiểm tra schema đang dùng: `cart_items.id` là UUID NOT NULL, không có giá trị mặc định; code trước đây chỉ insert cart_id/product_id/quantity nên vi phạm ràng buộc. `CartRepository` hiện cấp UUID khi insert; migration V10 bổ sung default ID và bảng cart_merge_receipts nếu thiếu, giữ nguyên giỏ/số lượng cũ. Khởi động lại ứng dụng để Flyway áp dụng V10.

## Cấu trúc MVC

```text
src/main/java/iuh/fit/nhom16/pcstore/
  identity/
    controller/     TestLoginController: login/me/logout, session
    model/          TestUser: một tài khoản test/1234 gán cứng
    service/        TestLoginService: kiểm tra tài khoản mẫu, tạo bản ghi nếu thiếu
  sales/
    controller/     OrderController, AdminOrderController: HTTP + validation
    dto/            Request/response, snapshot dòng hàng và quote
    model/          OrderStatus: luật chuyển trạng thái
    service/        OrderService: transaction, nghiệp vụ, thông báo, hết hạn
    repository/     OrderRepository: SQL có tham số, khóa hàng
    security/       OrderActorResolver: ACTIVE + role từ database
    exception/      OrderException: mã lỗi nghiệp vụ
    configuration/ Clock UTC, scheduling, async email
  payment/
    controller/     IPN endpoint
    service/        VnpayGateway: ký/xác minh; VnpayIpnService: cập nhật payment
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
| ORD07 | Order, items, payment attempt, history, idempotency, consumed quote, tồn kho và xóa giỏ nằm cùng transaction. |
| ORD08 | Idempotency-Key theo customer, so quoteId + paymentMethod; khóa cart serialize request cùng customer trên nhiều instance; unique quote_id bảo vệ một quote/một order. Retry trả đơn đã tạo, key dùng cho nội dung khác trả 409. |
| ORD09 | COD bắt đầu PENDING_CONFIRMATION/UNPAID; chỉ PAID khi Admin đánh dấu giao thành công COMPLETED. |
| ORD10 | URL VNPAY 2.1.0, HMAC-SHA512, amount ×100; IPN kiểm chữ ký, merchant, reference, số tiền, trạng thái giao dịch. Browser return chỉ hiển thị, không cập nhật payment. IPN lặp không ghi thanh toán hai lần. |
| ORD11 | PENDING_PAYMENT/PENDING_CONFIRMATION → CANCELLED hoặc EXPIRED; PENDING_CONFIRMATION → CONFIRMED → SHIPPING → COMPLETED. COD chờ xác nhận tối đa 24 giờ; VNPAY chờ thanh toán 15 phút. Scheduler quét mỗi phút; không hết hạn đơn VNPAY đã PAID. |
| ORD12 | Admin xác nhận/giao/hoàn tất; chỉ sửa lượng COD UNPAID/PENDING_CONFIRMATION. Điều chỉnh delta tồn, tổng và amount attempt bằng giá snapshot; ghi lượng cũ/mới và lý do vào history. |
| ORD13 | Customer chỉ thấy đơn của mình (404 nếu UUID thuộc người khác); Admin có API riêng; phân trang 20 đơn, lịch sử theo thứ tự ghi. |
| ORD14 | Event phát trong transaction, listener AFTER_COMMIT gửi async; lỗi SMTP được bắt, không rollback. Không lưu secret/PII vào log gửi email. |
| ORD15 | CSRF cookie/header giữ nguyên cho mọi API ghi; IPN GET xác thực chữ ký. 401/403/404/409 và lỗi validation; lỗi SQL không lộ ra response. Kiểm tra version khi sửa đơn; rollback lỗi transaction. |

Email gửi theo best effort sau commit, chưa có outbox/retry bền vững khi process dừng. Thanh toán thành công đến sau khi hủy/hết hạn được ghi `REVIEW_REQUIRED`; không tái giữ hàng. Đối soát/hoàn tiền thực hiện ngoài phạm vi luồng đặt hàng; hệ thống chặn hủy đơn đã PAID.

## API

Gọi `GET /api/csrf` trước các API ghi, giữ session cookie và gửi `X-XSRF-TOKEN` từ cookie `XSRF-TOKEN`. Đăng nhập bằng `POST /api/auth/login` với `{ "username": "test", "password": "1234" }`, kiểm tra phiên bằng `GET /api/auth/me`, đăng xuất bằng `POST /api/auth/logout`. Bộ chọn session cũ `POST /api/v1/cart/demo` chỉ dành cho profile `cart-demo`, không cần cho luồng đăng nhập mẫu.

| Method | Endpoint | Nội dung |
|---|---|---|
| GET | /api/v1/orders/account | Tài khoản đang chọn |
| POST | /api/v1/orders/quotes | `{cartVersion, shipping:{recipient,phone,province,ward,addressLine}}` |
| POST | /api/v1/orders | Header `Idempotency-Key`, body `{quoteId,paymentMethod:"COD" hoặc "VNPAY"}` |
| GET | /api/v1/orders?page=0 | Đơn của customer |
| GET | /api/v1/orders/{id} | Chi tiết + history |
| POST | /api/v1/orders/{id}/cancel | `{version,status:"CANCELLED",reason}` |
| GET | /api/v1/admin/orders?page=0 | Danh sách quản trị |
| GET | /api/v1/admin/orders/{id} | Chi tiết quản trị |
| PATCH | /api/v1/admin/orders/{id}/status | `{version,status,reason}` |
| PATCH | /api/v1/admin/orders/{id}/items/{productId} | `{version,quantity,reason}` |
| GET | /api/v1/payments/vnpay/ipn | Tham số `vnp_*` được gateway ký |

`/api/v1/orders/**` và `/api/v1/admin/orders/**` cho qua security filter để hỗ trợ identity giả lập; mỗi service entry point kiểm tra quyền từ database. Không bỏ qua CSRF. Khi tích hợp đăng nhập thật, principal phải cung cấp `CartUserIdentity`; không nhận userId/customerId/role từ body đặt hàng.

## VNPAY sandbox

Đặt `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL` trước khi chạy profile demo. Nếu chưa có thông tin merchant, VNPAY trả 503 và transaction rollback; COD chạy độc lập. Merchant cần cấu hình IPN URL HTTPS công khai trỏ về `/api/v1/payments/vnpay/ipn`. Không dùng return URL thay IPN.

Tham khảo [đặc tả thanh toán VNPAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html). Chưa xác nhận giao dịch đầu cuối với merchant thật trong phiên triển khai này.

## Database hiện có

Migration chung cho module nằm tại `db/orders/V9__checkout_orders.sql`; ứng dụng mặc định thêm location `db/orders`. Module dùng các bảng `sales_*` để không ép đổi/xóa bảng orders/payment legacy đang khác schema. Migration yêu cầu users/products UUID như code catalog/cart hiện tại. Chưa tự migrate dữ liệu đơn legacy sang module này. Migration thêm `users.phone_verified_at` nếu thiếu; không tự đánh dấu số điện thoại thật là đã xác minh.

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

Kết quả bản đăng nhập test/1234: 51 Java unit tests qua; 8 integration tests PostgreSQL qua trên schema test riêng, gồm login/session/logout, giữ giỏ sau đăng nhập lại, tương thích cột cart_items.id và cạnh tranh tồn kho/transaction. 7 frontend tests và Vite production build qua. Kiểm tra schema ứng dụng thực tế chỉ đọc, không sửa giỏ/dữ liệu hiện có; migration V10 sẽ được áp dụng khi khởi động lại backend.
