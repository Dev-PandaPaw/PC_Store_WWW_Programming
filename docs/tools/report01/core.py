"""12 use cases for the first academic report. CRUD/OTP are subflows, not extra features."""
CASES=[]
def case(code,name,actor,screens,module,goal,pre,inputs,steps,alternates,post,rules,tables):
    CASES.append(dict(code=code,name=name,actor=actor,screens=screens,module=module,goal=goal,pre=pre,inputs=inputs,steps=steps,alternates=alternates,post=post,rules=rules,tables=tables))
# step: (actor, action, guard-or-empty, terminal-alternative-or-empty)
def U(action): return ('Người dùng',action,'','')
def S(action,guard='',failure=''): return ('PC Store',action,guard,failure)

case('UC01','Tài khoản và xác thực','Guest, Customer','A01–A05, C01','identity',
'Tạo và sử dụng tài khoản để mua hàng; quản lý thông tin liên hệ của chính mình.',
'Các chức năng công khai không cần login. Sửa hồ sơ/liên kết cần phiên ACTIVE; thay liên hệ cần xác thực lại.',
'Email hợp lệ ≤254 ký tự; mật khẩu 8–72 byte UTF-8; tên 2–100 ký tự; phone E.164 Việt Nam; OTP 6 số. Profile: tên, một địa chỉ mặc định; không nhận role/status từ form.',[
U('Chọn đăng ký, đăng nhập, khôi phục hoặc cập nhật hồ sơ'),
S('Xác định thao tác; kiểm định dạng và quyền thao tác','Dữ liệu và quyền hợp lệ?','Báo lỗi field hoặc yêu cầu đăng nhập; không lưu'),
S('Xác minh bằng mật khẩu, OTP hoặc Google theo nhánh đã chọn','Bằng chứng xác thực hợp lệ?','Báo xác thực thất bại/hết hạn; không cấp phiên, không đổi liên hệ'),
S('Kiểm email/phone duy nhất, tài khoản ACTIVE và phương thức đăng nhập còn lại','Không trùng và tài khoản dùng được?','Báo trùng/khóa/không thể bỏ phương thức cuối; giữ dữ liệu cũ'),
S('Tạo/cập nhật tài khoản hoặc cấp phiên; tiêu thụ challenge dùng một lần'),
S('Đổi session ID khi login, giữ giỏ; trả hồ sơ hoặc màn hình đích')],
[(1,'Đăng ký','Email/password tạo Customer; phone đăng ký chỉ tạo sau OTP đúng; Google mới tạo identity theo issuer+subject.'),
 (3,'Quên mật khẩu','Form luôn trả thông báo trung tính. Link reset hạn 15 phút, dùng một lần; đặt mật khẩu mới vô hiệu phiên cũ rồi về login.'),
 (3,'Google trùng email','Không tự gộp. Đăng nhập tài khoản hiện hữu rồi link Google trong hồ sơ; identity thuộc user khác bị từ chối.'),
 (3,'Đổi liên hệ','Phone/email chỉ có hiệu lực sau xác minh; giữ liên hệ cũ trước khi hoàn tất. OTP hạn 5 phút, 5 lần sai, resend ≥60 giây.'),
 (5,'Hồ sơ và logout','Sửa tên/địa chỉ không đổi snapshot đơn. Logout hủy session, giỏ và quote; gỡ Google bị chặn nếu không còn cách đăng nhập.')],
'Đúng tài khoản được xác thực/cập nhật; lỗi không cấp quyền. Không lộ mật khẩu/OTP/token.',
'BR01 BR02 BR14','users user_roles external_identities verification_challenges')

