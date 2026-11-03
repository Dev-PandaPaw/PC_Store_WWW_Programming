# Cấu trúc repository PC Store

**Phiên bản:** 2.1 — đồng bộ với `PLAN.md` 2.1

**Base package:** `iuh.fit.nhom16.pcstore`

## 1. Nguyên tắc

Repository chứa một ứng dụng Spring Boot modular monolith gồm bảy module nghiệp vụ. Cấu trúc chỉ tạo ranh giới cần thiết cho đồ án; không tạo package hoặc lớp rỗng ngoài nhu cầu triển khai.

- Mỗi module sở hữu entity, repository và luật nghiệp vụ của mình.
- Module khác gọi contract công khai trong `api` hoặc application service được công bố.
- MVC và REST controller cùng gọi application service; không gọi HTTP nội bộ.
- Transaction boundary nằm tại application service.
- Tích hợp ngoài đi qua port/adapter.
- Không có package tổng hợp `common`, `shared` hoặc `utils`.
- Chỉ thêm abstraction khi có ít nhất một use case thực tế cần nó.

## 2. Cấu trúc cấp cao

```text
pc-store/
├── .github/                    # Workflow và template cộng tác
├── docs/                       # Kế hoạch, yêu cầu, kiến trúc, UI, test
├── e2e/                        # Playwright specs, pages, fixtures, support
├── ops/                        # Runbook và script vận hành tối thiểu
├── src/
│   ├── main/
│   │   ├── java/iuh/fit/nhom16/pcstore/
│   │   │   ├── identity/
│   │   │   ├── catalog/
│   │   │   ├── cart/
│   │   │   ├── advisory/
│   │   │   ├── sales/
│   │   │   ├── payment/
│   │   │   ├── aftersales/
│   │   │   └── configuration/
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── templates/
│   │       └── static/
│   └── test/
├── .gitattributes
├── .gitignore
├── CONTRIBUTING.md
└── README.md
```

`pom.xml`, Maven Wrapper, `compose.yaml`, `.env.example`, cấu hình Spring và workflow CI sẽ được tạo trong bước bootstrap kỹ thuật. Tài liệu cấu trúc không giả định chúng đã tồn tại.

## 3. Module nghiệp vụ

| Module | Dữ liệu và trách nhiệm |
|---|---|
| `identity` | User, role, credential, external identity, OTP, profile, địa chỉ mặc định và quản trị tài khoản |
| `catalog` | Product, category, specification, image, dữ liệu phù hợp tư vấn, stock/reservation và điều chỉnh tồn |
| `cart` | Giỏ hàng trong HTTP Session và cart snapshot |
| `advisory` | Tiêu chí tư vấn, lọc, xếp hạng và giải thích kết quả |
| `sales` | Checkout preview, order, item snapshot, revision, status history và email đơn |
| `payment` | COD, VNPAY payment attempt, callback event và payment state |
| `aftersales` | Eligibility bảo hành theo order item, yêu cầu và lịch sử xử lý |

Các module `inventory`, `fulfillment`, `notification` và `audit` của phiên bản 1.0 đã được loại. `aftersales` được giữ lại ở mức tối giản, không quản lý serial, vận chuyển bảo hành, linh kiện sửa chữa, đổi máy hoặc hoàn tiền. Phần tồn kho tối thiểu thuộc `catalog`; email đơn thuộc `sales`; lịch sử order thuộc `sales`.

## 4. Cấu trúc bên trong module

```text
<module>/
├── api/                # Contract công khai cho module khác
├── application/        # Use case, orchestration, transaction boundary
├── domain/             # Entity, value object, policy và state transition
├── infrastructure/     # JPA adapter và provider adapter nội bộ
└── web/                # MVC/REST controller, form và response mapping
```

Không phải module nào cũng cần đủ mọi package ngay từ đầu. Package chỉ được giữ trong scaffold để thống nhất vị trí code; khi triển khai, lớp phải đặt theo trách nhiệm:

- `api`: command/result hoặc facade thực sự được module khác gọi;
- `application`: service/use case, không chứa chi tiết HTML hoặc JPA query;
- `domain`: luật không phụ thuộc Spring MVC;
- `infrastructure`: persistence/provider implementation;
- `web`: route, validation form/request và chuyển đổi response.

Không import `domain`, `infrastructure` hoặc repository nội bộ của module khác.

