# ADR-001: Chuyển đổi công nghệ giao diện người dùng từ Thymeleaf sang React SPA

- **Trạng thái:** Được chấp nhận (Accepted)
- **Ngày quyết định:** 2026-10-25
- **Người đề xuất:** Võ Văn Cảnh (Tech Lead & Fullstack Developer)
- **Người phê duyệt:** Nhóm 16 - Đồ án WWW PC Store

---

## 1. Bối cảnh (Context)
Dự án PC Store ban đầu được định hướng sử dụng Spring Boot + Server-Side Rendering với Thymeleaf (đã hoàn thành báo cáo Lần 1). Tuy nhiên, yêu cầu trải nghiệm người dùng hiện đại cho trang web thương mại điện tử chuyên biệt máy tính cần:
- Tương tác mượt mà không tải lại trang khi lọc danh mục, thêm giỏ hàng, cấu hình linh kiện và bảo hành.
- Phân tách rõ ràng giữa Backend REST API và Frontend Single Page Application (SPA).
- Tận dụng hệ sinh thái phong phú của React (React 19, Vite, React Router v7).

## 2. Quyết định (Decision)
1. Thay thế hoàn toàn Thymeleaf bằng **React 19 SPA** được xây dựng với **Vite**.
2. Toàn bộ mã nguồn giao diện đặt trong thư mục `frontend/`.
3. Backend Spring Boot 4.1.2 chuyển đổi thành **REST API** cung cấp JSON chuẩn RFC 7807 (ProblemDetail).
4. Sử dụng Cookie CSRF (`CookieCsrfTokenRepository.withHttpOnlyFalse()`) và CORS cấu hình linh hoạt qua biến môi trường.
5. Thiết kế giao diện tuân thủ Design System tokens (`--pc-*`), không dùng UI template cồng kềnh, giữ phong cách clean, hiện đại, không text thừa.

## 3. Hệ quả (Consequences)
- **Tích cực:** Trải nghiệm người dùng vượt trội, phân chia công việc backend/frontend rõ ràng cho 5 thành viên trong nhóm, dễ kiểm thử và mở rộng.
- **Tiêu cực / Rủi ro:** Cần quản lý xác thực phiên / CSRF giữa 2 nguồn gốc trong môi trường phát triển (đã giải quyết qua CORS & Vite proxy/credentials).