case('UC02','Tìm và xem PC','Guest, Customer','S01–S03','catalog',
'Tìm một PC đang bán và đọc cấu hình, giá, bảo hành trước khi lựa chọn.',
'Không cần đăng nhập. Chỉ PC ACTIVE được công bố.',
'Từ khóa tên/SKU ≤100; loại, hãng, giá min/max, RAM, GPU; sort theo allowlist; page ≥0, size ≤100; slug PC.',[
U('Mở trang chủ hoặc danh sách PC; nhập từ khóa/bộ lọc'),
S('Kiểm bộ lọc, khoảng giá và phân trang','Bộ lọc hợp lệ?','Giữ form, thông báo điều kiện sai'),
S('Tìm PC ACTIVE, phân trang và sắp xếp ổn định','Có kết quả?','Hiện danh sách rỗng và nút xóa bộ lọc'),
U('Chọn PC cần xem'),
S('Đọc lại PC theo slug, cấu hình, ảnh và bảo hành','PC vẫn ACTIVE?','Trả 404; gợi ý quay danh sách'),
S('Hiển thị chi tiết và available; khóa nút thêm giỏ nếu hết hàng')],
[(1,'Trang chủ','Hiển thị tối đa 4 PC ACTIVE còn hàng; nếu không có vẫn giữ CTA catalog/tư vấn.'),
 (3,'Thay filter/sort','Quay lại bước 2; lưu query trên URL để back/refresh giữ điều kiện.'),
 (6,'Hết hàng','Vẫn xem cấu hình nhưng không thêm giỏ. Mở URL PC INACTIVE/DISCONTINUED trả 404; đơn cũ vẫn dùng snapshot.')],
'Hiển thị thông tin hiện tại; chưa giữ hàng và chưa phát sinh đơn.',
'BR03 BR15','products categories product_specs product_images product_suitability')

case('UC03','Tư vấn chọn PC','Guest, Customer','S04','advisory',
'Nhận tối đa ba PC phù hợp nhu cầu và ngân sách, kèm lý do và hạn chế rõ ràng.',
'Không cần đăng nhập; Admin đã nhập dữ liệu phù hợp theo nhu cầu trong form PC.',
'usageProfile: OFFICE_STUDY/PROGRAMMING/GAMING/CONTENT_CREATION; maxBudget nguyên 1..1 tỷ VND; minRamGb 0..1024, minStorageGb 0..100000, mặc định 0.',[
U('Chọn nhu cầu và nhập ngân sách, RAM, dung lượng tối thiểu'),
S('Kiểm enum và giới hạn số','Tiêu chí hợp lệ?','Báo lỗi theo field; giữ form'),
S('Lọc PC ACTIVE, còn hàng, có profile, không vượt ngân sách và đủ cấu hình','Có PC đạt tất cả điều kiện?','Trả rỗng, chỉ ra bộ lọc làm hết ứng viên; không tự tăng ngân sách'),
S('Xếp score giảm, giá tăng, SKU tăng; lấy tối đa 3'),
S('Hiển thị lý do, hạn chế, giá và phần ngân sách còn lại'),
U('Mở chi tiết hoặc chỉnh tiêu chí để tư vấn lại')],
[(3,'Không phù hợp','Cho chỉnh tiêu chí và quay bước 1; không đề xuất PC vượt trần như thể đã đạt yêu cầu.'),
 (4,'Đồng điểm','Giá thấp hơn đứng trước, cuối cùng SKU tăng để kết quả ổn định.'),
 (5,'Diễn giải điểm','Score5: Rất phù hợp; 3–4: Phù hợp; 1–2: Cân nhắc. Đây là rule có dữ liệu Admin, không phải AI/FPS/benchmark.')],
'Kết quả có thể giải thích; ngân sách chỉ tính giá PC, chưa phí giao; không reserve.',
'BR10','products product_specs product_suitability product_images')

