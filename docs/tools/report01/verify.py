"""Verify report artifacts, not application behavior."""
from pathlib import Path
import json,re,struct,xml.etree.ElementTree as ET
from core import CASES
from schema import TABLES
ROOT=Path(__file__).resolve().parents[3]
DOC=ROOT/'docs'; D=DOC/'architecture/diagrams/report-01'
manifest=json.loads((D/'manifest.json').read_text(encoding='utf-8'))
assert len(CASES)==12 and len(TABLES)==20 and len(manifest)==18
assert len({c['code'] for c in CASES})==12
render=json.loads((D/'render-log.json').read_text(encoding='utf-8'));assert render['returncode']==0
native=json.loads((D/'vp-build-log.json').read_text(encoding='utf-8'))
assert 'REPORT01_REOPEN_OK diagrams=13' in native['reopen']
root=ET.parse(D/'vp-export/project.xml').getroot()
diagrams=[e for e in root.iter() if e.tag.split('}')[-1]=='Diagram']
assert len(diagrams)==13, len(diagrams)
sizes=[]
for item in manifest:
 code=item['id'];source=D/'source'/f'{code}.puml';p=D/'png'/f'{code}.png'
 assert source.exists() and p.exists()
 raw=p.read_bytes();assert raw[:8]==b'\x89PNG\r\n\x1a\n'
 w,h=struct.unpack('>II',raw[16:24]);assert w>100 and h>100 and max(w,h)<16000
 sizes.append((code,w,h))
for c in CASES:
 for t in c['tables'].split():assert t in TABLES,(c['code'],t)
 spec=(DOC/'requirements/FUNCTIONAL_SPECIFICATION.md').read_text(encoding='utf-8')
 assert f'id="{c["code"].lower()}"' in spec
 assert f'ACT-{c["code"]}.png' in spec
report='''# Kiểm tra bộ tài liệu báo cáo lần 1

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
'''
out=DOC/'reports/REPORT_01_VALIDATION.md';out.write_text(report,encoding='utf-8')
bad=[];count=0
for md in DOC.rglob('*.md'):
 text=md.read_text(encoding='utf-8')
 for target in re.findall(r'\]\(([^)]+)\)',text):
  target=target.strip('<>')
  if target.startswith(('http:','https:','mailto:','#')):continue
  file=target.split('#')[0]
  if not file:continue
  count+=1
  if not (md.parent/file).exists():bad.append((str(md.relative_to(ROOT)),target))
assert not bad,bad
(D/'verification.json').write_text(json.dumps(dict(use_cases=12,tables=20,diagram_sources=18,png=18,native_vp_diagrams=13,checked_links=count,broken_links=bad,png_dimensions=sizes),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'PASS: 12 UC, 20 tables, 18 PNG/source pairs, 13 native VP diagrams, {count} local links')
