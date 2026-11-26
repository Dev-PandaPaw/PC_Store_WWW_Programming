"""One-time, idempotent synchronization of the accepted report decisions."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
def edit(name,replacements,addition):
 p=ROOT/name;s=p.read_text(encoding='utf-8')
 for a,b in replacements: s=s.replace(a,b)
 if '## Đồng bộ báo cáo lần 1 — v2.1' not in s: s=s.rstrip()+'\n\n'+addition.strip()+'\n'
 p.write_text(s,encoding='utf-8')
edit('docs/PLAN.md',[
('**Phiên bản:** 2.0','**Phiên bản:** 2.1'),
('Tạo order, order item, reservation và payment attempt trong transaction.','Tạo order, order item, marker `orders.reservation_status=HELD`, tăng `products.reserved_quantity` và tạo payment attempt trong cùng transaction; không có bảng reservation riêng.'),
('AWAITING_CONFIRMATION --> CANCELLED: customer or admin cancels','AWAITING_CONFIRMATION --> CANCELLED: COD PENDING only; customer or admin'),
('Sửa số lượng cần lý do; không cho đơn rỗng; giữ đơn giá snapshot và tính lại tổng/phí.','Sửa số lượng cần lý do; không cho đơn rỗng; giữ đơn giá/phí snapshot, tính lại tổng và cập nhật amount của COD attempt PENDING; chỉ tăng lượng PC còn ACTIVE.'),
('IPN xác minh chữ ký, reference, response code và số tiền','IPN xác minh chữ ký, merchant, reference, response code, transaction status và số tiền'),
('callback', 'callback')],
'''## Đồng bộ báo cáo lần 1 — v2.1

Báo cáo nhóm chức năng thành **12 use case chính**, không tách mỗi nút CRUD, OTP hoặc callback thành tính năng. Phạm vi vẫn **7 module, 24 màn hình, 20 bảng**; chưa code ứng dụng.

- [Đặc tả 12 chức năng](requirements/FUNCTIONAL_SPECIFICATION.md): bảng đặc tả, luồng con, activity và tiêu chí nghiệm thu.
- [Quy tắc nghiệp vụ](requirements/BUSINESS_RULES.md): BR01–BR15 và quyết định đồng bộ.
- [Thiết kế dữ liệu](data/DATABASE_DESIGN.md), [ma trận truy vết](requirements/TRACEABILITY_MATRIX.md), [báo cáo lần 1](reports/REPORT_01_ANALYSIS_AND_DESIGN.md).

Chốt chi tiết: S03 chỉ công bố ACTIVE; category không status; Admin user không có UI đổi role. Giá PC dương. Một order/một attempt, chỉ mở lại VNPAY PENDING cùng reference/deadline; FAILED cần hủy/đặt lại hoặc chờ hết hạn. Success đến sau hạn/hủy chuyển REVIEW_REQUIRED, không hồi sinh đơn. Bảo hành cộng tháng lịch tại Asia/Ho_Chi_Minh, có hiệu lực start≤now<end; yêu cầu đã gửi hợp lệ được xử lý sau end. District là trường địa chỉ tùy chọn. Reservation có HELD/RELEASED/CONSUMED để cập nhật tồn đúng một lần. User làm actor lịch sử không hard-delete.

Các quyết định trên bổ sung độ rõ cho v2.0, không mở rộng module/màn hình. Tài liệu thiết kế ghi rõ hiện trạng trước triển khai; không coi render diagram là nghiệm thu ứng dụng.
''')
edit('docs/ux/UI_SCREEN_SPEC.md',[
('**Phiên bản:** 2.0','**Phiên bản:** 2.1'),
('[PLAN.md](../PLAN.md) 2.0','[PLAN.md](../PLAN.md) 2.1'),
('[STRUCTURE.md](../STRUCTURE.md) 2.0','[STRUCTURE.md](../STRUCTURE.md) 2.1'),
('`INACTIVE/DISCONTINUED` không cho thêm giỏ nhưng vẫn có thể hiện thông tin khi route hợp lệ.','`INACTIVE/DISCONTINUED` trả 404 ở chi tiết công khai; lịch sử đơn vẫn hiển thị snapshot.'),
('Failed/cancelled: cho quay lại đơn hoặc thanh toán lại nếu server cho phép.','Failed/cancelled: cho quay lại đơn; FAILED phải hủy/đặt lại hoặc chờ hết hạn. Chỉ PENDING chưa hết hạn được mở lại gateway cùng reference, không tạo attempt mới.'),
('giá không âm; SKU/slug unique;','giá nguyên dương 1..1.000.000.000 VND; SKU/slug unique;'),
('Bảng tên, slug, mô tả ngắn, số sản phẩm và trạng thái.','Bảng tên, slug, mô tả ngắn và số sản phẩm; loại không có trạng thái riêng.'),
('role change chỉ xuất hiện khi backend cho phép.','không có thao tác đổi role trong scope UI.'),
('delta hoặc new onHand, reason bắt buộc','delta (khác 0), reason bắt buộc'),
('filter status và còn/hết hạn tại thời điểm tạo.','filter status; hiển thị còn/hết hạn hiện tại riêng với tính hợp lệ lúc gửi.'),
('tỉnh/thành, quận/huyện, phường/xã, địa chỉ chi tiết;','tỉnh/thành, quận/huyện tùy chọn, phường/xã, địa chỉ chi tiết;')],
'''## Đồng bộ báo cáo lần 1 — v2.1

[12 use case chính](../requirements/FUNCTIONAL_SPECIFICATION.md) bao phủ đúng24 màn hình qua [ma trận truy vết](../requirements/TRACEABILITY_MATRIX.md). Đây là cách nhóm chức năng để báo cáo, không phải tăng/giảm screen IDs.

- C02: quote10 phút, phí50.000 VND; giá và contact đổi phải xác nhận lại.
- C03/C05: một attempt/order; chỉ PENDING còn hạn có action mở lại cùng reference. REVIEW_REQUIRED không có lời hứa hoàn tiền.
- C06/C07: còn hạn khi start≤serverNow<end; cộng tháng lịch theo snapshot. Request cũ hợp lệ vẫn được theo dõi/xử lý sau end; không có serial.
- M05: sửa lượng COD cập nhật cả tổng đơn và COD amount, giữ phí/giá snapshot; lượng0 bỏ dòng nhưng không cho đơn rỗng. Chỉ tăng lượng PC ACTIVE.
- M06: user có order hoặc actor history không xóa; contact Admin đổi phải bỏ verified và không làm mất phương thức đăng nhập cuối.
- M07: thêm ghi chú trong drawer khi RECEIVED/PROCESSING; không đổi trạng thái, public/internal tách trường. Không reopen request đã đóng.
''')
edit('docs/STRUCTURE.md',[('**Phiên bản:** 2.0 — đồng bộ với `PLAN.md` 2.0','**Phiên bản:** 2.1 — đồng bộ với `PLAN.md` 2.1')],
'''## Đồng bộ báo cáo lần 1 — v2.1

Không thay cấu trúc ứng dụng hoặc thêm module. Tài liệu bổ sung:

```text
docs/
  requirements/FUNCTIONAL_SPECIFICATION.md  # 12 use case chính, bảng đặc tả và activity
  requirements/BUSINESS_RULES.md            # Quy tắc dùng chung và quyết định chi tiết
  requirements/TRACEABILITY_MATRIX.md      # UC ↔ UI ↔ dữ liệu ↔ nghiệm thu
  data/DATABASE_DESIGN.md                  # 20 bảng, ERD và dictionary
  reports/REPORT_01_ANALYSIS_AND_DESIGN.md  # Báo cáo có hình
  reports/REPORT_01_VALIDATION.md           # Bằng chứng kiểm artifact, không phải test app
  architecture/diagrams/report-01/         # Nguồn UML, ảnh, mô hình VP
  tools/report01/                          # Bộ dựng tài liệu, không phải code backend
```

Một use case có thể đi qua sales/payment/catalog; không vì sơ đồ mà thêm module. Runtime Java/Spring, database migration và UI code vẫn thuộc giai đoạn hiện thực sau.
''')
edit('docs/README.md',[
('Nội dung dự kiến','Nội dung'),
('phiên bản 2.0 đã thu gọn','phiên bản 2.1 đã thu gọn'),
('Chưa tạo tài liệu giả hoặc sơ đồ rỗng. Mỗi artifact sẽ được thêm khi có nội dung đủ để review.','Các tài liệu dưới đây là đặc tả trước triển khai, không phải bằng chứng ứng dụng đã được code.')],
'''## Đồng bộ báo cáo lần 1 — v2.1

Đọc theo thứ tự:

1. [Báo cáo lần 1 — 12 chức năng chính và các sơ đồ](reports/REPORT_01_ANALYSIS_AND_DESIGN.md).
2. [Bảng đặc tả đầy đủ 12 chức năng](requirements/FUNCTIONAL_SPECIFICATION.md).
3. [Danh mục sơ đồ và mã nguồn dựng lại](architecture/diagrams/report-01/README.md).
4. [Thiết kế dữ liệu20 bảng](data/DATABASE_DESIGN.md).
5. [Ma trận truy vết24 màn hình](requirements/TRACEABILITY_MATRIX.md).
6. [Kết quả kiểm tra tài liệu và sơ đồ](reports/REPORT_01_VALIDATION.md).

[Quy tắc chi tiết](requirements/BUSINESS_RULES.md) là phụ lục tra cứu; khi báo cáo tập trung bảng đặc tả, use case tổng quát và activity của12 chức năng chính.
''')
print('Synchronized PLAN, UI spec, STRUCTURE and docs index')