case('UC04','Quản lý giỏ hàng','Guest, Customer','S05; nút thêm tại S02–S04','cart',
'Tập hợp PC và số lượng trước khi checkout bằng giỏ HTTP Session.',
'Có session; không yêu cầu đăng nhập. Giỏ tối đa 20 SKU.',
'productId UUID; quantity nguyên 1..99; cartVersion; CSRF cho mutation. Không nhận giá có thẩm quyền.',[
U('Xem giỏ hoặc chọn thêm, đổi số lượng, xóa dòng'),
S('Đọc session cart và kiểm version cho thao tác ghi','Version và số lượng hợp lệ?','409 nếu hai tab xung đột hoặc lỗi lượng; tải lại, giữ giỏ mới hơn'),
S('Đọc catalog; thêm/đổi lượng cần PC ACTIVE và đủ available, xóa được cả dòng không còn bán','Thao tác được phép?','Giữ giỏ; báo thiếu hàng/ngừng bán/giới hạn 20 SKU'),
S('Cập nhật dòng: thêm trùng SKU cộng lượng, đổi lượng thay thế, xóa bỏ dòng; tăng version khi thay đổi'),
S('Tính lại giá hiện tại và subtotal; đánh dấu dòng không mua được'),
S('Hiện giỏ mới, empty state hoặc CTA checkout nếu hợp lệ')],
[(1,'Chỉ xem','Không mutate version; đọc lại giá và stock. Dòng PC đã xóa vẫn có cảnh báo và nút bỏ.'),
 (4,'Quantity bằng 0','Không dùng 0 để xóa; yêu cầu action xóa rõ ràng.'),
 (6,'Login/logout','Login đổi session ID nhưng giữ giỏ; logout hủy giỏ. Sau đặt thành công chỉ dọn giỏ nếu version vẫn khớp.')],
'Giỏ session cập nhật, không có cart table và không giữ tồn.',
'BR04','products product_specs product_images')

case('UC05','Đặt hàng và thanh toán','Customer','C02–C03','sales, payment',
'Tạo một đơn từ giỏ và chọn COD hoặc VNPAY sandbox, không bán vượt tồn hoặc nhân đôi đơn.',
'User ACTIVE đã đăng nhập, có phone verified; giỏ hợp lệ; địa chỉ đủ thông tin.',
'Người nhận 2–100; province/ward 1–100; district tùy chọn; addressLine 5–255; note ≤500; COD/VNPAY; quoteId, cartVersion, idempotencyKey.',[
U('Nhập nơi nhận và chọn COD hoặc VNPAY'),
S('Kiểm tài khoản, phone, giỏ, địa chỉ; tính giá + phí 50.000 và quote hạn 10 phút','Đủ điều kiện checkout?','Yêu cầu xác minh phone, sửa giỏ/địa chỉ; chưa tạo đơn'),
U('Xem tổng và xác nhận đặt hàng'),
S('Kiểm idempotency, khóa user và products theo ID; đọc lại quote/giá/stock','Key mới và quote/stock còn hợp lệ?','Cùng key/payload trả đơn cũ; mismatch hoặc thay đổi trả 409, không tạo đơn mới'),
S('Trong một transaction: tạo order/items snapshot/attempt/history, tăng reserved, lưu idempotency'),
S('Commit, dọn giỏ đúng version và gửi mail sau commit'),
S('COD: chờ xác nhận; VNPAY: redirect URL ký cùng reference và hạn 15 phút'),
S('VNPAY: IPN xác minh cập nhật; return chỉ đọc trạng thái; hiển thị kết quả server')],
[(4,'Cạnh tranh tồn','Khóa SKU theo ID; không đủ lượng rollback toàn bộ, yêu cầu preview lại. Một key không tạo hai đơn.'),
 (7,'COD','Order AWAITING_CONFIRMATION, payment PENDING; chỉ ghi PAID cùng lúc Admin xác nhận DELIVERED.'),
 (7,'Mở lại VNPAY','Chỉ PENDING dùng cùng reference/deadline, không tạo attempt mới. FAILED: hủy/đặt lại hoặc chờ hết hạn.'),
 (8,'IPN không hợp lệ/trùng','Sai chữ ký/ref/amount không thay tài chính. Trùng không xử lý lần hai. Return query không được ép PAID.'),
 (8,'Hết hạn/thành công muộn','Chưa trả sau 15 phút: EXPIRED và release một lần. Thành công sau hạn/hủy: REVIEW_REQUIRED, không hồi sinh order.'),
 (6,'Lỗi email','Không rollback đơn đã commit; khách xem kết quả tại tài khoản. Không có durable outbox trong scope.')],
'Đơn và tồn nhất quán; thanh toán online chỉ xác nhận qua IPN hợp lệ. Không có hoàn tiền/đối soát trong scope.',
'BR05 BR06 BR07 BR08 BR09 BR14','users products orders order_items payment_attempts payment_events order_status_history idempotency_records')

