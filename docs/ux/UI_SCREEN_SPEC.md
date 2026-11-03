# PC Store — Đặc tả UI 24 màn hình

**Phiên bản:** 2.1 — 26/09/2026

**Nguồn nghiệp vụ:** [PLAN.md](../PLAN.md) 2.1

**Nguồn cấu trúc:** [STRUCTURE.md](../STRUCTURE.md) 2.1
**Đích hiện thực:** Spring MVC, Thymeleaf, Bootstrap 5.3, CSS và JavaScript modules

Khi có xung đột, `PLAN.md` được ưu tiên. Tài liệu này mô tả giao diện cần hiện thực, không chứng minh controller, API hoặc template đã tồn tại.

## 1. Mục tiêu và phạm vi

UI phục vụ một luồng ngắn, rõ và có thể hoàn thành:

```text
Tư vấn PC → xem sản phẩm → giỏ → xác thực → checkout
→ COD/VNPAY sandbox → theo dõi đơn → Admin xử lý đơn
```

Tài liệu có đúng **24 màn hình chức năng**, tính cả form và detail Admin. Modal, state, responsive variant và email không được tính là màn hình mới.

Không thiết kế:

- so sánh PC riêng;
- nhiều địa chỉ hoặc trang quản lý phương thức đăng nhập riêng;
- máy/serial, checklist, vận chuyển bảo hành, linh kiện sửa chữa, đổi máy và hoàn tiền;
- dashboard, kho/ledger riêng, payment Admin, audit và notification center;
- màn hình quản trị rule tư vấn.

## 2. Design system

### 2.1. Phong cách

Giao diện cửa hàng công nghệ sáng, rõ cấu hình và giá. Ảnh PC là điểm nhấn; navy dùng cho cấu trúc, blue cho hành động chính và teal cho tư vấn. Không dùng giao diện gaming đen toàn trang, gradient neon hoặc animation liên tục.

Tên hiển thị: **PC Store**. Tagline: “Chọn đúng PC theo nhu cầu.”

### 2.2. Tokens

| Token | Giá trị | Công dụng |
|---|---|---|
| `--pc-bg` | `#F5F7FB` | Nền trang |
| `--pc-surface` | `#FFFFFF` | Card/form/table |
| `--pc-surface-muted` | `#EEF2F6` | Header bảng/vùng phụ |
| `--pc-text` | `#0F172A` | Chữ chính |
| `--pc-text-secondary` | `#475569` | Mô tả |
| `--pc-border` | `#CBD5E1` | Viền |
| `--pc-primary` | `#1D4ED8` | CTA/link/selected |
| `--pc-primary-hover` | `#1E40AF` | Hover/pressed |
| `--pc-primary-soft` | `#EFF6FF` | Nền selected |
| `--pc-accent` | `#0F766E` | Tư vấn |
| `--pc-accent-soft` | `#F0FDFA` | Nền giải thích |
| `--pc-success` | `#166534` | Thành công |
| `--pc-warning` | `#92400E` | Chờ/chú ý |
| `--pc-danger` | `#B91C1C` | Lỗi/hủy |
| `--pc-focus` | `#2563EB` | Focus outline |

- Font: Inter, fallback `system-ui, Arial, sans-serif`, hỗ trợ tiếng Việt.
- Body: 16/24 px. Bảng Admin: 14/20 px.
- H1: 32/40 px desktop, 26/34 px mobile.
- Spacing: 4, 8, 12, 16, 24, 32, 48, 64 px.
- Control cao tối thiểu 44 px; CTA checkout 48 px.
- Radius control 8 px, card 12 px, hero 16 px.
- Focus phải thấy rõ; status luôn có icon/nhãn, không truyền nghĩa chỉ bằng màu.
- Tương phản đạt WCAG AA; hỗ trợ keyboard, zoom 200% và `prefers-reduced-motion`.

### 2.3. Responsive

| Viewport | Quy ước |
|---|---|
| ≥1440 | Container storefront tối đa 1280 px; gutter 24 px |
| 1024–1439 | Container `calc(100% - 64px)` |
| 768–1023 | Container `calc(100% - 48px)`; grid 2 cột |
| <768 | Container `calc(100% - 32px)`; một cột |
| Admin ≥1200 | Sidebar 232 px; content padding 32 px |
| Admin <1200 | Sidebar thành drawer; content padding 16–24 px |

