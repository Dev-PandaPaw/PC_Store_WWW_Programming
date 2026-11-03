"""Build the focused report: 12 use cases, 13 UML diagrams and 5 ERDs.
Run from any directory: python docs/tools/report01/build.py
Rendering: python docs/tools/report01/render.py
"""
from pathlib import Path
import re, json, textwrap, xml.etree.ElementTree as ET
from core import CASES
from schema import TABLES
ROOT=Path(__file__).resolve().parents[3]
DOC=ROOT/'docs'
DIAG=DOC/'architecture/diagrams/report-01'
for p in [DIAG/'source',DIAG/'png',DOC/'reports',DOC/'data',DOC/'requirements']:
    p.mkdir(parents=True,exist_ok=True)
def write(path,text): path.write_text(text.strip()+'\n',encoding='utf-8')
def cell(t): return str(t).replace('|','\\|').replace('\n','<br>')
def wrap(t,n=53): return '\\n'.join(textwrap.wrap(t,n,break_long_words=False))
def pu(code,title,body):
    write(DIAG/'source'/f'{code}.puml','@startuml\n!pragma layout smetana\nskinparam defaultFontName Arial\nskinparam defaultFontSize 14\nskinparam shadowing false\nskinparam backgroundColor #FFFFFF\nskinparam ArrowColor #334155\nskinparam activityBackgroundColor #EFF6FF\nskinparam activityBorderColor #2563EB\nskinparam activityDiamondBackgroundColor #FFF7ED\nskinparam activityDiamondBorderColor #C2410C\nskinparam usecaseBackgroundColor #EFF6FF\nskinparam usecaseBorderColor #2563EB\nskinparam padding 8\ntitle '+title+'\n'+body+'\n@enduml')
def img(code,title,prefix='../architecture/diagrams/report-01'):
    return f'![{code} — {title}]({prefix}/png/{code}.png)\n\n[Nguồn sơ đồ {code}]({prefix}/source/{code}.puml)'

# Neutral, documented graph used by VP Open API; not a renamed .vpp or a claimed VP XML.
model=ET.Element('ReportModel',version='1',name='PC Store — Báo cáo lần 1')
def nd(d,key,kind,name,x,y,w=360,h=65):
    return ET.SubElement(d,'Node',id=key,kind=kind,name=name,x=str(x),y=str(y),w=str(w),h=str(h))
def edge(d,a,b,label=''):
    ET.SubElement(d,'Edge',source=a,target=b,label=label)