case('UC06','Theo dõi và hủy đơn của tôi','Customer','C04–C05','sales',
'Xem đơn đã đặt, tiến trình xử lý và tự hủy khi còn cho phép.',
'User ACTIVE; mọi truy vấn giới hạn theo principal.',
'Mã đơn, status filter, page/size; hủy có version và reason 5–500.',[
U('Mở danh sách và chọn đơn của mình'),
S('Tra đơn theo code + currentUser','Đơn thuộc người đang đăng nhập?','404; không tiết lộ chủ đơn khác'),
S('Hiển thị snapshots, tổng tiền, timeline, trạng thái payment và allowedActions'),
U('Nếu cần, chọn hủy và xác nhận lý do'),
S('Khóa order/attempt, so version và điều kiện hủy','COD chờ xác nhận chưa trả hoặc VNPAY chờ trả, HELD?','409 không được hủy; giữ đơn và tồn'),
S('Giảm reserved, đổi RELEASED/CANCELLED và ghi history trong cùng transaction'),
S('Trả trạng thái mới; đơn DELIVERED có liên kết tới bảo hành')],
[(3,'Chỉ theo dõi','Kết thúc sau hiển thị chi tiết, không bắt buộc chọn hủy.'),
 (5,'Đã thanh toán online/đã xác nhận COD','Không tự hủy; UI không đưa action ngoài điều kiện.'),
 (6,'Hủy và IPN đồng thời','Cùng khóa order/attempt; thao tác đến sau đọc lại trạng thái. Nếu hủy trước mà tiền đến sau thì REVIEW_REQUIRED.')],
'Đọc đúng đơn; nếu hủy thành công giải phóng tồn đúng một lần; lịch sử không bị xóa.',
'BR02 BR06 BR07 BR09','orders order_items order_revisions order_status_history payment_attempts products')

case('UC07','Quản lý PC và loại sản phẩm','Admin','M01–M03','catalog',
'Duy trì PC, cấu hình, ảnh, giá, dữ liệu tư vấn, loại PC và tồn đơn giản.',
'Admin ACTIVE. PC là cấu hình hoàn chỉnh; không có phân hệ linh kiện/đa kho.',
'SKU ≤50, slug ≤180, tên 2–180; categoryId; giá nguyên 1..1 tỷ; specs; warranty 0..60 tháng; version. Gallery ≤8 ảnh 5MiB. Delta tồn khác 0, reason 5–500.',[
U('Tìm PC/loại và chọn thêm, sửa, trạng thái, xóa hoặc điều chỉnh tồn'),
S('Kiểm quyền, field, ảnh, unique SKU/slug và version','Dữ liệu và version hợp lệ?','Báo field/trùng/stale; giữ form'),
S('Kiểm điều kiện riêng: xóa không tham chiếu, tồn không dưới reserved, ACTIVE đủ cấu hình','Thỏa điều kiện nghiệp vụ?','Chặn xóa dữ liệu đã dùng hoặc giảm quá tồn; đề nghị ngừng bán'),
S('Lưu thay đổi catalog trong transaction; stock adjustment ghi actor, before/after và lý do'),
S('Commit, dọn media bỏ sau commit; trả danh sách/chi tiết cập nhật')],
[(1,'Thêm/sửa PC','Tạo SKU mới cho cấu hình lớn; SKU sau tạo bất biến. Edit không được ghi stock/reserved bằng field thường.'),
 (1,'Gallery và tư vấn','Ảnh kiểm MIME/size, chọn cover và thứ tự; profile 4 nhu cầu score1–5 kèm lý do; nằm trong M02, không thêm màn hình.'),
 (3,'Xóa PC/loại','PC có order item không xóa; category có bất kỳ PC nào không xóa. Không cascade xóa đơn.'),
 (3,'Điều chỉnh tồn','Khóa product, newOnHand=old+delta; reserved≤newOnHand≤10000. Không chỉnh reserved trực tiếp.'),
 (4,'Ngừng bán','Đổi DISCONTINUED/INACTIVE, không hủy đơn đã có. Category không status, chỉ PC có status.')],
'Catalog cập nhật; snapshots đơn cũ không đổi, tồn có thể giải thích; không tạo ledger doanh nghiệp.',
'BR03 BR06 BR10 BR13','categories products product_specs product_images product_suitability inventory_adjustments order_items')

