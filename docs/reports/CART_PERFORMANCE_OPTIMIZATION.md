# Tối ưu giỏ hàng

## Nguyên nhân tìm thấy trong mã nguồn

| Vị trí | Nguyên nhân | Thay đổi |
| --- | --- | --- |
| `CartService.response` | Gọi `findById` riêng cho từng dòng mỗi khi đọc hoặc thay đổi cart. Quan hệ EAGER của Product có thể phát sinh thêm truy vấn. | Dùng projection `findCartProducts` lấy các trường cart cần trong một truy vấn, không tải category/spec; giữ nguyên thứ tự các dòng và cách xử lý sản phẩm đã mất. |
| `CartRepository.save` | Xóa toàn bộ dòng rồi INSERT riêng từng sản phẩm, kể cả khi chỉ sửa một số lượng. | Chỉ xóa sản phẩm đã bỏ; batch UPSERT tối đa 20 dòng, chỉ UPDATE khi quantity thực sự khác. Giữ transaction và khóa theo chủ giỏ. |
| `CartStorageService.merge` | Mỗi lần thêm một dòng trong quá trình merge lại dựng toàn bộ response. | Kiểm tra các dòng rồi dựng response một lần; giữ rollback toàn bộ, receipt chống gộp trùng và chỉ xóa giỏ khách sau commit. |
| `CartContext` | Sau merge thành công lại GET cùng giỏ. Request demo nằm trong hàng đợi nên có thể chặn thao tác thật ở môi trường DEV. | Dùng trực tiếp response merge; tách request demo khỏi hàng đợi mutation. |
| `CartContext` | Mọi lỗi 409, kể cả thiếu tồn kho, đều gây thêm GET cart. Tra cứu ảnh dùng `find` toàn catalog cho từng dòng. | Chỉ GET phục hồi khi `CART_VERSION_CONFLICT`; lập Map sản phẩm theo ID. |
| Điều khiển số lượng | Chờ server mới đổi số hiển thị; bấm liên tiếp gửi nhiều request, có thể dùng số lượng cũ. | Hiển thị bản nháp ngay, gộp +/− trong 300 ms, khóa bộ chỉnh số lượng đó trong lúc lưu và bỏ qua cập nhật không thay đổi. |
| `CartDrawer` | Link HTML đến `/cart` tải lại ứng dụng và khởi tạo lại dữ liệu. | Dùng React Router Link. |

Hàng đợi mutation vẫn được giữ để các request sử dụng đúng `cartVersion`. Chưa có benchmark kết nối database thật nên không kết luận thời gian giảm theo ms hoặc phần trăm.

## Giao diện và nhập số lượng

- Dùng chung `CartQuantity` cho trang cart và drawer.
- Nhập trực tiếp, nhấn Enter hoặc “Cập nhật” để lưu; rời nhóm điều khiển cũng lưu.
- +/− cập nhật bản nháp ngay và tự lưu sau 300 ms không bấm tiếp.
- Chỉ nhận số nguyên 1–99, giới hạn theo lượng khả dụng server trả về. Backend tiếp tục kiểm tra tồn kho và phiên bản.
- Nút giảm dừng ở 1; dùng nút thùng rác để xóa sản phẩm.
- Escape hủy bản nháp. Có trạng thái đang lưu, lỗi tại dòng và khôi phục số lượng nếu request thất bại.
- Chuẩn hóa gap trang và hai cột 24 px; gap dòng 16 px; padding dòng 20 px; khoảng cách tiêu đề, nút và phần tổng tiền đồng nhất. Giữ bố cục responsive một cột trên mobile.
- Có trạng thái đang tải, tránh hiển thị giỏ rỗng khi request đầu chưa xong; ảnh hỗ trợ lazy loading/async decoding.

## Kiểm tra

- Frontend: `cd frontend; npm run build`.
- Backend: `./mvnw.cmd -q "-Dtest=CartServiceTest,CartStorageServiceTest,CartOwnerResolverTest,CartRepositoryTest,CartProductQueryTest" test` (20 tests).
- Chrome headless, API giả lập cục bộ: nhập và submit; ba lần tăng nhanh chỉ tạo một PATCH; chặn rỗng/0/thập phân/chữ/vượt tồn kho; lỗi 409 tồn kho giữ tổng tiền và khôi phục số lượng; blur tự lưu.
- Đo overflow bằng DOM ở viewport 1440, 390, 320 px: không tràn ngang. Đã xem ảnh chụp mobile.

Các kiểm tra trình duyệt dùng API giả lập; các kiểm tra repository dùng JdbcTemplate mock. Chưa chạy tích hợp PostgreSQL hay benchmark với mạng/database thực tế.