Grid catalog: desktop 3–4 cột tùy chiều rộng, tablet 2 cột, mobile 1 cột. Checkout desktop có form và summary 360 px; dưới 1024 xếp một cột.

### 2.4. Định dạng

- Tiền: `18.490.000 ₫`.
- Ngày: `26/09/2026`; thời gian: `26/09/2026, 14:30`.
- SKU dùng monospace; số tiền/cột số dùng tabular numerals và căn phải.
- Form bắt buộc có label và dấu `*`; placeholder không thay label.
- UI Customer không hiển thị UUID, enum, exception, class name hoặc từ IPN/reservation.
- Demo hiển thị “OTP giả lập” và “VNPAY sandbox” đúng ngữ cảnh.

## 3. Shell

### 3.1. `L-STORE`

- Skip link; demo ribbon khi cần.
- Header: logo, search, “Tư vấn chọn PC”, tài khoản và giỏ có số lượng.
- Navigation: “Tất cả PC”, “Tư vấn chọn PC”, “Đơn hàng của tôi” khi đăng nhập.
- Footer: giới thiệu, mua hàng và tài khoản; không bịa địa chỉ/hotline.
- Mobile: logo/menu/cart trên hàng đầu, search hàng riêng, navigation trong drawer.

### 3.2. `L-AUTH`

Header gọn có logo và “Về cửa hàng”. Form rộng tối đa 440 px. Có `returnTo` nội bộ để quay lại checkout; không hiện token hoặc URL kỹ thuật.

### 3.3. `L-ACCOUNT`

Kế thừa storefront. Desktop sidebar gồm “Hồ sơ”, “Đơn hàng”, “Bảo hành của tôi” và “Đăng xuất”. Mobile dùng menu dropdown/drawer.

### 3.4. `L-CHECKOUT`

Header gọn; tiến trình “Giỏ hàng → Xác nhận đơn → Kết quả”. Desktop: form + summary 360 px. Điều hướng quay lại không làm mất dữ liệu.

### 3.5. `L-ADMIN`

Sidebar chỉ gồm:

- Sản phẩm;
- Loại sản phẩm;
- Đơn hàng;
- Tài khoản;
- Bảo hành;
- Xem cửa hàng.

`/admin` redirect tới `/admin/orders`. Không có dashboard. Header có breadcrumb, tên Admin và logout. Mobile sidebar là drawer có focus trap và đóng bằng Escape.

## 4. Component dùng chung

| ID | Component | Contract chính |
|---|---|---|
| C01 | Header | currentUser, cartCount, activeNav; search/logout/openCart |
| C02 | PageHeading | title, description, breadcrumbs, primaryAction |
| C03 | Button | variant, disabledReason, pending; chống double click |
| C04 | Field | name, label, value, required, hint, fieldError |
| C05 | Select/Choice | options, value, disabled options, semantic input |
| C06 | ProductCard | id, slug, name, image, price, specs, stockState, addToCart |
| C07 | SpecTable | ordered label/value/unit; thiếu ghi “Chưa có dữ liệu” |
| C08 | Quantity | integer ≥1; remove là action riêng |
| C09 | StatusBadge | domain, code, label, tone |
| C10 | MoneySummary | subtotal, shippingFee, total, quoteExpiry |
| C11 | DataTable | columns, rows, sort, paging, rowActions, empty/error |
| C12 | FilterBar | query, filters, chips; apply/reset; giữ filter trong URL |
| C13 | Pagination | page, size, total; catalog 12, Admin 20 mặc định |
| C14 | Feedback | loading/empty/error/success/forbidden + retry/next action |
| C15 | ConfirmDialog | title, consequence, confirmLabel, pending, focus management |
| C16 | Upload | JPEG/PNG/WebP ≤5 MB, preview, cover, reorder, retry |
| C17 | Timeline | event time, actor, label, public note |
| C18 | ProductFormSections | basic, price/stock, specs, advisory, images |
| C19 | OrderActions | allowedActions từ server, confirm dialog và reason |
| C20 | EnvironmentLabel | OTP giả lập/VNPAY sandbox |
| C21 | WarrantyCard | orderItem, unitIndex, startAt, expiresAt, eligibility, requestAction |

