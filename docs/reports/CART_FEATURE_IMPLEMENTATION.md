# Cart: MVC, session guest và giỏ tài khoản

Thiết kế cập nhật 26/10/2026 theo yêu cầu lưu giỏ tài khoản. Thay thế quyết định session-only trước đó của BR04, UC04 và PLAN. Chỉ triển khai Cart; module đăng nhập do thành viên khác xây dựng.

## Kiến trúc

```text
cart/
  controller/  CartController, CartDemoController (HTTP)
  service/     CartService (nghiệp vụ), CartStorageService (session/transaction)
  repository/  CartRepository (SQL)
  model/       CartSession (trạng thái giỏ dùng chung cho hai loại lưu trữ)
  dto/         CartDtos (request/response)
  security/    CartUserIdentity, CartOwnerResolver (hợp đồng tích hợp xác thực)
  exception/   CartException
frontend/
  services/cart.js                 REST client
  contexts/CartContext.jsx         trạng thái và điều phối
  pages/CartPage.jsx               View
  components/CartDemoPanel.jsx     View giả lập chỉ ở dev
```

React là View; REST controller nhận request; service xử lý quy tắc; repository đọc/ghi DB.
CartService không phụ thuộc HttpSession hoặc SQL. Catalog và Advisory giữ nguyên tổ chức hiện tại.

## CART-01 — Nghiệp vụ và chủ giỏ

- Guest: session lưu productId, quantity, version; hết session thì hết giỏ.
- User: một giỏ duy nhất theo users.id; lưu DB nên có thể đọc lại từ session/thiết bị mới.
- Chủ giỏ lấy từ Authentication principal triển khai CartUserIdentity. API Cart thông thường không nhận userId từ request.
- SKU được đại diện bởi products.id có sku unique. Mỗi biến thể mua được phải có productId/SKU riêng.
- Chỉ thêm ACTIVE; quantity nguyên 1..99; tối đa 20 SKU. Tổng trên từng SKU không vượt stock_on_hand - reserved_quantity.
- Giá, tên và trạng thái đọc từ catalog. Giỏ không snapshot giá và không giữ hàng.
- Sửa giỏ user khóa hàng carts bằng SELECT FOR UPDATE trong transaction, kiểm cartVersion trước ghi.
- Gộp giỏ bằng POST /api/v1/cart/merge: cộng cùng SKU; tất cả hoặc không có gì. Bất kỳ dòng nào không hợp lệ thì rollback và giữ nguyên giỏ khách.
- Merge receipt ngăn gộp trùng nếu retry sau khi commit nhưng mất response.
- Lưu **giỏ hiện tại**, chưa có nhật ký ADD/REMOVE hay lịch sử đơn hàng.

## CART-02 — Bảng và migration

Migration V8__persistent_user_carts.sql:

| Bảng | Khóa và dữ liệu |
| --- | --- |
| carts | id UUID PK; user_id UUID UNIQUE FK users; version; created_at; updated_at |
| cart_items | PK(cart_id, product_id); quantity CHECK 1..99; FK cart và product |
| cart_merge_receipts | guest_id UUID PK; cart_id FK; created_at |

Inventory tiếp tục dùng products.stock_on_hand/reserved_quantity. Reservation chỉ thuộc checkout, Cart không tạo reservation.
V8 nhắm schema UUID hiện tại mà entity/V6/V7 sử dụng. Các migration V1–V5 cũ vẫn là BIGINT và không dựng được schema UUID mới từ database rỗng; đây là vấn đề baseline có sẵn, chưa sửa trong phạm vi Cart.
Chưa chạy V8 trên database thật trong phiên này. Không tự sửa tồn kho hoặc seed user.
RLS bật cho bảng Cart; backend cần kết nối role có quyền phù hợp như các bảng storefront hiện hành.

## CART-03 — Trạng thái và vòng đời

- Cart rỗng/có hàng được xác định từ items; version tăng khi ghi.
- Guest sang user: merge thành công rồi bỏ dữ liệu guest; lỗi giữ guest để sửa/gộp lại.
- User về guest: giỏ DB giữ nguyên; giỏ guest độc lập.
- Order/Reservation không được triển khai ở đây. Checkout tương lai phải kiểm lại giá và tồn, dùng HELD/RELEASED/CONSUMED theo thiết kế sales.

## CART-04 — REST và hợp đồng đăng nhập

Giữ nguyên GET/DELETE /api/v1/cart, POST /api/v1/cart/items, PATCH/DELETE /api/v1/cart/items/{productId}.
POST/PATCH body chứa quantity và cartVersion; POST thêm productId. DELETE gửi cartVersion ở query.
POST /api/v1/cart/merge không có body. Tất cả mutation cần CSRF.
Lỗi nghiệp vụ: 404 không có sản phẩm/dòng, 409 version cũ/ngừng bán/thiếu tồn/vượt giới hạn; 401 định danh thiếu hoặc user không ACTIVE.

Nhóm Auth tích hợp:

1. Principal đã xác thực triển khai CartUserIdentity.userId() trả UUID user.
2. Đổi session ID sau login nhưng giữ thuộc tính guest cart.
3. Sau login gọi POST /api/v1/cart/merge rồi GET Cart. Frontend hiện gọi khi AuthContext.user.id thay đổi.
4. Logout invalidate session. Không xóa carts/cart_items. Frontend reset hiển thị giỏ khi user thay đổi.

Không nhận UUID user bằng header tùy ý hoặc query trong các API chính.

## Giả lập để kiểm thử độc lập

Chạy backend với profile bổ sung:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,cart-demo"
```

Chạy npm run dev trong frontend, vào /cart. Bảng giả lập chỉ xuất hiện nếu frontend dev và backend bật cart-demo.
Nhập UUID của user ACTIVE có sẵn. V6 có user mẫu a0000000-0000-0000-0000-000000000002 nếu database đã seed.
POST /api/v1/cart/demo với {"userId":"UUID"} chuyển sang user, {"userId":null} chuyển sang guest. GET cùng URL đọc chế độ.
Đây là giả lập riêng Cart, không tạo Authentication, không cấp quyền các module khác, không gọi /auth/login.
Không bật cart-demo trên môi trường công khai/production. Khi không bật profile, controller demo không được đăng ký và resolver bỏ qua định danh giả.
Các sản phẩm V7 ban đầu có tồn 0 nên sẽ bị từ chối thêm đúng quy tắc; dùng sản phẩm đã có tồn để kiểm thử thành công.

## Kiểm thử

- [x] Unit test: SKU cộng dồn, SKU khác dòng, inactive/thiếu tồn, version cũ.
- [x] Unit test: guest độc lập giữa session và không ghi repository.
- [x] Unit test: đọc lại giỏ user ở session mới; user khác/guest không thấy giỏ đó.
- [x] Unit test: merge thành công, rollback khi vượt tồn, chống gộp trùng khi retry.
- [x] Unit test: giả lập chỉ có hiệu lực với profile; principal thật ưu tiên; user inactive bị chặn.
- [x] Maven clean test: 13 tests qua.
- [x] Vite production build qua.
- [ ] Chạy migration V8 và kiểm thử PostgreSQL thực tế.
- [ ] Kiểm thử trình duyệt: guest → user → guest → cùng user, hai user, tải lại và hai thiết bị.
- [ ] Kiểm thử tranh chấp transaction trên PostgreSQL với nhiều request đồng thời.
- [ ] Tích hợp principal/login/logout thật sau khi nhóm Auth hoàn tất.

Các test storage hiện mock repository/transaction manager; chưa thay thế kiểm thử DB đồng thời hoặc browser E2E.
