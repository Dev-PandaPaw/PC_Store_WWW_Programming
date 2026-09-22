# Cấu trúc repository

## 1. Nguyên tắc

Repository là một ứng dụng Spring Boot modular monolith. Mỗi module nghiệp vụ sở hữu dữ liệu và logic của mình; giao tiếp liên module qua API/port công khai. Cấu trúc hiện tại chỉ là khung thư mục, chưa chứa mã triển khai.

Java group và base package thống nhất là `iuh.fit.nhom16.pcstore`.

Không tạo package nghiệp vụ `common`, `utils` hoặc `shared` chung. Khi thật sự xuất hiện một khái niệm dùng lại, nhóm phải xác định module sở hữu hoặc ghi ADR trước khi mở rộng cấu trúc.

## 2. Cấu trúc cấp cao

```text
pc-store/
├── .github/                 # Workflow và template cộng tác
├── docs/                    # Đặc tả và tài liệu bàn giao
├── e2e/                     # Playwright: spec, page object, fixture, support
├── ops/                     # Caddy, backup, runbook, script và quan sát hệ thống
├── src/
│   ├── main/
│   │   ├── java/iuh/fit/nhom16/pcstore/
│   │   └── resources/
│   └── test/
├── .gitattributes
├── .gitignore
├── CONTRIBUTING.md
└── README.md
```

`pom.xml`, Maven Wrapper, `compose.yaml`, `.env.example`, Docker/Caddy config và CI workflow chưa được tạo vì đó là sản phẩm của bước bootstrap kỹ thuật, không phải scaffold rỗng.

## 3. Module nghiệp vụ

| Module | Trách nhiệm |
|---|---|
| `identity` | User, credential, external identity, OTP, hồ sơ và phân quyền |
| `catalog` | Product, category, brand, specification và ảnh |
| `advisory` | Hồ sơ nhu cầu, dữ liệu phù hợp, gợi ý và so sánh |
| `inventory` | Số dư, ledger, reservation và máy vật lý |
| `cart` | Giỏ hàng trong HTTP Session |
| `sales` | Checkout, order, revision và điều phối giao dịch |
| `payment` | Payment attempt, receipt, event và đối soát/hoàn tiền |
| `fulfillment` | Phân bổ serial, checklist, shipment và hàng hoàn |
| `aftersales` | Warranty entitlement và service request |
| `notification` | In-app notification, outbox và email |
| `audit` | Nhật ký thao tác nghiệp vụ |

Mỗi module có năm khu vực:

| Khu vực | Nội dung được phép đặt |
|---|---|
| `api` | Contract, command/result, event và port công khai cho module khác |
| `application` | Use case, orchestration, transaction boundary và authorization nghiệp vụ |
| `domain` | Entity, value object, policy, state transition và domain rule |
| `infrastructure` | JPA repository adapter, provider adapter và chi tiết kỹ thuật nội bộ |
| `web` | MVC/REST controller, form/request/response và web mapping |

`configuration` chỉ chứa cấu hình ghép nối nền tảng (`security`, `web`, `persistence`, `session`, `integration`, `scheduling`, `observability`), không chứa luật nghiệp vụ.

## 4. Tài nguyên giao diện

- `templates/storefront/`: trang công khai, catalog, tư vấn, so sánh và giỏ hàng.
- `templates/account/`: xác thực, hồ sơ, checkout, đơn hàng, thiết bị và hậu mãi của khách.
- `templates/admin/`: các màn hình vận hành theo từng nhóm công việc.
- `templates/fragments/`: layout và fragment tái sử dụng.
- `templates/mail/`: email do hệ thống render.
- `templates/errors/`: trang lỗi HTML.
- `static/css`, `static/js`, `static/images`: tài nguyên trình duyệt; JavaScript được chia theo bề mặt sử dụng.
- `db/migration/`: migration Flyway bất biến sau khi merge.

## 5. Kiểm thử

- `src/test/java/.../<module>/`: test đặt cùng namespace với module tương ứng.
- `architecture/`: kiểm tra ranh giới module và dependency bằng Spring Modulith.
- `integration/`: test nhiều adapter/module, transaction và Testcontainers.
- `support/`: fixture builder, test helper và cấu hình test dùng chung.
- `src/test/resources/fixtures/`: dữ liệu kiểm thử tĩnh.
- `src/test/resources/db/`: dữ liệu hỗ trợ kiểm thử database, không thay migration chính.
- `src/test/resources/contracts/`: mẫu callback và payload provider đã loại secret.
- `e2e/`: Playwright spec, page object, fixture và support cho luồng trình duyệt.

## 6. Tài liệu và vận hành

Tài liệu được chia theo artifact bàn giao thay vì theo thành viên. `ops/` chỉ chứa tài sản vận hành có thể thực thi hoặc áp dụng; hướng dẫn giải thích đặt ở `docs/operations/`.

## 7. Quy tắc thêm tệp đầu tiên

1. Chọn module sở hữu use case hoặc dữ liệu.
2. Chọn lớp theo trách nhiệm thực, không theo tên công nghệ đơn thuần.
3. Nếu cần gọi module khác, dùng contract trong `api`; không import `domain` hoặc `infrastructure` của module đó.
4. Controller MVC và REST cùng gọi application service; không gọi HTTP nội bộ lẫn nhau.
5. Tích hợp ngoài phải đi qua provider port, đặt implementation trong `infrastructure`.
6. Mọi thay đổi schema đi vào `db/migration`; dữ liệu demo/test đặt đúng khu vực, không trộn với migration production.
7. Cập nhật test và tài liệu trong cùng thay đổi khi contract hoặc hành vi thay đổi.