### 4.1. State bắt buộc

Mỗi màn hình áp dụng các state phù hợp:

- `loading`: skeleton đúng footprint;
- `ready`;
- `empty`: có giải thích và CTA/xóa filter;
- `validationError`: error summary + lỗi dưới field;
- `requestError`: thông báo và retry có chủ đích;
- `unauthorized`: dẫn login với returnTo nội bộ;
- `forbidden`: trang 403 không tiết lộ dữ liệu;
- `conflict`: dữ liệu/tồn đã thay đổi, yêu cầu tải lại;
- `success`: toast cho mutation nhỏ, result page cho đặt hàng/payment.

Không tự retry tạo order/payment bằng key mới. Pending khóa đúng CTA, không khóa toàn trang. Response tìm kiếm cũ đến muộn phải bị bỏ.

### 4.2. Badge

| Domain/code | Nhãn |
|---|---|
| Order `AWAITING_PAYMENT` | Chờ thanh toán |
| Order `AWAITING_CONFIRMATION` | Chờ xác nhận |
| Order `CONFIRMED` | Đã xác nhận |
| Order `SHIPPING` | Đang giao |
| Order `DELIVERED` | Đã giao |
| Order `CANCELLED` | Đã hủy |
| Order `EXPIRED` | Hết hạn |
| Payment `PENDING` | Chờ thanh toán |
| Payment `PAID` | Đã thanh toán |
| Payment `FAILED` | Thất bại |
| Payment `REVIEW_REQUIRED` | Cần kiểm tra |
| Product `ACTIVE` | Đang bán |
| Product `INACTIVE` | Tạm ẩn |
| Product `DISCONTINUED` | Ngừng bán |
| User `ACTIVE` | Hoạt động |
| User `LOCKED` | Đã khóa |
| Warranty `REQUESTED` | Đã gửi yêu cầu |
| Warranty `RECEIVED` | Đã tiếp nhận |
| Warranty `PROCESSING` | Đang xử lý |
| Warranty `COMPLETED` | Đã hoàn tất |
| Warranty `REJECTED` | Đã từ chối |
| Warranty `CANCELLED` | Đã hủy |

## 5. Danh mục 24 màn hình

| ID | Màn hình | Shell | Route |
|---|---|---|---|
| S01 | Trang chủ | STORE | `/` |
| S02 | Danh sách PC | STORE | `/pcs` |
| S03 | Chi tiết PC | STORE | `/pcs/{slug}` |
| S04 | Tư vấn và kết quả | STORE | `/advisory` |
| S05 | Giỏ hàng | STORE | `/cart` |
| A01 | Đăng nhập | AUTH | `/login` |
| A02 | Đăng ký | AUTH | `/register` |
| A03 | OTP | AUTH | `/auth/otp` |
| A04 | Quên mật khẩu | AUTH | `/forgot-password` |
| A05 | Đặt lại mật khẩu | AUTH | `/reset-password` |
| C01 | Hồ sơ | ACCOUNT | `/account/profile` |
| C02 | Checkout | CHECKOUT | `/checkout` |
| C03 | Kết quả đặt hàng/thanh toán | CHECKOUT | `/checkout/result` |
| C04 | Đơn hàng của tôi | ACCOUNT | `/account/orders` |
| C05 | Chi tiết đơn | ACCOUNT | `/account/orders/{code}` |
| C06 | Bảo hành của tôi | ACCOUNT | `/account/warranties` |
| C07 | Chi tiết/tạo yêu cầu bảo hành | ACCOUNT | `/account/warranties/{orderItemId}/{unitIndex}`, `/account/warranty-requests/{requestCode}` |
| M01 | Danh sách PC Admin | ADMIN | `/admin/products` |
| M02 | Thêm/sửa PC | ADMIN | `/admin/products/new`, `/admin/products/{id}/edit` |
| M03 | Loại sản phẩm | ADMIN | `/admin/categories` |
| M04 | Danh sách đơn Admin | ADMIN | `/admin/orders` |
| M05 | Chi tiết đơn Admin | ADMIN | `/admin/orders/{code}` |
| M06 | Quản lý tài khoản | ADMIN | `/admin/users` |
| M07 | Yêu cầu bảo hành Admin | ADMIN | `/admin/warranties` |

## 6. Storefront

