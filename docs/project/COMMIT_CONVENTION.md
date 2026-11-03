# Quy ước commit

Dự án sử dụng định dạng Conventional Commits để lịch sử thay đổi dễ đọc, dễ tìm kiếm và không bị lẫn nhiều kiểu đặt tên.

## 1. Định dạng bắt buộc

```text
<type>(<scope>): <mô tả ngắn>
```

Ví dụ:

```text
feat(cart): thêm kiểm tra số lượng sản phẩm
fix(catalog): ngăn giữ hàng vượt tồn khả dụng
docs(project): bổ sung quy ước commit
chore(scaffold): khởi tạo cấu trúc module
```

## 2. Type được sử dụng

| Type | Sử dụng khi |
|---|---|
| `feat` | Thêm hoặc thay đổi một chức năng người dùng/nghiệp vụ |
| `fix` | Sửa lỗi hành vi hoặc nghiệp vụ |
| `refactor` | Cải tổ mã nguồn nhưng không đổi hành vi |
| `test` | Thêm hoặc sửa kiểm thử |
| `docs` | Chỉ thay đổi tài liệu |
| `style` | Chỉ sửa định dạng, không đổi logic |
| `perf` | Cải thiện hiệu năng |
| `build` | Thay đổi Maven, dependency hoặc quá trình build |
| `ci` | Thay đổi workflow CI/CD |
| `chore` | Công việc bảo trì, scaffold hoặc công cụ hỗ trợ |
| `revert` | Hoàn tác một commit trước đó |

Không tự tạo type mới nếu chưa thống nhất với nhóm.

## 3. Scope được sử dụng

Ưu tiên scope theo module hoặc khu vực bị tác động trực tiếp:

```text
identity
catalog
advisory
cart
sales
payment
aftersales
ui
api
data
db
security
integration
e2e
ops
docs
project
scaffold
```

Nếu thay đổi bao phủ nhiều module và không có module chính, có thể bỏ scope:

```text
build: cấu hình dependency nền tảng cho dự án
```

Không dùng scope chung chung như `code`, `backend`, `update` hoặc tên thành viên.

## 4. Quy tắc viết mô tả

- Viết ngắn gọn, mô tả đúng kết quả của commit.
- Bắt đầu bằng chữ thường và không đặt dấu chấm ở cuối.
- Giữ toàn bộ dòng tiêu đề tối đa khoảng 72 ký tự khi có thể.
- Một commit chỉ nên chứa một thay đổi logic có thể giải thích độc lập.
- Có thể dùng tiếng Việt có dấu; không trộn tiếng Việt và tiếng Anh tùy tiện trong cùng câu.
- Không dùng nội dung mơ hồ như `update`, `fix bug`, `sửa code`, `done`, `test thử` hoặc `commit mới`.
- Không đưa tên người làm, ngày tháng hoặc số thứ tự commit vào tiêu đề.

## 5. Form commit đầy đủ

Với thay đổi nhỏ, chỉ cần dòng tiêu đề:

```text
<type>(<scope>): <mô tả ngắn>
```

Với thay đổi cần giải thích, dùng form sau:

```text
<type>(<scope>): <mô tả ngắn>

Lý do:
- <vấn đề hoặc mục tiêu cần giải quyết>

Thay đổi:
- <thay đổi quan trọng thứ nhất>
- <thay đổi quan trọng thứ hai>

Kiểm tra:
- <cách đã kiểm tra hoặc "chưa chạy - nêu lý do">

Refs: <mã ticket hoặc issue nếu có>
```

Phần body phải cách tiêu đề một dòng trống. Không cần giữ các mục không có nội dung.

## 6. Breaking change

Thay đổi phá vỡ contract, schema hoặc cách sử dụng phải thêm `!` và mô tả rõ ở footer:

```text
feat(payment)!: thay đổi contract xác nhận thanh toán

BREAKING CHANGE: callback phải gửi thêm trường transactionTime.
```

Không dùng breaking change cho migration thông thường vẫn tương thích với phiên bản đang chạy.

## 7. Commit mẫu theo dự án

```text
feat(identity): thêm đăng nhập bằng email
feat(catalog): thêm bộ lọc theo CPU và GPU
fix(cart): giữ nguyên giỏ hàng sau khi đăng nhập
fix(catalog): giải phóng lượng giữ khi đơn hết hạn
feat(payment): xử lý callback VNPAY trùng lặp
test(sales): kiểm tra idempotency khi tạo đơn
feat(aftersales): thêm yêu cầu bảo hành theo mặt hàng đã mua
feat(db): thêm bảng warranty_requests
docs(api): mô tả contract checkout
ci: chạy test với PostgreSQL Testcontainers
```

Với thay đổi database, `db` là scope; không dùng `db` làm type.

## 8. Commit không hợp lệ

```text
update code
fix bug
Nhan sua database
commit lan 2
feat: update
feat(Canh): lam gio hang
```

Các commit tạm như `wip`, `temp` hoặc `save` phải được squash hoặc đổi tên trước khi merge vào `main`.
