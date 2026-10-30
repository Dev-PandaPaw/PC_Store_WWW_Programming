# PC Store

Luồng đặt hàng ORD01–ORD15 đã được bổ sung theo MVC, với checkout COD và tài khoản giả lập. Xem [hướng dẫn chạy và kiểm thử chức năng đặt hàng](docs/reports/ORDER_FEATURE_IMPLEMENTATION.md).

Kho mã nguồn cho đồ án website giới thiệu và bán máy PC trực tuyến.

## Trạng thái hiện tại

Repository đang ở giai đoạn **scaffold kiến trúc phiên bản 2.0**. Phạm vi đã được thu gọn cho đồ án nhóm 5 người; chưa có mã Java, giao diện, migration, cấu hình chạy, dependency hay pipeline thực thi.

Java group và base package của dự án: `iuh.fit.nhom16.pcstore`.

## Tài liệu bắt đầu

- [Kế hoạch toàn dự án](docs/PLAN.md)
- [Quy ước cấu trúc repository](docs/STRUCTURE.md)
- [Mục lục tài liệu](docs/README.md)
- [Quy trình cộng tác](CONTRIBUTING.md)

## Phạm vi scaffold

Khung hiện tại bao gồm:

- 6 module nghiệp vụ: `identity`, `catalog`, `cart`, `advisory`, `sales`, `aftersales`;
- cấu trúc tài nguyên Thymeleaf, static asset và Flyway;
- cấu trúc unit/integration/architecture test và E2E;
- khu vực tài liệu, vận hành và CI;
- tệp giữ chỗ để toàn bộ cấu trúc được Git theo dõi.

Luồng chính của sản phẩm là tư vấn PC theo nhu cầu → giỏ Session → đặt hàng COD → Admin xử lý đơn → Customer theo dõi bảo hành tối giản. Đặc tả UI có 24 màn hình chức năng.

Các tệp triển khai như `pom.xml`, Maven Wrapper, `compose.yaml`, `.env.example`, workflow CI và cấu hình Spring sẽ được tạo trong bước bootstrap kỹ thuật, khi nhóm bắt đầu hiện thực và chốt được cấu hình chạy thực tế.