### S01 — Trang chủ

- Hero: tiêu đề, mô tả ngắn, CTA “Nhận tư vấn” và “Xem tất cả PC”.
- Bốn thẻ nhu cầu dẫn S04 với profile được chọn sẵn.
- “PC nổi bật” tối đa 4 sản phẩm `ACTIVE`, còn hàng.
- Khối quy trình ba bước: nhu cầu → gợi ý → đặt hàng.
- Empty featured không làm mất CTA catalog/advisory.
- Mobile CTA xếp dọc; không carousel bắt buộc.

### S02 — Danh sách PC

- Search tên/SKU; filter loại, hãng, giá, RAM, GPU; sort.
- Card hiển thị ảnh, tên, CPU/GPU/RAM/storage, giá và tình trạng.
- Desktop filter sidebar; tablet/mobile filter drawer.
- Filter/sort/page nằm trong query string để back/refresh giữ trạng thái.
- Zero result có “Xóa bộ lọc”; lỗi tải có retry.
- Add cart gửi product ID và quantity; không gửi giá có thẩm quyền.

### S03 — Chi tiết PC

- Gallery; tên, SKU, giá, tình trạng, quantity và CTA.
- Bảng thông số đầy đủ; mô tả và số tháng bảo hành.
- Khối “Phù hợp với” hiển thị lý do/giới hạn đã nhập cho nhu cầu.
- `INACTIVE/DISCONTINUED` trả 404 ở chi tiết công khai; lịch sử đơn vẫn hiển thị snapshot.
- Hết hàng: khóa CTA, gợi ý quay lại catalog/tư vấn.

### S04 — Tư vấn và kết quả

Một màn hình gồm form trên và kết quả dưới để giảm route/template.

Form:

- `usageProfile` bắt buộc;
- `maxBudget` bắt buộc, VND;
- `minRamGb` lựa chọn;
- `minStorageGb` lựa chọn;
- submit “Gợi ý PC phù hợp”.

Kết quả tối đa ba card, mỗi card có score dạng chữ “Rất phù hợp/Phù hợp/Cân nhắc”, lý do, hạn chế, giá, ngân sách còn lại và CTA. Không hiển thị score như benchmark. Không có kết quả thì nêu tiêu chí đang giới hạn và giữ form để chỉnh.

### S05 — Giỏ hàng

- Danh sách ảnh/tên/cấu hình, giá hiện tại, quantity, line total và remove.
- Summary subtotal; phí ghi “Tính tại checkout”; CTA “Tiến hành đặt hàng”.
- Cảnh báo item đổi giá, hết hàng hoặc ngừng bán; bắt buộc xử lý trước checkout.
- Empty có CTA catalog và advisory.
- Conflict hai tab yêu cầu tải lại; không ghi đè dữ liệu mới âm thầm.

## 7. Xác thực và Customer

### A01 — Đăng nhập

- Tab email/mật khẩu và phone/OTP; nút Google.
- Email login validate format; password không echo sau lỗi.
- Phone chọn “Gửi OTP” chuyển A03 cùng challenge ID server-side.
- Link đăng ký/quên mật khẩu; giữ returnTo nội bộ.
- Lỗi credential dùng thông báo chung, không tiết lộ tài khoản tồn tại.

### A02 — Đăng ký

- Họ tên, email, mật khẩu, xác nhận mật khẩu.
- Tùy chọn đăng ký bằng phone hoặc Google theo action riêng.
- Thành công dẫn login/profile phù hợp và gửi email khi có email.
- Duplicate email/phone map inline.

### A03 — OTP

- Hiển thị mục đích và số phone đã mask.
- Sáu chữ số, countdown hết hạn, resend countdown và số lần còn lại.
- Nhãn “OTP giả lập” ở local/demo; không đưa OTP vào production UI.
- OTP hết hạn/sai/dùng lại có thông báo khác nhau nhưng không lộ secret.
- Thành công quay đúng luồng login/profile/checkout.

### A04 — Quên mật khẩu

- Nhập email; submit luôn hiển thị thông báo trung tính.
- Không xác nhận email có tồn tại.
- Link hợp lệ được gửi qua email provider.

### A05 — Đặt lại mật khẩu

