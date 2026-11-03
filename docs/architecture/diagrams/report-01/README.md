# Danh mục sơ đồ báo cáo lần 1

**18 sơ đồ:** 1 use case tổng quát, 12 activity, 1 ERD tổng và 4 ERD chi tiết. Không tạo use case riêng cho từng nút CRUD/OTP.

| Mã | Nội dung | Mục đích | Nguồn | PNG |
|---|---|---|---|---|
| UC-OVERVIEW | Use case tổng quát | Ranh giới hệ thống, actors và 12 mục tiêu người dùng | [PlantUML](source/UC-OVERVIEW.puml) | [Ảnh](png/UC-OVERVIEW.png) |
| ACT-UC01 | Tài khoản và xác thực | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC01 | [PlantUML](source/ACT-UC01.puml) | [Ảnh](png/ACT-UC01.png) |
| ACT-UC02 | Tìm và xem PC | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC02 | [PlantUML](source/ACT-UC02.puml) | [Ảnh](png/ACT-UC02.png) |
| ACT-UC03 | Tư vấn chọn PC | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC03 | [PlantUML](source/ACT-UC03.puml) | [Ảnh](png/ACT-UC03.png) |
| ACT-UC04 | Quản lý giỏ hàng | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC04 | [PlantUML](source/ACT-UC04.puml) | [Ảnh](png/ACT-UC04.png) |
| ACT-UC05 | Đặt hàng và thanh toán | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC05 | [PlantUML](source/ACT-UC05.puml) | [Ảnh](png/ACT-UC05.png) |
| ACT-UC06 | Theo dõi và hủy đơn của tôi | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC06 | [PlantUML](source/ACT-UC06.puml) | [Ảnh](png/ACT-UC06.png) |
| ACT-UC07 | Quản lý PC và loại sản phẩm | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC07 | [PlantUML](source/ACT-UC07.puml) | [Ảnh](png/ACT-UC07.png) |
| ACT-UC08 | Xử lý đơn hàng | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC08 | [PlantUML](source/ACT-UC08.puml) | [Ảnh](png/ACT-UC08.png) |
| ACT-UC09 | Quản lý tài khoản khách hàng | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC09 | [PlantUML](source/ACT-UC09.puml) | [Ảnh](png/ACT-UC09.png) |
| ACT-UC10 | Tra cứu bảo hành và tiến độ | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC10 | [PlantUML](source/ACT-UC10.puml) | [Ảnh](png/ACT-UC10.png) |
| ACT-UC11 | Gửi hoặc hủy yêu cầu bảo hành | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC11 | [PlantUML](source/ACT-UC11.puml) | [Ảnh](png/ACT-UC11.png) |
| ACT-UC12 | Tiếp nhận và xử lý bảo hành | Các bước Bn và nhánh E-Bn trong bảng đặc tả UC12 | [PlantUML](source/ACT-UC12.puml) | [Ảnh](png/ACT-UC12.png) |
| ERD-ALL | Mô hình dữ liệu ALL | PK/FK, tùy chọn và bội số; bảng đầy đủ trong từ điển dữ liệu | [PlantUML](source/ERD-ALL.puml) | [Ảnh](png/ERD-ALL.png) |
| ERD-IDENTITY | Mô hình dữ liệu IDENTITY | PK/FK, tùy chọn và bội số; bảng đầy đủ trong từ điển dữ liệu | [PlantUML](source/ERD-IDENTITY.puml) | [Ảnh](png/ERD-IDENTITY.png) |
| ERD-CATALOG | Mô hình dữ liệu CATALOG | PK/FK, tùy chọn và bội số; bảng đầy đủ trong từ điển dữ liệu | [PlantUML](source/ERD-CATALOG.puml) | [Ảnh](png/ERD-CATALOG.png) |
| ERD-SALES-PAYMENT | Mô hình dữ liệu SALES-PAYMENT | PK/FK, tùy chọn và bội số; bảng đầy đủ trong từ điển dữ liệu | [PlantUML](source/ERD-SALES-PAYMENT.puml) | [Ảnh](png/ERD-SALES-PAYMENT.png) |
| ERD-AFTERSALES | Mô hình dữ liệu AFTERSALES | PK/FK, tùy chọn và bội số; bảng đầy đủ trong từ điển dữ liệu | [PlantUML](source/ERD-AFTERSALES.puml) | [Ảnh](png/ERD-AFTERSALES.png) |

## Cách dựng lại

Chạy `python docs/tools/report01/build.py`, rồi `python docs/tools/report01/render.py` từ repository. Bộ dựng chỉ viết tài liệu/sơ đồ; không sinh code ứng dụng hay migration. Java và Python cần sẵn; renderer tải PlantUML 1.2025.4 từ Maven Central vào thư mục temp nếu chưa có. Nội dung diagram được render tại máy, không upload dữ liệu lên dịch vụ vẽ online.

`model-input.xml` là định dạng trung gian của bộ sinh Open API, **không phải** XML trao đổi của Visual Paradigm. Project `.vpp` và XML do VP xuất được kiểm riêng trong [kết quả kiểm tra](../../../reports/REPORT_01_VALIDATION.md).

## Visual Paradigm

[Mở project VPP](PC_STORE_REPORT_01.vpp) — 1 use case tổng và 12 activity dạng phần tử native. [XML trao đổi](vp-export/project.xml) giữ cùng [data.zip](vp-export/data.zip). 5 ERD bàn giao bằng nguồn PlantUML và PNG ở bảng trên; chưa có ERD native do API tạo khóa ngoại báo lỗi.
