# Cập nhật giỏ ngay khi đăng nhập

Trước đây `/auth/login` đã gộp và lưu giỏ, nhưng frontend bỏ qua kết quả gộp. Khi user thay đổi, CartProvider xóa giỏ hiển thị rồi gọi `/csrf` và `/v1/cart/merge` lần nữa. Người dùng thấy badge/giỏ trống hoặc chờ thêm một lượt request sau khi đăng nhập.

Luồng hiện tại:

1. Chờ các thao tác giỏ khách đang gửi hoàn tất trước khi gửi login; không để một request giỏ cũ chạy sau bước gộp.
2. Backend gộp giỏ một lần trong transaction, rồi trả thông tin tài khoản cùng `cart` đã commit trong response `/api/auth/login`.
3. AuthProvider cập nhật user và snapshot giỏ cùng một state. CartProvider dùng snapshot này ngay, giữ đúng cartVersion; không xóa về giỏ trống và không gọi merge/get lại sau login.
4. Badge, dòng hàng và tổng tiền nhận chung snapshot server. Các thao tác thêm/sửa tiếp theo vẫn hiển thị optimistic và dùng version đã xác nhận.

Khi tải lại trang, `/auth/me` không có snapshot nên frontend vẫn đọc `/v1/cart` để lấy dữ liệu mới. Snapshot login cũ không được dùng cho guest hoặc user khác. Response `/auth/me` đến muộn không ghi đè phiên vừa đăng nhập/đăng xuất.

CSRF vẫn được giữ: dùng cookie hiện có, chỉ lấy token nếu chưa có. Với tài khoản test đã tồn tại và ACTIVE, login chỉ đọc trạng thái; không tính BCrypt và insert lại user/roles ở mỗi lần đăng nhập. Lần đầu tạo tài khoản và merge lỗi vẫn giữ nguyên cơ chế rollback và giỏ khách.

Đây là cập nhật giao diện ngay từ response đăng nhập, không polling. Thời gian gửi request, truy vấn và commit database vẫn phụ thuộc kết nối; chưa đưa ra cam kết thời gian dưới một ngưỡng millisecond cụ thể.

Kiểm chứng:

- Unit test: response login chứa đúng snapshot gộp, merge chỉ gọi một lần; merge thất bại không tạo phiên đăng nhập; user có sẵn không bị tính lại BCrypt/ghi lại dữ liệu.
- Frontend: kiểm tra snapshot không bị cộng giỏ khách hai lần, version đúng cho thao tác tiếp theo, không tái dùng giỏ sau logout/đổi tài khoản.
- PostgreSQL: kiểm tra snapshot trong login response khớp giỏ lưu DB và giữ đúng số lượng khi đăng xuất/đăng nhập lại.

```powershell
mvn test
cd frontend
node --test --test-isolation=none scripts/optimistic-cart.test.mjs scripts/login-cart.test.mjs scripts/catalog.test.mjs
npm run build
```
