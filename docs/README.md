# Mục lục tài liệu

| Khu vực | Nội dung |
|---|---|
| `PLAN.md` | Đặc tả và kế hoạch triển khai phiên bản 2.1 đã thu gọn |
| `STRUCTURE.md` | Cấu trúc 7 module và quy tắc đặt mã nguồn |
| `requirements/` | Actor, use case, yêu cầu, acceptance criteria, traceability |
| `architecture/` | Kiến trúc, sơ đồ module, ADR và quyết định kỹ thuật |
| `data/` | ERD, data dictionary, state model và migration strategy |
| `api/` | MVC route, REST contract và provider contract |
| `ux/` | UI flow, wireframe và trạng thái màn hình |
| `testing/` | Test plan, test case, dữ liệu demo và bằng chứng kiểm thử |
| `operations/` | Hướng dẫn chạy, triển khai, backup/restore và monitoring |
| `project/` | Backlog, phân công, rủi ro, biên bản review và kịch bản bảo vệ |

Đặc tả giao diện:

- [Đặc tả UI 24 màn hình](ux/UI_SCREEN_SPEC.md) — design system, shell, component contract, route, state và luồng demo theo phạm vi 2.0.

Quy ước làm việc:

- [Quy ước commit](project/COMMIT_CONVENTION.md)

Các tài liệu dưới đây là đặc tả trước triển khai, không phải bằng chứng ứng dụng đã được code.

## Đồng bộ báo cáo lần 1 — v2.1

Đọc theo thứ tự:

1. [Báo cáo lần 1 — 12 chức năng chính và các sơ đồ](reports/REPORT_01_ANALYSIS_AND_DESIGN.md).
2. [Bảng đặc tả đầy đủ 12 chức năng](requirements/FUNCTIONAL_SPECIFICATION.md).
3. [Danh mục sơ đồ và mã nguồn dựng lại](architecture/diagrams/report-01/README.md).
4. [Thiết kế dữ liệu20 bảng](data/DATABASE_DESIGN.md).
5. [Ma trận truy vết24 màn hình](requirements/TRACEABILITY_MATRIX.md).
6. [Kết quả kiểm tra tài liệu và sơ đồ](reports/REPORT_01_VALIDATION.md).

[Quy tắc chi tiết](requirements/BUSINESS_RULES.md) là phụ lục tra cứu; khi báo cáo tập trung bảng đặc tả, use case tổng quát và activity của12 chức năng chính.
