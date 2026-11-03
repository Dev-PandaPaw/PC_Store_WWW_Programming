# Phản hồi tức thì cho giỏ hàng

Giao diện hiện dùng cập nhật optimistic, thay cho trạng thái chờ/spinner trong lần triển khai trước.

- Nút thêm phản hồi ngay và cho phép thêm tiếp trong khi đồng bộ nền. Drawer trang chủ mở ngay.
- Header, trang giỏ và drawer dùng chung trạng thái: số lượng, thành tiền và tổng tiền thay đổi ngay khi thêm, sửa hoặc xóa.
- Bỏ chữ/nút “Cập nhật”. Số lượng hợp lệ tự lưu; các thay đổi liên tiếp cùng sản phẩm được gộp trước khi gửi PATCH (250 ms). Các thao tác vẫn dùng đúng thứ tự và cartVersion server.
- Lưu riêng dữ liệu server đã xác nhận và danh sách thao tác đang chờ. Mỗi response chỉ xác nhận thao tác tương ứng, sau đó áp lại các thao tác mới hơn, tránh nhảy số hoặc đếm trùng.
- Server từ chối: chỉ bỏ thao tác bị lỗi và giữ các thao tác còn lại. Có thông báo lỗi.
- Request cart có timeout 15 giây. Khi timeout/lỗi mạng/5xx hoặc xung đột phiên bản, tải lại giỏ để đối chiếu trước khi chạy thao tác kế tiếp; không tự gửi lại lệnh thêm vì có thể gây cộng trùng nếu server đã ghi nhận.

Kiểm tra: frontend build; 5 test trạng thái optimistic; Chrome headless với API giả lập chậm 1 giây cho nhập số lượng tự lưu, badge cập nhật trước response, gộp PATCH, thao tác trong khi request cũ đang chạy, rollback, nhiều lần thêm liên tiếp và drawer mở ngay. Không cần kết nối database thực tế cho thay đổi giao diện này.

“Realtime” ở đây là cập nhật ngay trên ứng dụng đang mở. Chưa bổ sung cơ chế đẩy thay đổi giữa thiết bị hoặc tab khác.