- Mật khẩu mới và xác nhận; hiển thị yêu cầu mật khẩu.
- Token valid/expired/used có state riêng.
- Thành công dẫn login; token không xuất hiện lại trong nội dung trang/log.

### C01 — Hồ sơ

Gộp thông tin cá nhân, liên hệ và địa chỉ mặc định:

- họ tên;
- email và trạng thái xác minh;
- phone và trạng thái xác minh, action qua A03;
- người nhận, tỉnh/thành, quận/huyện tùy chọn, phường/xã, địa chỉ chi tiết;
- phương thức đã liên kết hiển thị read-only với action link/unlink khi hợp lệ.

Không có danh sách nhiều địa chỉ. Không cho bỏ phương thức đăng nhập cuối cùng. Update email/phone phải đi qua xác minh phù hợp.

### C02 — Checkout

- Kiểm tra authentication, cart, verified phone và address trước render.
- Form người nhận, địa chỉ snapshot, ghi chú và payment method COD/VNPAY.
- Phone đã xác minh hiển thị read-only; link sửa về hồ sơ.
- Summary đọc từ server: item, unit price, subtotal, phí 50.000 ₫, total và quote expiry.
- Hidden idempotency key, cart version và quote ID.
- Giá/tồn/quote thay đổi hiển thị conflict và nút cập nhật tóm tắt.
- VNPAY có nhãn sandbox và mô tả redirect ra cổng ngoài.

### C03 — Kết quả đặt hàng/thanh toán

Các state:

- COD tạo thành công: mã đơn, tổng tiền, chờ Admin xác nhận.
- VNPAY redirect pending: “Đang xác nhận thanh toán”, tự poll có giới hạn.
- Paid: đã nhận xác nhận server, dẫn chi tiết đơn.
- Failed/cancelled: cho quay lại đơn; FAILED phải hủy/đặt lại hoặc chờ hết hạn. Chỉ PENDING chưa hết hạn được mở lại gateway cùng reference, không tạo attempt mới.
- Expired: giải thích giữ hàng đã hết hạn.
- Review required: thông báo đang kiểm tra, không tuyên bố hoàn tiền.

Trang không suy `PAID` từ query string return URL.

### C04 — Đơn hàng của tôi

- Search mã; filter trạng thái; sort ngày mới nhất.
- Card/table: code, date, item summary, total, order badge, payment badge.
- Pagination và empty state.
- Mọi row dẫn C05; server lọc theo current user.

### C05 — Chi tiết đơn

- Mã, thời điểm, order/payment badges.
- Item snapshot, địa chỉ/phone snapshot và money summary.
- Timeline public của status/revision.
- Hành động hủy chỉ khi `allowedActions` cho phép; confirm nêu hậu quả.
- Với đơn `DELIVERED`, mỗi item hiển thị ngày bắt đầu/hết hạn bảo hành và CTA dẫn C07 khi đủ điều kiện.
- Không hiển thị internal payment payload hoặc ghi chú Admin.
- Order người khác trả 404/403 không tiết lộ chủ sở hữu.

### C06 — Bảo hành của tôi

- Tabs “Còn bảo hành”, “Hết hạn” và “Yêu cầu đã gửi”.
- Card theo từng order item; quantity lớn hơn 1 hiển thị từng vị trí máy `1..quantity`.
- Hiển thị product snapshot, order code, deliveredAt, warrantyMonths, expiresAt và trạng thái eligibility.
- CTA “Yêu cầu bảo hành” dẫn C07 khi còn hạn và chưa có yêu cầu đang mở.
- Yêu cầu đã gửi hiển thị code, mô tả ngắn, status, updatedAt và dẫn C07.
- Empty giải thích chỉ đơn đã giao mới xuất hiện; không quảng bá quyền lợi ngoài snapshot.

### C07 — Chi tiết/tạo yêu cầu bảo hành

Khi chưa có yêu cầu mở:

- Hiển thị order/product snapshot, unit index, thời hạn và số ngày còn lại do server trả.
- Form có `issueDescription` bắt buộc và tối đa ba ảnh JPEG/PNG/WebP ≤5 MB mỗi ảnh.
- Submit không nhận user ID, ngày hết hạn hoặc trạng thái từ client.
- Hết hạn, order chưa giao hoặc không thuộc Customer trả feedback phù hợp và không render form.

Khi đã có yêu cầu:

- Hiển thị request code, mô tả, ảnh, current status và timeline public.
- CTA hủy chỉ ở `REQUESTED`, có confirm.
- `REJECTED` hiển thị lý do; `COMPLETED` hiển thị kết quả xử lý.
- Không hiển thị internal note, chi phí, linh kiện hoặc lời hứa đổi/hoàn tiền.

## 8. Admin

### M01 — Danh sách PC

- Search tên/SKU; filter trạng thái/loại/hãng/tồn; sort/paging.
- Cột ảnh, SKU, tên, loại, giá, `onHand`, `reserved`, `available`, trạng thái và actions.
- CTA “Thêm PC”; actions sửa, ngừng bán/xóa theo quyền.
- Delete bị tham chiếu hiển thị lý do và gợi ý ngừng bán.
- Điều chỉnh tồn mở dialog: delta (khác 0), reason bắt buộc, preview before/after do server; chặn dưới reserved.

### M02 — Thêm/sửa PC

Form chia section:

1. Cơ bản: SKU, name, slug, category, brand, description.
2. Giá/trạng thái/tồn ban đầu.
3. Specs: CPU, GPU, RAM, storage, motherboard, PSU, case, OS, warranty.
4. Tư vấn: score/lý do/hạn chế cho bốn nhu cầu.
5. Gallery: upload, cover và order.

Rules:

- edit không cho sửa tồn bằng field thường; dùng dialog adjustment tại M01;
- giá nguyên dương 1..1.000.000.000 VND; SKU/slug unique;
- score 1–5, lý do bắt buộc khi có score;
- ảnh sai type/size giữ nguyên dữ liệu form và báo từng file;
- optimistic version conflict yêu cầu tải dữ liệu mới.

### M03 — Loại sản phẩm

- Bảng tên, slug, mô tả ngắn và số sản phẩm; loại không có trạng thái riêng.
- Thêm/sửa bằng form inline hoặc modal accessible.
- Xóa chỉ khi không có sản phẩm; không force cascade.
- Search, paging khi cần; empty có CTA tạo loại đầu tiên.

### M04 — Danh sách đơn Admin

- Search code/customer/phone; filter date, order status, payment method/status.
- Cột code, date, customer, items, total, order/payment badge và action xem.
- Mặc định sort mới nhất; trạng thái `REVIEW_REQUIRED` có cảnh báo dễ thấy.
- `/admin` redirect tới màn hình này.

### M05 — Chi tiết đơn Admin

- Snapshot customer/address/items và money summary.
- Payment attempt/event tóm tắt đã lọc secret.
- Timeline status và revisions.
- Action server cung cấp: confirm, revise COD quantity, cancel, mark shipping, mark delivered.
- Không có dropdown chọn status tùy ý.

Sửa quantity:

- chỉ COD, payment chưa trả, order `AWAITING_CONFIRMATION`;
- hiển thị old/new quantity, total cũ/mới và reservation impact;
- reason bắt buộc; không cho mọi item về 0;
- conflict tồn hoặc version yêu cầu refresh.

Payment đến muộn/sai lệch hiển thị panel “Cần kiểm tra”; không có nút ép `PAID`, auto refund hoặc khôi phục đơn.

### M06 — Quản lý tài khoản

- Search họ tên/email/phone; filter role/status/provider.
- Bảng user, contact đã mask phù hợp, role, verification, status, createdAt.
- Detail/edit trong drawer hoặc modal: profile, linked methods, trạng thái và số đơn.
- Actions khóa/mở/xóa, reason + confirm.
- Không hiển thị password hash, OTP, token hoặc impersonation.
- User có order không được hard-delete; Admin cuối không được khóa/xóa; không có thao tác đổi role trong scope UI.

### M07 — Yêu cầu bảo hành Admin

- Search request code/order/customer; filter status; hiển thị còn/hết hạn hiện tại riêng với tính hợp lệ lúc gửi.
- Bảng: request code, customer, product snapshot, order code, unit index, submittedAt, eligibility và status.
- Detail drawer/panel: mô tả, ảnh, thời hạn snapshot, public timeline và internal note.
- Action theo server: tiếp nhận, bắt đầu xử lý, hoàn tất hoặc từ chối.
- `REJECTED` bắt buộc public reason; `COMPLETED` bắt buộc public resolution.
- Không có action tạo vận đơn, thay máy, ghi chi phí, hoàn tiền hoặc sửa tồn.
- Optimistic version conflict yêu cầu refresh; transition sai bị backend từ chối.

