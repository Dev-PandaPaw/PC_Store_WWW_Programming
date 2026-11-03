# Kiểm tra riêng thao tác thêm vào giỏ hàng

## Các nguyên nhân xác định trong mã

1. **Đọc dữ liệu sản phẩm lặp lại:** `CartService.add` trước đây gọi `findById` để kiểm tra sản phẩm, rồi truy vấn lại danh sách sản phẩm khi dựng response. Entity Product có category/spec EAGER, nên lượt `findById` còn có thể tải thêm các quan hệ. Tối ưu trước đó mới gộp truy vấn dựng response, chưa loại bỏ lượt đọc này.
2. **Không có phản hồi trong thời gian chờ:** ProductCard và trang chi tiết chỉ đổi giao diện sau khi `await addItem` hoàn tất. Trang chủ cũng chỉ mở drawer sau khi server trả thành công. Vì vậy người dùng không thấy trạng thái xử lý trong thời gian API đang chạy.
3. **Bấm lặp tạo hàng đợi:** Nút chỉ disabled khi giỏ chưa sẵn sàng, không disabled lúc đang thêm. Mỗi lần bấm tạo thêm một mutation; CartContext xử lý tuần tự để bảo vệ cartVersion nên độ trễ tích lũy khi có nhiều request chờ.

## Yếu tố chưa đo được

- Cấu hình local dùng database Supabase từ xa, không phải database trên máy. Round trip mạng, chờ connection pool và khóa giỏ tài khoản có thể ảnh hưởng độ trễ nhưng chưa có số đo để xác định mức đóng góp.
- Giỏ tài khoản còn thực hiện kiểm tra tài khoản ACTIVE, tải/khóa cart, tải các dòng, lưu thay đổi và commit. Đây là các thao tác bổ sung so với giỏ khách; không nên bỏ kiểm tra hoặc khóa chỉ để giảm thời gian.
- Hệ thống duyệt tự động từ chối chạy backend kết nối database dùng chung vì chưa có cho phép rõ ràng cho phép đo này. Không khởi chạy backend hoặc thử kết nối gián tiếp sau khi bị từ chối.

## Thay đổi

- Thêm record `CartProductSnapshot` và JPQL constructor projection: chỉ lấy ID, SKU, tên, giá, trạng thái và tồn kho.
- Add/update đọc toàn bộ các ID cần thiết trong một truy vấn projection và tái sử dụng cùng snapshot cho kiểm tra và response; merge cũng dùng một batch. Không tải entity hoặc category/spec qua luồng cart.
- Nút thêm phản hồi ngay bằng spinner/trạng thái đang thêm, disabled và ref chặn các lần bấm trùng trong khi request chưa hoàn tất. Chỉ báo thành công hoặc mở drawer sau khi API thành công; lỗi mở lại nút.
- DEV ghi Performance measures `cart.add.queue` và `cart.add.request` để phân biệt thời gian chờ hàng đợi với thời gian thực thi request. Thời gian request có thể bao gồm GET phục hồi khi xung đột phiên bản; phép đo không tự phân rã backend/database.

## Xác minh cục bộ

- Frontend production build thành công.
- 20 test backend đạt. Có test xác nhận add dùng đúng một lần đọc projection cho cả sản phẩm cũ và mới; query projection được Hibernate kiểm tra với entity mappings, hoàn toàn không cấu hình kết nối database.
- Chrome headless + API giả lập chậm 1.000 ms: trạng thái đang thêm xuất hiện sau khoảng 5 ms trong lần chạy kiểm thử. Đây là thời gian phản hồi giao diện trên máy kiểm thử, **không phải thời gian API thực tế**.
- Năm lần bấm liên tiếp trên cùng nút tạo đúng một POST; kiểm tra trên trang chi tiết và ProductCard.
- API giả lập báo thiếu tồn kho: giữ tổng giỏ, hiển thị lỗi và mở lại nút. Drawer chỉ mở khi thêm thành công.
- Các kiểm tra nhập số lượng, debounce, rollback và viewport 320/390/1440 px vẫn đạt.

Chưa có benchmark với Supabase thật; cần được cho phép chạy phép đo trước khi kết luận độ trễ từ mạng/database.
