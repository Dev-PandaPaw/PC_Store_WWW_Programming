# Luồng đặt hàng và API quản trị

1. Mọi thẻ sản phẩm hiển thị một dòng tồn kho ngay từ đầu, dùng chung dữ liệu với giỏ hàng. Khóa nút thêm khi hết hàng hoặc giỏ đã đủ số lượng cho phép. Không chèn lỗi dài vào thẻ sau khi bấm. Khi thêm hợp lệ, giỏ cập nhật và thông báo xanh thành công xuất hiện ngay theo cơ chế optimistic UI, không chờ API. Giỏ được lưu ở nền; nếu thất bại, giỏ hoàn tác và báo lỗi ở phía trên màn hình. Backend vẫn kiểm tra tồn kho thực tế.
2. Từ giỏ hàng bấm **Đặt hàng** để mở `/checkout`. Chưa đăng nhập sẽ chuyển `/login`, đăng nhập xong trở về `/checkout`; phiên hết hạn khi gọi API cũng chuyển login. Form nhận hàng điền ngay tên và số điện thoại từ phiên đăng nhập; API cập nhật ở nền và không ghi đè những ô khách đã sửa.
   Tỉnh/thành phố có dropdown tìm kiếm có/không dấu; chọn tỉnh mới mở ô quận/huyện tương ứng. Thay tỉnh xóa quận/huyện và phường/xã cũ. Phải chọn giá trị hợp lệ từ danh sách trước khi xác nhận. Bộ dữ liệu địa giới trước 07/2025 được lưu trong frontend, xem `frontend/src/data/vietnamDistricts.source.md`. API nhận thêm trường tùy chọn `shipping.district`; địa chỉ lưu trên đơn bao gồm quận/huyện.
3. Bấm **Xác nhận** để kiểm tra thông tin, giá và tồn kho qua `POST /api/v1/orders/quotes`. Khi thành công, form nhận hàng được thay bằng màn hình kiểm tra đơn hàng. Hai form không xuất hiện đồng thời. Bấm **Chỉnh sửa thông tin** để quay lại, giữ dữ liệu đã nhập. Khi giỏ hàng thay đổi, phải xác nhận lại.
4. Bấm **Đặt hàng** để tạo đơn. Màn hình thành công hiện mã đơn rút gọn, địa chỉ, tổng tiền và liên kết xem đơn. Mở **Chi tiết** ở mã đơn để xem/sao chép mã đầy đủ; mã thật trong database và API giữ nguyên. Với VNPAY, màn hình này có nút tiếp tục thanh toán; đơn vẫn chờ thanh toán.
5. Đăng nhập admin rồi mở `/admin/orders`. Trang chỉ hiển thị sau khi `GET /api/v1/admin/orders/account` trả về thành công. Khách hàng không thấy liên kết quản trị trên trang đơn hàng.

## API mẫu

Dùng cookie phiên đăng nhập admin hiện có. Các request thay đổi dữ liệu phải gửi header `X-XSRF-TOKEN` lấy từ cookie `XSRF-TOKEN` sau `GET /api/csrf`.

Khi bật tài khoản kiểm thử có sẵn (`app.test-login.enabled`), có thể đăng nhập bằng API mẫu sau. Tài khoản `test` hiện được cấp cả `CUSTOMER` và `ADMIN` trong luồng tạo tài khoản kiểm thử của dự án.

```http
POST /api/auth/login
Content-Type: application/json
X-XSRF-TOKEN: <token>

{"username":"test","password":"1234"}
```

```http
GET /api/v1/admin/orders/account
```

Trả về tài khoản có `roles` chứa `ADMIN`. Phiên chưa đăng nhập nhận 401; tài khoản không có quyền nhận 403. Không tạo tài khoản hoặc cấp quyền qua endpoint này.

```http
GET /api/v1/admin/orders?page=0
GET /api/v1/admin/orders/{id}
```

Xác nhận đơn COD đang chờ xác nhận (thay `id` và `version` bằng dữ liệu từ API):

```http
PATCH /api/v1/admin/orders/{id}/status
Content-Type: application/json
X-XSRF-TOKEN: <token>

{
  "version": 0,
  "status": "CONFIRMED",
  "reason": "Đã kiểm tra thông tin nhận hàng"
}
```

Sau đó có thể chuyển `CONFIRMED` → `SHIPPING` → `COMPLETED`, mỗi lần dùng version mới nhất. Trang quản trị đã kết nối các API danh sách, trạng thái và sửa số lượng hiện có.

## Kiểm tra thủ công

- Bấm thêm liên tiếp đến hết tồn kho: lần vượt tồn kho báo ngay, không tăng số lượng giỏ.
- Trước khi bấm: mọi thẻ đều có dòng trạng thái tồn kho; các vị trí hiển thị cùng sản phẩm phải đồng bộ. Nút hết hàng/đã đủ số lượng bị khóa.
- Làm chậm mạng rồi thêm sản phẩm: thông báo xanh thành công và giỏ xuất hiện ngay, thông báo tự đóng sau 0,5 giây kể từ lúc bấm. API thành công không làm thông báo xuất hiện lại; nếu API thất bại, giỏ hoàn tác và báo lỗi. Xác nhận đặt hàng vẫn phải chờ giỏ lưu xong.
- Mở checkout khi đã đăng nhập: tên và số điện thoại xuất hiện ngay. Gõ trước khi API trả về: dữ liệu đang nhập được giữ nguyên.
- Chỉ thấy form nhận hàng ở bước 1. Xác nhận thành công mới thấy màn hình kiểm tra đơn ở bước 2. Các nhãn nút giữ nguyên trong khi xử lý.
- Xác nhận rồi bấm chỉnh sửa: dữ liệu cũ vẫn còn. Sửa địa chỉ phải xác nhận lại; xác nhận hết hạn cũng phải xác nhận lại.
- Mã đơn hiển thị ngắn gọn; mở Chi tiết và Sao chép phải nhận đúng mã đầy đủ từ API.
- Đặt COD: kiểm tra màn hình thành công và đơn xuất hiện trong lịch sử; VNPAY có liên kết thanh toán.
- Mở `/admin/orders` bằng khách và admin: chỉ admin thấy danh sách và thao tác quản lý.