## 9. Dialog và trang hỗ trợ

Các thành phần sau không tính thêm vào 24:

- confirm remove cart item;
- filter drawer;
- điều chỉnh tồn;
- confirm delete/ngừng bán product/category;
- revise quantity/hủy/chuyển trạng thái order;
- user detail/lock/delete;
- hủy yêu cầu bảo hành và cập nhật trạng thái bảo hành;
- trang lỗi 400/403/404/500 dùng template chung;
- Google account conflict/link confirmation;
- email verification result state trong auth shell.

Dialog phải có focus trap, Escape, trả focus về trigger và khóa confirm khi pending.

## 10. Dữ liệu mẫu nhất quán

| SKU | Tên | Cấu hình mẫu | Giá | Tồn |
|---|---|---|---|---|
| PC-OFF-01 | PC Học tập Essential | Core i3 / UHD / 16 GB / SSD 512 GB | 9.990.000 ₫ | Còn |
| PC-DEV-01 | PC Lập trình Studio | Core i5 / UHD / 32 GB / SSD 1 TB | 18.490.000 ₫ | Còn |
| PC-GAM-01 | PC Gaming Balance | Ryzen 5 / RTX 4060 / 16 GB / SSD 1 TB | 22.990.000 ₫ | Còn |
| PC-CRE-01 | PC Sáng tạo Pro | Core i7 / RTX 4070 / 32 GB / SSD 1 TB | 32.990.000 ₫ | Hết |

Đây là fixture demo, không phải giá thị trường hoặc benchmark. Profile lập trình, ngân sách 20.000.000 ₫ gợi ý PC-DEV-01 và còn 1.510.000 ₫.

Customer demo: “Nguyễn Minh An”, email `an.demo@example.com`, phone mask `•••• •• 6789`. Không ghi credential/token.

- Order `PC260926-001`: COD, 2 × PC-DEV-01, phí 50.000 ₫, tổng 37.030.000 ₫.
- Order `PC260926-002`: VNPAY sandbox, 1 × PC-GAM-01, phí 50.000 ₫, tổng 23.040.000 ₫.
- Dùng record/variant riêng cho pending, paid, expired và review-required; không trộn state mâu thuẫn trong cùng fixture.
- Order `PC260926-001` có variant `DELIVERED` ngày 30/09/2026; PC-DEV-01 snapshot bảo hành 24 tháng, hết hạn do server trả 30/09/2028.
- Warranty `WR261001-001`: order item PC-DEV-01, unit index 1, lỗi “Máy khởi động lại khi làm việc”, có variant `REQUESTED`, `PROCESSING` và `COMPLETED` tách riêng.
- Một item fixture đã hết hạn để kiểm tra trạng thái read-only; không dùng ngày hiện tại của trình duyệt tự tính eligibility.

## 11. Luồng tương tác bắt buộc

1. Guest S01 → S04 → S03 → S05 → A01 → C02 → A03 → C02 COD → C05.
2. Google login A01 → xử lý conflict/liên kết → verify phone → C02 VNPAY → cổng ngoài → C03 → C05.
3. Admin M04 → M05 sửa quantity COD → confirm → shipping → delivered; Customer C05 thấy timeline.
4. Admin M01/M02 tạo, sửa, điều chỉnh tồn và bị chặn xóa sản phẩm đã có đơn.
5. Admin M03 bị chặn xóa loại đang có sản phẩm.
6. Admin M06 khóa user; phiên cũ không thực hiện tiếp protected action.
7. Customer C05 → C07 gửi yêu cầu; C06 xem trạng thái; Admin M07 tiếp nhận/xử lý; Customer C07 xem timeline.
8. Hai tab sửa cart hoặc quote cũ → conflict → preview lại, không nhân đôi order.
9. VNPAY callback trùng/sai/muộn → C03/M05 hiển thị kết quả đúng, không tự phục hồi order.

## 12. Hướng dẫn sinh prototype

Prompt nền:

> Thiết kế UI PC Store theo tài liệu này, tiếng Việt, VND, desktop 1440 px và mobile 390 px. Dùng đúng token, component và shell; ưu tiên HTML semantic có thể chuyển thành Bootstrap 5.3 và Thymeleaf fragments. Chỉ sinh screen IDs được yêu cầu, kèm loading/empty/error/conflict/success phù hợp. Dùng fixture mục 10 nhất quán. Không bổ sung tính năng ngoài scope hoặc gửi giá/role/userId từ browser như nguồn tin cậy.

Thứ tự:

| Đợt | Phạm vi | Gate |
|---|---|---|
| 0 | Tokens, components, 5 shells | Desktop/mobile nhất quán, accessible |
| 1 | S01–S05 | Advisory/catalog/cart nối nhau |
| 2 | A01–A05, C01 | Auth/OTP/profile đủ state |
| 3 | C02–C07 | Checkout/payment/order và eligibility bảo hành đúng |
| 4 | M01–M03 | Catalog/category/stock đúng rule |
| 5 | M04–M07 | Order/user/warranty actions đúng allowedActions |
| 6 | Rà responsive và 9 luồng | Không dead-end hoặc CTA ngoài scope |

Naming: `S02 / Catalog / Desktop / Ready`, `C02 / Checkout / Mobile / Conflict`, `C07 / Warranty Detail / Mobile / Processing`, `M07 / Warranty Admin / Desktop / Requested`.

## 13. Checklist nghiệm thu UI

- [ ] Có đúng 24 screen IDs; form/detail Admin đã được tính rõ.
- [ ] Navigation không còn link tới compare, devices/serial, shipments, payments Admin, audit hoặc dashboard; chỉ có bảo hành tối giản.
- [ ] Mỗi màn hình có route, content, action và state chính.
- [ ] Tất cả form có label, error summary, field error và giữ dữ liệu không nhạy cảm sau lỗi.
- [ ] Mobile 360–390 px không overflow toàn trang; table có chiến lược responsive rõ.
- [ ] Tư vấn cùng một trang form/kết quả, trả tối đa ba PC có giải thích.
- [ ] Checkout chỉ hiển thị tiền server; phone verified; quote expiry và idempotency không bị bỏ.
- [ ] Return URL không làm UI tự báo `PAID`.
- [ ] Admin không chọn status tùy ý hoặc ép payment thành công.
- [ ] Quantity edit chỉ xuất hiện cho COD/chưa trả/chờ xác nhận.
- [ ] Product/category/user delete constraints có nội dung dễ hiểu.
- [ ] Eligibility bảo hành do server tính; Customer chỉ thấy order item của mình; transition/lý do đúng state machine.
- [ ] UI không đưa vào serial, vận chuyển bảo hành, linh kiện sửa chữa, đổi máy hoặc hoàn tiền.
- [ ] Fixture cart/order/payment nhất quán về SKU, quantity và total.
- [ ] Focus, contrast, keyboard, zoom và async announcement đạt yêu cầu accessibility.

## Đồng bộ báo cáo lần 1 — v2.1

[12 use case chính](../requirements/FUNCTIONAL_SPECIFICATION.md) bao phủ đúng24 màn hình qua [ma trận truy vết](../requirements/TRACEABILITY_MATRIX.md). Đây là cách nhóm chức năng để báo cáo, không phải tăng/giảm screen IDs.

- C02: quote10 phút, phí50.000 VND; giá và contact đổi phải xác nhận lại.
- C03/C05: một attempt/order; chỉ PENDING còn hạn có action mở lại cùng reference. REVIEW_REQUIRED không có lời hứa hoàn tiền.
- C06/C07: còn hạn khi start≤serverNow<end; cộng tháng lịch theo snapshot. Request cũ hợp lệ vẫn được theo dõi/xử lý sau end; không có serial.
- M05: sửa lượng COD cập nhật cả tổng đơn và COD amount, giữ phí/giá snapshot; lượng0 bỏ dòng nhưng không cho đơn rỗng. Chỉ tăng lượng PC ACTIVE.
- M06: user có order hoặc actor history không xóa; contact Admin đổi phải bỏ verified và không làm mất phương thức đăng nhập cuối.
- M07: thêm ghi chú trong drawer khi RECEIVED/PROCESSING; không đổi trạng thái, public/internal tách trường. Không reopen request đã đóng.
