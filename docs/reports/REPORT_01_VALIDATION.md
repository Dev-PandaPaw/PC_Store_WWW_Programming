# Kiểm tra bộ tài liệu báo cáo lần 1

Ngày 26/09/2026. Đây là kiểm tra **artifact phân tích/thiết kế**, không phải kết quả test ứng dụng.

## Kết quả đã kiểm tra

| Hạng mục | Kết quả |
|---|---|
| Scope báo cáo | 12 use case chính; 7 module; 24 màn hình; không thêm chức năng kinh doanh |
| Đặc tả | 12 bảng đầy đủ, bước chính/ngoại lệ/luồng con, tiêu chí nghiệm thu và activity |
| Sơ đồ nguồn và ảnh | 18 nguồn PlantUML và 18 PNG; renderer kết thúc exit code 0 |
| Dữ liệu | 20 bảng; mọi tên bảng trong ma trận chức năng tồn tại trong dictionary |
| Visual Paradigm | Project native mở lại thành công qua CLI/Open API: 13 diagram (1 use case + 12 activity) |
| XML Visual Paradigm | XML do ExportXML chính thức xuất; parse được và có13 diagram; giữ kèm data.zip |
| Kiểm hình | Đã xem trực quan use case tổng, activity checkout và ERD bảo hành; tất cả PNG có header/kích thước hợp lệ |
| Liên kết | Script kiểm tất cả liên kết tương đối trong Markdown thuộc docs; không có đường dẫn mất |
| Mã ứng dụng | Không thêm Spring/Java backend, migration hoặc UI runtime |

## File bàn giao và giới hạn công cụ

- [Project Visual Paradigm](../architecture/diagrams/report-01/PC_STORE_REPORT_01.vpp): các actor/use case/action/decision/control flow là phần tử native chỉnh sửa được, không phải ảnh dán lên canvas. Bố cục native dạng cột khác ảnh PlantUML; PNG là bản dùng để nhúng báo cáo.
- [XML Visual Paradigm](../architecture/diagrams/report-01/vp-export/project.xml), kèm [data.zip](../architecture/diagrams/report-01/vp-export/data.zip): xuất từ project bằng lệnh chính thức; không đổi đuôi mã nguồn khác thành XML/VPP.
- 5 ERD **chưa nằm trong project VPP**: đã có nguồn PlantUML chỉnh sửa được, dựng thành PNG và dictionary đầy đủ. API VP CE18 báo `Invalid connection. DBTable is not allowed with DBForeignKey` khi tạo FK, dù đã thử đúng loại diagram/data model. Không đưa mô hình FK lỗi vào project bàn giao.
- Open API export XML trong chế độ headless từng báo HeadlessException; đã giải quyết bằng CLI `com.vp.cmd.ExportXML`. Lỗi ghi Program Files được giải quyết bằng thư mục plugin người dùng AppData.
- Chưa kiểm toàn bộ bố cục native qua UI; đã mở project lại bằng engine VP và kiểm XML mô hình. Thử export native ACT-UC05 cho thấy chế độ PNG nền trong/nền trắng hiển thị nhãn không nhất quán trong CLI. Vì vậy ảnh nhúng báo cáo dùng bản PlantUML đã kiểm, không dùng các ảnh export native thử nghiệm. Project vẫn lưu các phần tử và nội dung để chỉnh sửa trong Visual Paradigm.

## Dựng và kiểm lại

```powershell
python docs/tools/report01/build.py
python docs/tools/report01/render.py
python docs/tools/report01/run_vp.py
python docs/tools/report01/verify.py
git diff --check
```

`build.py` dùng 12 use case trong core.py và 20 bảng trong schema.py. `run_vp.py` cần VP CE18 và JDK, dùng bản sao temp của project đã bàn giao làm seed rồi tạo project trống mới; không sửa các project khác. Bộ dựng được cài tại AppData/VisualParadigm/plugins/pcstore.report01, không thay file cài đặt Visual Paradigm.

Nguồn renderer cố định PlantUML1.2025.4; SHA256 và exit code trong [render log](../architecture/diagrams/report-01/render-log.json). Bằng chứng tạo/mở lại/export VP trong [VP build log](../architecture/diagrams/report-01/vp-build-log.json).

## Nghiệm thu ứng dụng về sau

Các AC trong đặc tả, test race tồn/IPN, bảo mật owner/CSRF và hiệu năng là **kế hoạch nghiệm thu**, chưa chạy do chưa triển khai app. Không suy từ việc dựng sơ đồ thành công rằng backend đã đúng.