case('UC08','Xử lý đơn hàng','Admin','M04–M05','sales, payment',
'Xác nhận đơn, sửa lượng COD được phép, cập nhật giao hàng và xem ngoại lệ thanh toán.',
'Admin ACTIVE; order tồn tại; chỉ action hợp lệ được hiển thị.',
'Bộ lọc/mã đơn; action CONFIRM/REVISE/CANCEL/SHIP/DELIVER; version; reason khi sửa/hủy; itemId→quantity 0..99.',[
U('Tra cứu và mở chi tiết đơn'),
S('Đọc snapshots, timeline, payment summary và action hợp lệ','Đơn tồn tại?','404; trở về danh sách'),
U('Chọn action và xác nhận thông tin'),
S('Khóa order/attempt, so version và bảng chuyển trạng thái','Action hợp lệ?','409; tải lại dữ liệu mới, không ép trạng thái'),
S('Sửa/hủy/giao: khóa products theo ID; kiểm và cập nhật lượng giữ/xuất','Đủ tồn và đơn còn ít nhất một item khi sửa?','409 thiếu tồn hoặc 422 đơn rỗng; rollback'),
S('Ghi order/revision/history/payment liên quan và commit'),
S('Hiển thị timeline mới; DELIVERED bắt đầu quyền bảo hành')],
[(4,'Sửa lượng','Chỉ COD PENDING, AWAITING_CONFIRMATION. Giữ đơn giá/phí snapshot; không thêm SKU; lượng0 bỏ dòng, phải còn dòng dương; cập nhật cả payment amount.'),
 (4,'Xác nhận/hủy','CONFIRM từ chờ xác nhận nếu COD PENDING hoặc VNPAY PAID. Admin chỉ hủy COD chưa xác nhận/chưa trả.'),
 (5,'Giao hàng','CONFIRMED→SHIPPING: giảm onHand và reserved, HELD→CONSUMED. Không tích hợp vận đơn.'),
 (6,'Giao thành công','SHIPPING→DELIVERED: deliveredAt server; COD chuyển PAID cùng transaction. Không tác động tồn lần nữa.'),
 (2,'REVIEW_REQUIRED','Hiện cảnh báo để xử lý thủ công ngoài hệ thống; không có nút ép PAID, khôi phục đơn hay refund.')],
'Chỉ chuyển trạng thái hợp lệ; lịch sử actor/thời gian/lý do được giữ; không phát sinh quản trị logistics.',
'BR06 BR07 BR08 BR09 BR11','orders order_items products payment_attempts payment_events order_revisions order_status_history')