overview=['left to right direction', 'actor "Người truy cập" as Visitor <<abstract>>','actor Guest','actor Customer','actor Admin','actor Google','actor VNPAY','actor "OTP Provider" as OTP','actor "Mail Service" as Mail',
'Visitor <|-- Guest','Visitor <|-- Customer','Customer <|-- Admin','rectangle "PC Store" {']
d=ET.SubElement(model,'Diagram',id='UC-OVERVIEW',kind='UseCase',name='UC-OVERVIEW — PC Store')
for i,(key,label) in enumerate([('Visitor','Người truy cập'),('Guest','Guest'),('Customer','Customer'),('Admin','Admin')]): nd(d,key,'Actor',label,30,80+i*170,100,90)
for i,(key,label) in enumerate([('Google','Google'),('VNPAY','VNPAY'),('OTP','OTP Provider'),('Mail','Mail Service')]): nd(d,key,'Actor',label,1040,80+i*170,110,90)
nd(d,'Boundary','System','PC Store',200,30,800,1040)
for i,c in enumerate(CASES):
    overview.append(f'usecase "{c["code"]}\\n{c["name"]}" as {c["code"]}')
    nd(d,c['code'],'UseCase',c['code']+'\n'+c['name'],240+(i//6)*380,70+(i%6)*160,310,85)
overview.append('}')
links={'Guest':['UC01'],'Visitor':['UC02','UC03','UC04'],'Customer':['UC01','UC05','UC06','UC10','UC11'],'Admin':['UC07','UC08','UC09','UC12'],'Google':['UC01'],'VNPAY':['UC05'],'OTP':['UC01'],'Mail':['UC01','UC05','UC11','UC12']}
for a,ucs in links.items():
    for code in ucs:
        overview.append(f'{code} -- {a}' if a in ('Google','VNPAY','OTP','Mail') else f'{a} -- {code}');edge(d,a,code)
for child,parent in [('Guest','Visitor'),('Customer','Visitor'),('Admin','Customer')]:
    e=ET.SubElement(d,'Edge',source=child,target=parent,label='',kind='Generalization')
pu('UC-OVERVIEW','Use case tổng quát — 12 chức năng chính','\n'.join(overview))

spec=['# PC Store — Đặc tả 12 chức năng chính','',
'**Phiên bản 2.1 — 26/09/2026.** Phạm vi giữ nguyên 7 module, 24 màn hình. Tài liệu nhóm thao tác nhỏ thành 12 use case phục vụ báo cáo; số use case không phải số màn hình hay số API.',
'', 'Các thao tác thêm/sửa/xóa, OTP, callback và kiểm tra dữ liệu là **luồng con**, không phải tính năng độc lập cần tách thành hàng chục use case. Nguồn: phiếu đề tài SRC01, yêu cầu môn học SRC02 và PLAN/UI đã thu gọn SRC03; xem [quy tắc nghiệp vụ](BUSINESS_RULES.md).',
'','## Danh mục','', '| Mã | Chức năng chính | Actor | Module | Màn hình |','|---|---|---|---|---|']
for c in CASES: spec.append(f'| [{c["code"]}](#{c["code"].lower()}) | {c["name"]} | {c["actor"]} | {c["module"]} | {c["screens"]} |')
spec+=['','## Quy ước đọc và xử lý lỗi chung','',
'Mỗi bước Bn dùng cùng số trong activity. Nhánh E-Bn là nhánh lỗi/thay thế tại bước đó; khi kết thúc nhánh lỗi, người dùng có thể sửa và gửi lại thao tác từ đầu. Quyền và dữ liệu riêng được kiểm phía server; không có side effect trước khi kiểm điều kiện. Mutation lỗi trước commit rollback toàn bộ. Lỗi media/email sau commit không biến kết quả đã lưu thành thất bại giả.',
'','REST: 401 thiếu phiên, 403 thiếu quyền/CSRF, 404 tài nguyên không có hoặc không thuộc chủ, 422 sai form, 409 sai trạng thái/version/trùng/thiếu tồn, 503 dịch vụ ngoài không khả dụng. MVC hiện cùng lỗi theo field hoặc flash message. GET chỉ đọc; thao tác ghi dùng POST/PATCH/DELETE và CSRF, ngoại trừ IPN có chữ ký.',
'','Đối với nhóm chức năng, hậu điều kiện áp dụng cho nhánh được chọn; không bắt người dùng thực hiện mọi thao tác trong nhóm ở một lần tương tác. Đăng nhập là tiền điều kiện của checkout/quản trị, không vẽ `include Đăng nhập` cho mọi use case. Không ép dùng include/extend khi không có nhu cầu tái sử dụng riêng.', '']

diagram_rows=[('UC-OVERVIEW','Use case tổng quát','Ranh giới hệ thống, actors và 12 mục tiêu người dùng')]
for c in CASES:
    code=c['code'];act='ACT-'+code
    body=['|'+c['actor'].split(',')[0]+'|','start']
    d=ET.SubElement(model,'Diagram',id=act,kind='Activity',name=act+' — '+c['name'])
    nd(d,'start','Initial','',450,15,20,20);prev='start';y=65
    for i,(who,action,guard,failure) in enumerate(c['steps'],1):
        lane=c['actor'].split(',')[0] if who=='Người dùng' else 'PC Store'
        body+=['|'+lane+'|',f':B{i}. {wrap(action)};']
        key=f'b{i}';nd(d,key,'Action',f'B{i}. [{lane}]\n{action}',250,y,430,85);edge(d,prev,key);prev=key;y+=130
        if guard:
            body += ['|PC Store|',f'if ({wrap(guard,40)}) then (Có)', 'else (Không)',f':E-B{i}. {wrap(failure)};','stop','endif']
            g=f'g{i}';nd(d,g,'Decision',guard,390,y,150,95);edge(d,prev,g)
            err=f'e{i}';nd(d,err,'Action',f'E-B{i}. {failure}',720,y,370,110);edge(d,g,err,'Không')
            fin=f'f{i}';nd(d,fin,'Final','',900,y+145,22,22);edge(d,err,fin)
            ok=f'ok{i}';nd(d,ok,'Merge','',450,y+140,20,20);edge(d,g,ok,'Có');prev=ok;y+=210
        # Optional user actions have explicit early completion.
        if (code=='UC06' and i==3) or (code=='UC10' and i==4) or (code=='UC05' and i==7):
            q='Muốn hủy đơn?' if code=='UC06' else ('Mở yêu cầu đã gửi?' if code=='UC10' else 'Phương thức VNPAY?')
            body+=['|'+c['actor'].split(',')[0]+'|',f'if ({q}) then (Có)','else (Không)','stop','endif']
            g=f'optional{i}';nd(d,g,'Decision',q,390,y,150,85);edge(d,prev,g)
            fin=f'skip{i}';nd(d,fin,'Final','',870,y+30,22,22);edge(d,g,fin,'Không')
            ok=f'continue{i}';nd(d,ok,'Merge','',450,y+130,20,20);edge(d,g,ok,'Có');prev=ok;y+=180
    body+=['stop']
    nd(d,'end','Final','',450,y,22,22);edge(d,prev,'end')
    pu(act,act+' — '+c['name'],'\n'.join(body))
    diagram_rows.append((act,c['name'],'Các bước Bn và nhánh E-Bn trong bảng đặc tả '+code))
    spec += [f'<a id="{code.lower()}"></a>',f'## {code} — {c["name"]}','','| Thuộc tính | Đặc tả |','|---|---|',
    f'| Yêu cầu | FR-{code[2:]} — {c["goal"]} |',f'| Tác nhân / module | {c["actor"]} / {c["module"]} |',
    f'| Nguồn | SRC01/SRC03; UC03 và UC10–UC12 là điểm nhấn/bổ sung đã chốt theo SRC03 |',
    f'| Kích hoạt | {cell(c["steps"][0][1])} |',f'| Tiền điều kiện | {cell(c["pre"])} |',f'| Đầu vào / kiểm tra | {cell(c["inputs"])} |',
    f'| Hậu điều kiện / đầu ra | {cell(c["post"])} |',f'| Màn hình | {c["screens"]}; route theo [UI spec](../ux/UI_SCREEN_SPEC.md) |',
    f'| Dữ liệu liên quan | {", ".join("`"+t+"`" for t in c["tables"].split())} |',
    f'| Quy tắc | {c["rules"]}; [giải thích đầy đủ](BUSINESS_RULES.md) |','',
    '### Luồng chính','','| Bước | Tác nhân | Hành động và phản hồi |','|---|---|---|']
    for i,(who,action,guard,failure) in enumerate(c['steps'],1): spec.append(f'| B{i} | {who} | {cell(action)} |')
    spec += ['','### Luồng thay thế và ngoại lệ','','| Mã / vị trí | Điều kiện | Xử lý và điểm kết thúc |','|---|---|---|']
    for i,(_,_,guard,failure) in enumerate(c['steps'],1):
        if guard: spec.append(f'| E-B{i} | Không đạt: {cell(guard)} | {cell(failure)}; kết thúc lần thao tác này. |')
    for j,(step,condition,detail) in enumerate(c['alternates'],1): spec.append(f'| A{j} tại B{step} | {condition} | {cell(detail)} |')
    spec += ['','### Tiêu chí nghiệm thu','','| Mã | Tình huống | Kết quả mong đợi |','|---|---|---|',f'| AC-{code}-01 | Thỏa tiền điều kiện và chạy luồng chính hợp lệ | {cell(c["post"])} |']
    for i,(_,_,guard,failure) in enumerate(c['steps'],1):
        if guard: spec.append(f'| AC-{code}-E{i} | Làm sai điều kiện tại B{i}: {cell(guard)} | {cell(failure)}; không ghi dữ liệu thành công một phần. |')
    spec += [f'| AC-{code}-AUTH | Gọi dữ liệu/thao tác riêng bằng tài khoản khác hoặc không đủ quyền (nếu có) | Không được đọc/ghi; 401/403/404 theo quy ước chung. |','',f'### Activity {code}','',img(act,c['name']),'']
write(DOC/'requirements/FUNCTIONAL_SPECIFICATION.md','\n'.join(spec))

# ERD definitions share the exact dictionary source to prevent table drift.
groups={'ERD-ALL':list(TABLES),'ERD-IDENTITY':[n for n,t in TABLES.items() if t['module']=='identity'],
'ERD-CATALOG':[n for n,t in TABLES.items() if t['module']=='catalog'],
'ERD-SALES-PAYMENT':[n for n,t in TABLES.items() if t['module'] in ('sales','payment')],
'ERD-AFTERSALES':[n for n,t in TABLES.items() if t['module']=='aftersales']}
relations=[]
for name,t in TABLES.items():
    for col,typ,flags,meaning in t['columns']:
        m=re.search(r'FK (\w+)\.(\w+)',flags)
        if m: relations.append((m[1],name,col,'NULL' in flags and 'NN' not in flags))
for code,names in groups.items():
    refs={a for a,b,_,_ in relations if b in names and a not in names}
    vd=ET.SubElement(model,'Diagram',id=code,kind='ERD',name=code+' — PC Store')
    for j,name in enumerate(names+sorted(refs)):
        tn=nd(vd,name,'Table',name,40+(j%4)*460,40+(j//4)*650,390,70+len(TABLES[name]['columns'])*22)
        for col,typ,flags,meaning in TABLES[name]['columns']:
            ET.SubElement(tn,'Column',name=col,type=typ,pk=str('PK' in flags).lower(),nullable=str('NN' not in flags).lower(),description=meaning)
    for parent,child,col,nullable in relations:
        if child in names:
            ET.SubElement(vd,'Edge',source=parent,target=child,label=col,kind='ForeignKey',nullable=str(nullable).lower(),single=str(child in ('product_specs','payment_attempts') or (child=='idempotency_records' and col=='order_id')).lower())
    body=['hide circle','skinparam linetype ortho','skinparam entityBackgroundColor #F8FAFC','skinparam entityBorderColor #475569']
    for name in names:
        t=TABLES[name];body.append(f'entity "{name}" as {name} {{')
        for col,typ,flags,meaning in t['columns']:
            if code=='ERD-ALL' and not ('PK' in flags or 'FK' in flags or col in ('status','reservation_status','quantity','stock_on_hand','reserved_quantity')): continue
            mark='* ' if 'NN' in flags else '  '
            tags=' <<PK>>' if 'PK' in flags else (' <<FK>>' if 'FK' in flags else '')
            body.append(f'{mark}{col}: {typ}{tags}')
        body.append('}')
    for ref in sorted(refs): body.append(f'entity "{ref} (tham chiếu)" as {ref} {{\n* id: uuid <<PK>>\n}}')
    for parent,child,col,nullable in relations:
        if child not in names: continue
        left='|o' if nullable else '||'
        right='o|' if child in ('product_specs','payment_attempts') or (child=='idempotency_records' and col=='order_id') else 'o{'
        body.append(f'{parent} {left}--{right} {child} : {col}')
    pu(code,code+' — PC Store','\n'.join(body))
    diagram_rows.append((code,'Mô hình dữ liệu '+code.replace('ERD-',''),'PK/FK, tùy chọn và bội số; bảng đầy đủ trong từ điển dữ liệu'))

db=['# PC Store — Thiết kế cơ sở dữ liệu','',
'Phiên bản 2.1 — thiết kế trước hiện thực. **20 bảng**, không thêm bảng giỏ, reservation, shipment, serial hay refund. PostgreSQL; UUID do ứng dụng sinh; thời gian UTC với timestamptz; giá VND numeric(19,0). NN = NOT NULL, NULL = được bỏ trống. Giá trị mặc định trong bảng là contract ứng dụng/DDL dự kiến, chưa có migration.',
'','## ERD tổng quát','',img('ERD-ALL','20 bảng — chỉ hiển thị khóa và trường trạng thái để dễ đọc'),
'','Đường nối dùng crow’s foot: `||` đúng một, `o|` không hoặc một, `o{` không hoặc nhiều. Parent order có ít nhất một item và đúng một attempt sau commit, được bảo đảm bởi transaction ứng dụng; FK một chiều không tự chứng minh điều đó. Cột FK nullable có parent tùy chọn. `user_roles` dùng khóa ghép; `product_specs` dùng shared PK.',
'','## Nguyên tắc quan hệ và lưu trữ','',
'- FK mặc định RESTRICT/NO ACTION, không cascade xóa lịch sử. Xóa dữ liệu phụ của user/PC chưa phát sinh lịch sử bằng service theo thứ tự phụ→cha trong transaction.',
'- Application Java kiểm enum/range/state/owner; không dùng CHECK, trigger hoặc stored procedure để thay business validation theo yêu cầu đề tài.',
'- PK/FK/UNIQUE/NOT NULL và index bảo vệ quan hệ; partial unique cho một yêu cầu bảo hành mở và một ảnh cover.',
'- Không đặt index mọi cột. Index FK và tổ hợp truy vấn nêu tại từng bảng; đo lại khi có dữ liệu thực.',
'- orders + order_items + reservation_status xác định hàng đang giữ; products.reserved_quantity là số dư cập nhật cùng giao dịch. Cart/quote ở session.',
'- Snapshot sản phẩm/giá/địa chỉ/tháng bảo hành giữ lịch sử khi catalog/profile đổi. JSONB chỉ dùng snapshot có cấu trúc hoặc sự kiện đã lọc secret.',
'','## ERD theo nhóm','']
for code in list(groups)[1:]: db += ['### '+code,'',img(code,code),'']
db += ['## Từ điển dữ liệu đầy đủ','']
for i,(name,t) in enumerate(TABLES.items(),1):
    db += [f'### {i}. `{name}`',f'\nModule **{t["module"]}** — {t["purpose"]}\n', '| Cột | Kiểu | Khóa / null / mặc định | Ý nghĩa |','|---|---|---|---|']
    for row in t['columns']: db.append('| '+' | '.join(cell(v) for v in row)+' |')
    db += ['', '**Ràng buộc và index:** '+t['constraints'],'']
db+=['## Ví dụ dữ liệu và kiểm tra thiết kế','',
'PC-DEV-01 giá 18.490.000, onHand=5, reserved=0. Đặt COD quantity2: subtotal36.980.000, phí50.000, tổng37.030.000; reserved=2. Sửa quantity1: tổng18.540.000 và reserved=1, COD attempt.amount cũng18.540.000. SHIPPING: onHand=4, reserved=0, marker CONSUMED. DELIVERED: COD PAID và bắt đầu thời hạn bảo hành; không giảm tồn thêm.',
'', 'Item đã giao 31/01/2026 10:00 giờ Việt Nam, warrantyMonthsSnapshot=1: hết hạn28/02/2026 10:00. Yêu cầu gửi09:59 hợp lệ và xử lý được sau10:00; yêu cầu gửi đúng10:00 bị từ chối. Hai request đồng thời cùng item/unit chỉ một request mở được commit. Các ví dụ là fixture đồ án, không dữ liệu giao dịch thật.']
write(DOC/'data/DATABASE_DESIGN.md','\n'.join(db))

trace=['# Ma trận truy vết — 12 chức năng chính','',
'CRUD, OTP và callback là luồng con. Các yêu cầu FR dưới đây tương ứng 12 mục tiêu, không tạo thêm scope. Tiêu chí AC là kế hoạch nghiệm thu trước code, chưa phải test ứng dụng đã chạy.','',
'| Yêu cầu / UC | Chức năng | Màn hình | Activity | Dữ liệu | Quy tắc |','|---|---|---|---|---|---|']
for c in CASES:
    trace.append(f'| FR-{c["code"][2:]} / [{c["code"]}](FUNCTIONAL_SPECIFICATION.md#{c["code"].lower()}) | {c["name"]} | {c["screens"]} | [ACT-{c["code"]}](../architecture/diagrams/report-01/png/ACT-{c["code"]}.png) | {", ".join(c["tables"].split())} | {c["rules"]} |')
trace+=['','## Kiểm tra bao phủ màn hình','',
'S01–S03→UC02; S04→UC03; S05→UC04; A01–A05/C01→UC01; C02–C03→UC05; C04–C05→UC06; C06→UC10; C07→UC10/UC11; M01–M03→UC07; M04–M05→UC08; M06→UC09; M07→UC12. Tổng đúng **24 screen IDs**.',
'','## Đối chiếu module','',
'identity→UC01/UC09; catalog→UC02/UC07; cart→UC04; advisory→UC03; sales→UC05/UC06/UC08; payment→UC05/UC08; aftersales→UC10/UC11/UC12. Có **7 module**, một use case có thể đi qua nhiều module, không cần tách thêm module để vẽ.',
'','## Nghiệm thu nghiệp vụ trọng tâm','',
'1. Khách tư vấn→xem PC→giỏ→verified phone→COD→Admin giao→xem bảo hành.',
'2. VNPAY đúng hạn, callback sai, callback trùng, hết hạn và success đến muộn; không oversell/đơn trùng.',
'3. Customer không xem đơn/ảnh/yêu cầu người khác; Admin không lộ password hoặc private note cho khách.',
'4. Xóa PC có đơn/category có PC/user có đơn bị chặn; khóa Admin cuối bị chặn.',
'5. Sửa lượng COD giữ giá snapshot và cập nhật reserved/payment amount; đã trả hoặc xác nhận bị chặn.',
'6. Bảo hành biên hết hạn, request trùng, hủy khi REQUESTED, từ chối có lý do và hoàn tất có kết quả.']
write(DOC/'requirements/TRACEABILITY_MATRIX.md','\n'.join(trace))

index=['# Danh mục sơ đồ báo cáo lần 1','',
f'**{len(diagram_rows)} sơ đồ:** 1 use case tổng quát, 12 activity, 1 ERD tổng và 4 ERD chi tiết. Không tạo use case riêng cho từng nút CRUD/OTP.','',
'| Mã | Nội dung | Mục đích | Nguồn | PNG |','|---|---|---|---|---|']
for code,title,purpose in diagram_rows:
    index.append(f'| {code} | {title} | {purpose} | [PlantUML](source/{code}.puml) | [Ảnh](png/{code}.png) |')
index+=['','## Cách dựng lại','',
'Chạy `python docs/tools/report01/build.py`, rồi `python docs/tools/report01/render.py` từ repository. Bộ dựng chỉ viết tài liệu/sơ đồ; không sinh code ứng dụng hay migration. Java và Python cần sẵn; renderer tải PlantUML 1.2025.4 từ Maven Central vào thư mục temp nếu chưa có. Nội dung diagram được render tại máy, không upload dữ liệu lên dịch vụ vẽ online.',
'','`model-input.xml` là định dạng trung gian của bộ sinh Open API, **không phải** XML trao đổi của Visual Paradigm. Project `.vpp` và XML do VP xuất được kiểm riêng trong [kết quả kiểm tra](../../../reports/REPORT_01_VALIDATION.md).']
index+=['','## Visual Paradigm','',
'[Mở project VPP](PC_STORE_REPORT_01.vpp) — 1 use case tổng và 12 activity dạng phần tử native. [XML trao đổi](vp-export/project.xml) giữ cùng [data.zip](vp-export/data.zip). 5 ERD bàn giao bằng nguồn PlantUML và PNG ở bảng trên; chưa có ERD native do API tạo khóa ngoại báo lỗi.']
write(DIAG/'README.md','\n'.join(index))
ET.indent(model)
ET.ElementTree(model).write(DIAG/'model-input.xml',encoding='utf-8',xml_declaration=True)
write(DIAG/'manifest.json',json.dumps([dict(id=a,title=b,purpose=c) for a,b,c in diagram_rows],ensure_ascii=False,indent=2))

report=['# Báo cáo lần 1 — Phân tích và thiết kế PC Store','',
'**Nhóm 16 — Lập trình WWW Java — ThS. Đặng Thị Thu Hà**  \n**Phiên bản 2.1 — 26/09/2026 — Trạng thái: tài liệu thiết kế, chưa triển khai ứng dụng.**',
'','## 1. Bài toán, mục tiêu và phạm vi','',
'PC Store giúp khách tìm PC hoàn chỉnh theo nhu cầu, xem cấu hình và đặt mua; Admin quản lý danh mục, xác nhận/giao đơn và tiếp nhận bảo hành. Điểm nhấn là **tư vấn PC có giải thích**, lọc đúng ngân sách và nêu hạn chế thay vì chỉ liệt kê sản phẩm.',
'','Phạm vi phù hợp nhóm năm người: **7 module, 24 màn hình, 12 chức năng chính**. Không có tự build linh kiện, serial, đa kho, vận đơn, hoàn tiền, dashboard doanh nghiệp hay quản trị rule riêng. Bảo hành chỉ theo dòng đơn và vị trí máy, có trạng thái và kết quả cho khách xem.',
'','Tài liệu môn học yêu cầu lần 1 tập trung phân tích, use case tổng quát và thiết kế CSDL. Bảng đặc tả và activity dưới đây làm rõ các chức năng trước khi hiện thực. Không tách mỗi thao tác CRUD/OTP thành use case độc lập. Phân tích chi tiết: [đặc tả 12 chức năng](../requirements/FUNCTIONAL_SPECIFICATION.md); [quy tắc nghiệp vụ](../requirements/BUSINESS_RULES.md).',
'','## 2. Tác nhân và use case tổng quát','',
'Guest được xem PC, dùng tư vấn và giỏ. Customer có các khả năng truy cập chung và được đặt/theo dõi đơn, dùng bảo hành của mình. Admin có chức năng Customer và quyền quản lý. Google, VNPAY, OTP Provider, Mail Service là tác nhân hỗ trợ ngoài hệ thống. “Người truy cập” là actor trừu tượng cho khả năng dùng chung, tránh coi Customer là người chưa đăng nhập.',
'',img('UC-OVERVIEW','Use case tổng quát'),
'','Sơ đồ dùng association và generalization đúng vai trò; không ép include/extend để trang trí. Login là tiền điều kiện của thao tác bảo vệ. Scheduler hết hạn và database là bên trong hệ thống, không vẽ thành người dùng ngoài.',
'','## 3. Bảng đặc tả chức năng và activity','',
'Mỗi mục dẫn tới bảng đặc tả có tiền/hậu điều kiện, input, bước chính, nhánh thay thế, quy tắc, dữ liệu và tiêu chí nghiệm thu. Mã Bn/E-Bn được giữ giữa phần chữ và activity. Các thao tác nhỏ nằm trong bảng luồng con để báo cáo gọn và dễ bảo vệ.','']
for c in CASES:
    report += [f'### {c["code"]} — {c["name"]}','',f'**Mục tiêu:** {c["goal"]}',
    '', '| Tác nhân | Tiền điều kiện | Kết quả |','|---|---|---|', f'| {c["actor"]} | {cell(c["pre"])} | {cell(c["post"])} |',
    '',f'[Bảng đặc tả đầy đủ {c["code"]}](../requirements/FUNCTIONAL_SPECIFICATION.md#{c["code"].lower()})','',img('ACT-'+c['code'],c['name']),'',
    '**Điểm cần giải thích:** '+c['alternates'][0][2],'']
report += ['## 4. Thiết kế cơ sở dữ liệu','',
'20 bảng phục vụ identity, catalog, sales, payment và aftersales. Cart và quote nằm trong session; advisory đọc catalog/profile, không cần bảng mới. Bảng order_items lưu snapshot để lịch sử mua hàng và bảo hành không thay đổi theo catalog.',
'',img('ERD-ALL','ERD tổng quát'),
'','[ERD chi tiết và từ điển đầy đủ 20 bảng](../data/DATABASE_DESIGN.md). Khóa ngoại không cascade xóa lịch sử; validation nghiệp vụ tại Java. Giữ tồn được biểu diễn bằng marker trên orders và reserved_quantity ở products, không có bảng reservation riêng.',
'','## 5. Các quyết định nghiệp vụ cần bảo vệ','',
'- Giá và phí do server tính; quote10 phút không giữ hàng. Chỉ commit đặt đơn mới tăng reserved.',
'- Đơn COD chờ xác nhận mới được sửa lượng/hủy; giữ đơn giá snapshot. SHIPPING giảm onHand/reserved đúng một lần; DELIVERED ghi COD PAID.',
'- VNPAY có một attempt/order, hạn15 phút. Return không quyết định PAID; IPN hợp lệ mới cập nhật. Tiền đến muộn được đánh dấu REVIEW_REQUIRED, không hồi sinh đơn.',
'- Bảo hành bắt đầu lúc giao thành công, cộng tháng lịch từ snapshot; yêu cầu gửi hợp lệ được tiếp tục xử lý sau hết hạn. Không hai request mở cho cùng item/unit.',
'- Chủ đơn/yêu cầu mới xem dữ liệu riêng; Admin note không xuất hiện trong DTO Customer.',
'','## 6. Phân công, kiểm chứng và nguồn','',
'| Thành viên | Nội dung báo cáo/thiết kế phụ trách theo PLAN |','|---|---|',
'| Võ Văn Cảnh | UC05, transaction đặt hàng, thanh toán |',
'| Bùi Ngọc Bửu | UC02/UC07, catalog và số dư tồn, ERD |',
'| Huỳnh Đoàn Nhân | UC03/UC04, tư vấn và giỏ, nối UI |',
'| Võ Văn Nhựt | UC01/UC09, xác thực và phân quyền |',
'| Lê Thị Kim Ngân | UC06/UC08/UC10–UC12, đơn và bảo hành, truy vết/báo cáo |',
'', 'Đây là phân công theo kế hoạch, không phải tuyên bố ai đã hoàn thành code. [Ma trận truy vết](../requirements/TRACEABILITY_MATRIX.md) bao phủ24 màn hình; [kết quả kiểm tra artifact](REPORT_01_VALIDATION.md) ghi những kiểm tra đã chạy, tách khỏi nghiệm thu ứng dụng dự kiến.',
'','Nguồn đầu vào: phiếu đăng ký `23676641_DangkyDetai.pdf`, tài liệu môn `Phieu DK _BaiTap_Nhom_LapTrinhWWWJava_1 (1).docx`, PLAN/UI phiên bản hiện hành. Giao thức thanh toán đối chiếu [VNPAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html); ký pháp theo [OMG UML](https://www.omg.org/spec/UML/2.5.1/); công cụ dựng nguồn sơ đồ [PlantUML](https://plantuml.com/).',
'', 'Giới hạn đồ án: số máy theo vị trí không thay thế serial; tiếp nhận bảo hành/giao hàng được Admin xác nhận thủ công. Chưa có dữ liệu khảo sát cửa hàng hoặc kiểm thử ứng dụng, do đó không ghi nhận số liệu hiệu năng/doanh thu hay kết quả triển khai giả.']
write(DOC/'reports/REPORT_01_ANALYSIS_AND_DESIGN.md','\n'.join(report))
print(f'Built {len(CASES)} use cases, {len(diagram_rows)} diagram sources, {len(TABLES)} table dictionaries')
