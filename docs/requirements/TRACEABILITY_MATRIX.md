# Ma trận truy vết — 12 chức năng chính

CRUD, OTP và callback là luồng con. Các yêu cầu FR dưới đây tương ứng 12 mục tiêu, không tạo thêm scope. Tiêu chí AC là kế hoạch nghiệm thu trước code, chưa phải test ứng dụng đã chạy.

| Yêu cầu / UC | Chức năng | Màn hình | Activity | Dữ liệu | Quy tắc |
|---|---|---|---|---|---|
| FR-01 / [UC01](FUNCTIONAL_SPECIFICATION.md#uc01) | Tài khoản và xác thực | A01–A05, C01 | [ACT-UC01](../architecture/diagrams/report-01/png/ACT-UC01.png) | users, user_roles, external_identities, verification_challenges | BR01 BR02 BR14 |
| FR-02 / [UC02](FUNCTIONAL_SPECIFICATION.md#uc02) | Tìm và xem PC | S01–S03 | [ACT-UC02](../architecture/diagrams/report-01/png/ACT-UC02.png) | products, categories, product_specs, product_images, product_suitability | BR03 BR15 |
| FR-03 / [UC03](FUNCTIONAL_SPECIFICATION.md#uc03) | Tư vấn chọn PC | S04 | [ACT-UC03](../architecture/diagrams/report-01/png/ACT-UC03.png) | products, product_specs, product_suitability, product_images | BR10 |
| FR-04 / [UC04](FUNCTIONAL_SPECIFICATION.md#uc04) | Quản lý giỏ hàng | S05; nút thêm tại S02–S04 | [ACT-UC04](../architecture/diagrams/report-01/png/ACT-UC04.png) | products, product_specs, product_images | BR04 |
| FR-05 / [UC05](FUNCTIONAL_SPECIFICATION.md#uc05) | Đặt hàng và thanh toán | C02–C03 | [ACT-UC05](../architecture/diagrams/report-01/png/ACT-UC05.png) | users, products, orders, order_items, payment_attempts, payment_events, order_status_history, idempotency_records | BR05 BR06 BR07 BR08 BR09 BR14 |
| FR-06 / [UC06](FUNCTIONAL_SPECIFICATION.md#uc06) | Theo dõi và hủy đơn của tôi | C04–C05 | [ACT-UC06](../architecture/diagrams/report-01/png/ACT-UC06.png) | orders, order_items, order_revisions, order_status_history, payment_attempts, products | BR02 BR06 BR07 BR09 |
| FR-07 / [UC07](FUNCTIONAL_SPECIFICATION.md#uc07) | Quản lý PC và loại sản phẩm | M01–M03 | [ACT-UC07](../architecture/diagrams/report-01/png/ACT-UC07.png) | categories, products, product_specs, product_images, product_suitability, inventory_adjustments, order_items | BR03 BR06 BR10 BR13 |
| FR-08 / [UC08](FUNCTIONAL_SPECIFICATION.md#uc08) | Xử lý đơn hàng | M04–M05 | [ACT-UC08](../architecture/diagrams/report-01/png/ACT-UC08.png) | orders, order_items, products, payment_attempts, payment_events, order_revisions, order_status_history | BR06 BR07 BR08 BR09 BR11 |
| FR-09 / [UC09](FUNCTIONAL_SPECIFICATION.md#uc09) | Quản lý tài khoản khách hàng | M06 | [ACT-UC09](../architecture/diagrams/report-01/png/ACT-UC09.png) | users, user_roles, external_identities, verification_challenges, orders | BR01 BR02 BR15 |
| FR-10 / [UC10](FUNCTIONAL_SPECIFICATION.md#uc10) | Tra cứu bảo hành và tiến độ | C05–C07 | [ACT-UC10](../architecture/diagrams/report-01/png/ACT-UC10.png) | orders, order_items, warranty_requests, warranty_request_images, warranty_request_events | BR02 BR11 BR12 BR13 |
| FR-11 / [UC11](FUNCTIONAL_SPECIFICATION.md#uc11) | Gửi hoặc hủy yêu cầu bảo hành | C07 | [ACT-UC11](../architecture/diagrams/report-01/png/ACT-UC11.png) | orders, order_items, warranty_requests, warranty_request_images, warranty_request_events | BR11 BR12 BR13 BR14 |
| FR-12 / [UC12](FUNCTIONAL_SPECIFICATION.md#uc12) | Tiếp nhận và xử lý bảo hành | M07 | [ACT-UC12](../architecture/diagrams/report-01/png/ACT-UC12.png) | warranty_requests, warranty_request_images, warranty_request_events, orders, order_items, users | BR11 BR12 BR14 |

## Kiểm tra bao phủ màn hình

S01–S03→UC02; S04→UC03; S05→UC04; A01–A05/C01→UC01; C02–C03→UC05; C04–C05→UC06; C06→UC10; C07→UC10/UC11; M01–M03→UC07; M04–M05→UC08; M06→UC09; M07→UC12. Tổng đúng **24 screen IDs**.

## Đối chiếu module

identity→UC01/UC09; catalog→UC02/UC07; cart→UC04; advisory→UC03; sales→UC05/UC06/UC08; payment→UC05/UC08; aftersales→UC10/UC11/UC12. Có **7 module**, một use case có thể đi qua nhiều module, không cần tách thêm module để vẽ.

## Nghiệm thu nghiệp vụ trọng tâm

1. Khách tư vấn→xem PC→giỏ→verified phone→COD→Admin giao→xem bảo hành.
2. VNPAY đúng hạn, callback sai, callback trùng, hết hạn và success đến muộn; không oversell/đơn trùng.
3. Customer không xem đơn/ảnh/yêu cầu người khác; Admin không lộ password hoặc private note cho khách.
4. Xóa PC có đơn/category có PC/user có đơn bị chặn; khóa Admin cuối bị chặn.
5. Sửa lượng COD giữ giá snapshot và cập nhật reserved/payment amount; đã trả hoặc xác nhận bị chặn.
6. Bảo hành biên hết hạn, request trùng, hủy khi REQUESTED, từ chối có lý do và hoàn tất có kết quả.