case('UC09','Quản lý tài khoản khách hàng','Admin','M06','identity',
'Tra cứu, cập nhật thông tin cần thiết, khóa/mở hoặc xóa tài khoản đủ điều kiện.',
'Admin ACTIVE; không có UI cấp role hoặc xem credential.',
'q ≤100; role/status/provider filter; tên/contact/địa chỉ; version; reason 5–500; action UPDATE/LOCK/UNLOCK/DELETE.',[
U('Tìm tài khoản và mở drawer'),
S('Đọc thông tin được phép, số đơn và các phương thức đăng nhập','User tồn tại?','404; không hiện dữ liệu credential'),
U('Chọn cập nhật, khóa, mở khóa hoặc xóa và nhập lý do'),
S('Khóa user, kiểm version, unique liên hệ và ràng buộc Admin cuối/lịch sử','Thao tác được phép?','409 do stale, trùng, Admin cuối, user có giao dịch hoặc mất phương thức cuối'),
S('Lưu thay đổi hoặc xóa user chưa có tham chiếu; đổi contact bỏ verified, tăng sessionVersion khi cần'),
S('Trả drawer mới; phiên bị khóa/vô hiệu sẽ bị chặn ở request bảo vệ tiếp theo')],
[(4,'Xóa','Không xóa user có đơn hoặc được tham chiếu làm actor lịch sử; đề nghị khóa.'),
 (4,'Admin cuối','Không khóa/xóa Admin ACTIVE cuối. Không nhận role/password từ form quản lý.'),
 (5,'Đổi contact','Không tự xác minh thay khách; không đổi phone-only sang phone chưa verified nếu không còn phương thức khác.'),
 (5,'Mở khóa','Chuyển ACTIVE; yêu cầu login mới, không khôi phục session cũ.')],
'Tài khoản được quản lý đúng điều kiện; đơn/snapshot không bị sửa theo profile.',
'BR01 BR02 BR15','users user_roles external_identities verification_challenges orders')

case('UC10','Tra cứu bảo hành và tiến độ','Customer','C05–C07','aftersales',
'Biết mặt hàng đã mua còn bảo hành không và xem tình trạng yêu cầu đã gửi.',
'Đăng nhập ACTIVE; chỉ dữ liệu của chính Customer.',
'Tab còn hạn/hết hạn/yêu cầu đã gửi; orderItemId/unitIndex hoặc requestCode; page/size.',[
U('Mở Bảo hành của tôi hoặc bảo hành từ chi tiết đơn'),
S('Tra đơn DELIVERED của currentUser và item snapshot','Có mặt hàng đã giao?','Hiển thị empty; giải thích chỉ đơn đã giao có bảo hành'),
S('Tính thời hạn từ deliveredAt + tháng snapshot, theo tháng lịch giờ Việt Nam'),
S('Hiển thị từng vị trí 1..quantity, trạng thái còn/hết hạn và yêu cầu đang mở'),
U('Mở chi tiết một yêu cầu đã gửi nếu có'),
S('Kiểm owner, đọc ảnh và timeline công khai','Yêu cầu thuộc Customer?','404; không lộ chủ khác hoặc internal note'),
S('Hiển thị kết quả/từ chối và action được phép kể cả yêu cầu đã đóng')],
[(3,'Hết hạn','now≥end hoặc warrantyMonths=0 không tạo yêu cầu mới; vẫn xem lịch sử.'),
 (4,'Không có yêu cầu','Kết thúc tại quyền lợi; chọn gửi mới chuyển UC11 nếu đủ điều kiện.'),
 (6,'Ảnh private','Chỉ owner/Admin được xem qua endpoint kiểm quyền, không có public static URL.')],
'Khách thấy quyền lợi và tiến độ của mình; không có hồ sơ serial hay cam kết ngoài snapshot.',
'BR02 BR11 BR12 BR13','orders order_items warranty_requests warranty_request_images warranty_request_events')

