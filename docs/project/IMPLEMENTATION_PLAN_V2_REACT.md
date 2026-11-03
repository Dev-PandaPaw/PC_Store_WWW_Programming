# Kế hoạch Hiện thực Chi tiết V2 - Mô hình Spring Boot REST API + React SPA
**Dự án:** PC Store - Website bán máy tính PC trực tuyến (Nhóm 16)  
**Tác giả:** Võ Văn Cảnh  
**Ngày cập nhật:** 2026-10-25  

---

## 1. Phân công 5 Thành viên Nhóm

| Thành viên | Vai trò & Bounded Context phụ trách | Phạm vi Frontend (React) | Phạm vi Backend (Spring Boot) |
|---|---|---|---|
| **Võ Văn Cảnh** | Tech Lead, Architecture, BC-01 Identity & BC-03 Advisory | Auth Shell, Login, Register, Advisory Page | User, Security, JWT/Session, Advisory Engine |
| **Thành viên 2 (Bửu)** | BC-02 Catalog & Inventory Management | Storefront Shell, Catalog, Product Detail | Category, Product, Spec, Inventory Adjustment |
| **Thành viên 3 (Ngân)** | BC-04 Sales & Cart Management | Cart Page, Checkout Page, Order Success | Cart Context, Order, OrderItem, Checkout Service |
| **Thành viên 4 (Nhân)** | BC-05 Payment & Order Fulfillment | Order History, Order Detail, Payment Callback | PaymentAttempt, VNPay/Momo/COD, Idempotency |
| **Thành viên 5 (Nhựt)** | BC-06 Aftersales & Warranty | Warranty Submit, Warranty Status, Admin Shell | WarrantyRequest, Event Tracking, Admin Modules |

---

## 2. Lộ trình Triển khai (6 Pha)

- **Pha 0: Foundation Bootstrap (Hiện tại)**: Đã hoàn tất kết nối Supabase, Flyway 20 bảng, Spring Boot 4 REST scaffold, React SPA scaffold với 24 màn hình.
- **Pha 1: Identity & Authentication Slice**: Hiện thực login, register, profile, session & CSRF cookie.
- **Pha 2: Catalog & Advisory Slice**: Hiện thực API sản phẩm, phân trang, bộ lọc linh kiện PC, thuật toán gợi ý cấu hình.
- **Pha 3: Cart & Checkout Slice**: Hiện thực giỏ hàng, đặt hàng, xử lý tranh chấp tồn kho và địa chỉ giao hàng.
- **Pha 4: Payment & Fulfillment Slice**: Tích hợp cổng thanh toán trực tuyến, trạng thái đơn hàng.
- **Pha 5: Aftersales & Admin Dashboard**: Quản lý đơn hàng, danh mục, bảo hành và phân quyền Admin.