## 5. Configuration

```text
configuration/
├── security/           # SecurityFilterChain, OAuth2, CSRF, authorization
├── web/                # MVC, locale, error handling, static resources
├── persistence/        # JPA/Flyway/database wiring
└── integration/        # HTTP client, mail và provider properties
```

Không còn package cấu hình Redis session, scheduling platform hoặc observability riêng trong scaffold v2. Job hết hạn payment/order có thể đặt trong `payment.infrastructure` hoặc cấu hình scheduling tối thiểu khi use case được hiện thực.

## 6. Thymeleaf và static assets

```text
templates/
├── storefront/
│   ├── home/
│   ├── catalog/
│   ├── advisory/
│   └── cart/
├── account/
│   ├── auth/
│   ├── profile/
│   ├── checkout/
│   ├── orders/
│   └── warranties/
├── admin/
│   ├── catalog/
│   ├── categories/
│   ├── orders/
│   ├── users/
│   └── warranties/
├── fragments/
├── mail/
└── errors/

static/
├── css/
├── images/
└── js/
    ├── storefront/
    ├── account/
    └── admin/
```

- `fragments` chứa layout, header, footer, form control, pagination và status badge dùng lại.
- `mail` chỉ chứa email đăng ký, xác minh/reset và đơn hàng.
- Không tạo template cho so sánh, thiết bị/serial, dashboard, kho riêng, checklist, vận đơn, thanh toán Admin hoặc audit. Bảo hành chỉ dùng hai màn hình Customer và một màn hình Admin theo UI spec.
- JavaScript chia theo bề mặt sử dụng, không nhúng business rule có thẩm quyền vào trình duyệt.

## 7. Database migration

Migration đặt tại `src/main/resources/db/migration/`:

```text
V1__identity.sql
V2__catalog_and_advisory_data.sql
V3__sales.sql
V4__payments.sql
V5__aftersales.sql
V6__seed_demo_reference_data.sql    # chỉ khi nhóm quyết định seed qua Flyway
```

Quy tắc:

- migration đã merge không sửa lại;
- schema change dùng migration mới;
- seed demo không chứa secret hoặc dữ liệu cá nhân thật;
- test fixture không trộn vào migration production;
- Hibernate dùng `validate` ở demo/staging.

## 8. Kiểm thử

```text
src/test/java/iuh/fit/nhom16/pcstore/
├── identity/
├── catalog/
├── cart/
├── advisory/
├── sales/
├── payment/
├── aftersales/
├── integration/
├── architecture/
└── support/

src/test/resources/
├── fixtures/
├── db/
└── contracts/

e2e/
├── specs/
├── pages/
├── fixtures/
└── support/
```

- Unit test đặt cùng namespace module.
- Integration test dùng PostgreSQL Testcontainers cho transaction và concurrency.
- `contracts` chứa VNPAY/Google payload mẫu đã loại secret.
- E2E chỉ bao phủ luồng chính trong `PLAN.md`; không tạo page object cho màn hình ngoài phạm vi.

## 9. Tài liệu và vận hành

```text
docs/
├── PLAN.md
├── STRUCTURE.md
├── README.md
├── requirements/
├── architecture/
├── data/
├── api/
├── ux/
├── testing/
├── operations/
└── project/

ops/
├── backup/
├── runbooks/
└── scripts/
```

Không giữ thư mục Caddy và observability trong scaffold v2. Nếu nhóm chọn deploy public, tài sản reverse proxy hoặc metric được thêm cùng ADR và runbook thực tế.

## 10. Quy tắc thêm file

1. Xác định module sở hữu dữ liệu/use case.
2. Đặt controller/form vào `web`, orchestration vào `application`, luật vào `domain`, kỹ thuật vào `infrastructure`.
3. Nếu module khác cần gọi, công bố contract nhỏ trong `api`.
4. Không để controller truy cập repository trực tiếp.
5. Tích hợp Google, OTP, VNPAY hoặc mail phải có provider adapter.
6. Thay đổi schema luôn có migration và integration test tương xứng.
7. Thay đổi contract phải cập nhật `PLAN.md`, UI/API spec và test trong cùng PR.
8. Không tạo lớp/file giữ chỗ ngoài scaffold hoặc code chưa dùng.

## Đồng bộ báo cáo lần 1 — v2.1

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