case('UC11','Gửi hoặc hủy yêu cầu bảo hành','Customer','C07','aftersales',
'Đề nghị hỗ trợ cho một đơn vị PC đã mua, hoặc hủy khi cửa hàng chưa tiếp nhận.',
'Đăng nhập; item thuộc đơn DELIVERED của mình; tạo mới còn hạn và không có yêu cầu mở cùng item/unit.',
'orderItemId, unitIndex 1..quantity; description 10–2000; ≤3 ảnh JPEG/PNG/WebP ≤5MiB; clientRequestId. Hủy: requestCode, version, reason5–500.',[
U('Chọn gửi mới hoặc hủy yêu cầu của mình'),
S('Kiểm owner, đầu vào và ảnh; khóa item khi tạo hoặc request khi hủy','Dữ liệu và quyền hợp lệ?','Báo lỗi field/ảnh hoặc 404; không ghi một phần'),
S('Tạo: kiểm DELIVERED, còn hạn, unitIndex và không có yêu cầu mở; hủy: kiểm REQUESTED/version','Đủ điều kiện thao tác?','409 hết hạn/trùng/đã tiếp nhận; giữ dữ liệu cũ'),
S('Tạo REQUESTED + snapshot hạn + metadata ảnh + event; hoặc chuyển CANCELLED và ghi lý do trong transaction'),
S('Commit; gửi mail sau commit nếu có email verified'),
S('Trả mã yêu cầu và timeline công khai')],
[(3,'Submit lặp','Cùng clientRequestId/cùng payload trả yêu cầu cũ; khác payload trả409. Khóa item và unique active request chặn race.'),
 (3,'Hủy','Chỉ REQUESTED; nếu Admin đã RECEIVED thì không hủy. Không xóa yêu cầu khỏi lịch sử.'),
 (4,'Sau hết hạn','Yêu cầu đã được tạo hợp lệ vẫn được xử lý. Sau yêu cầu đóng có thể gửi mới nếu còn hạn.'),
 (5,'Mail lỗi','Không rollback request; Customer vẫn theo dõi tại UC10.')],
'Một yêu cầu mở/item/unit; không đổi tồn, không tạo shipment/refund/replacement.',
'BR11 BR12 BR13 BR14','orders order_items warranty_requests warranty_request_images warranty_request_events')

case('UC12','Tiếp nhận và xử lý bảo hành','Admin','M07','aftersales',
'Cập nhật tiến độ và kết quả bảo hành để khách theo dõi được.',
'Admin ACTIVE; request tồn tại. Tiếp nhận máy trực tiếp và trao đổi ngoài hệ thống.',
'Filter q/status; requestCode; version; action RECEIVE/PROCESS/COMPLETE/REJECT/NOTE; publicNote/internalNote; kết quả hoặc lý do10–2000 ký tự.',[
U('Tìm và mở yêu cầu bảo hành'),
S('Hiển thị thông tin đơn, item/unit, ảnh, thời hạn snapshot và timeline','Yêu cầu tồn tại?','404; quay danh sách'),
U('Chọn tiếp nhận, xử lý, hoàn tất, từ chối hoặc thêm ghi chú'),
S('Khóa request; so version, trạng thái và nội dung bắt buộc','Action và nội dung hợp lệ?','409 chuyển sai/stale hoặc422 thiếu kết quả/lý do; giữ trạng thái'),
S('Cập nhật trạng thái và append event; tách publicNote khỏi internalNote'),
S('Commit, gửi thông báo nếu có; Customer chỉ nhận phần công khai')],
[(4,'Chuyển trạng thái','REQUESTED→RECEIVED→PROCESSING→COMPLETED. Từ chối từ RECEIVED/PROCESSING→REJECTED, cần lý do công khai.'),
 (4,'Ghi chú','Chỉ RECEIVED/PROCESSING; ít nhất một public/internal note; không sửa event cũ, không reopen yêu cầu đóng.'),
 (4,'Đã hết hạn hiện tại','Không từ chối chỉ vì hiện tại quá hạn nếu yêu cầu đã được tạo hợp lệ trước hạn.'),
 (5,'Hoàn tất','Ghi kết quả công khai và closedAt; không gia hạn bảo hành, không sửa tồn hoặc tự hoàn tiền.')],
'Khách thấy tiến độ/kết luận rõ ràng; lịch sử được giữ và ghi chú nội bộ không bị lộ.',
'BR11 BR12 BR14','warranty_requests warranty_request_images warranty_request_events orders order_items users')

assert len(CASES)==12
