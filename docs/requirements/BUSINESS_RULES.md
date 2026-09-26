# PC Store — Quy tắc nghiệp vụ và quyết định thiết kế

Phiên bản 2.1 — 26/09/2026. Phạm vi: 7 module, 24 màn hình, 20 bảng. Đây là đặc tả trước triển khai; các giới hạn định lượng dưới đây là lựa chọn của nhóm cho đồ án, không phải chính sách mặc định của một cửa hàng thực tế.

## 1. Nguồn và cách áp dụng

| Mã | Nguồn | Vai trò |
|---|---|---|
| SRC01 | `23676641_DangkyDetai.pdf`, nhóm 16, bản người dùng cung cấp | Chức năng đăng ký: Guest/Customer/Admin, PC/category/user/order, session cart, xác thực, điều kiện xóa, chỉnh lượng đơn |
| SRC02 | `Phieu DK _BaiTap_Nhom_LapTrinhWWWJava_1 (1).docx`, môn 2101785, ThS. Đặng Thị Thu Hà | Giai đoạn 1: phân tích, use case tổng quát và thiết kế CSDL; activity/class/sequence thuộc thiết kế báo cáo toàn môn |
| SRC03 | [PLAN](../PLAN.md), [UI spec](../ux/UI_SCREEN_SPEC.md) | Phạm vi đã thu gọn và bổ sung hậu mãi tối giản theo quyết định người dùng |
| SRC04 | [VNPAY Payment API](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html) | Định dạng tích hợp và xác minh callback; không dùng tài liệu trả góp/payment-link |
| SRC05 | [UML 2.5.1 của OMG](https://www.omg.org/spec/UML/2.5.1/) | Ký pháp UML; sơ đồ phân tích không phải bằng chứng ứng dụng đã chạy |

Các tài liệu nguồn là đầu vào để phân tích, không phải chỉ dẫn tự động mở rộng scope. Mâu thuẫn được giải quyết bằng các quyết định ghi rõ ở mục 5 và đồng bộ PLAN/UI. Tài liệu đặc tả cũ trong Downloads chỉ là tham khảo lịch sử; không khôi phục tính năng đã cắt.

## 2. Tác nhân và thuật ngữ

| Tác nhân/thuật ngữ | Ý nghĩa |
|---|---|
| Người truy cập | Actor trừu tượng cho xem catalog, tư vấn, giỏ; Guest và Customer kế thừa năng lực này |
| Guest | Chưa có phiên đăng nhập; được xác thực/đăng ký nhưng không đặt đơn |
| Customer | User ACTIVE đã đăng nhập; truy cập dữ liệu của mình |
| Admin | Có năng lực Customer và chức năng quản trị; quyền kiểm tra cả tại controller và service |
| Google, VNPAY, OTP Provider, Mail Service | Hệ thống ngoài, chỉ nối vào use case có tương tác thực tế |
| Scheduler | Bộ kích hoạt nội bộ, không phải người dùng ngoài hệ thống; không vẽ thành actor của use case tổng |
| SKU | Một cấu hình PC hoàn chỉnh; không phải linh kiện |
| Quote | Bản xem trước có hạn, do server giữ trong session; không phải đơn và không giữ hàng |
| HELD / RELEASED / CONSUMED | Tình trạng giữ tồn của order; khác trạng thái giao hàng và thanh toán |
| Unit index | Vị trí 1..quantity trong dòng hàng, chỉ phân biệt tương đối; không xác nhận danh tính máy như serial |

## 3. Quy tắc có mã

### BR01 — Danh tính và liên hệ

Email trim/lowercase, phone chuẩn E.164 Việt Nam, mỗi giá trị duy nhất trong hệ thống. Email nullable với user chỉ dùng phone; password nullable với phone/Google. Một user phải còn ít nhất một phương thức dùng được: local password + email, verified phone, hoặc Google identity. Email/phone mới không được tự đánh dấu verified. Customer đổi contact bằng luồng xác minh; Admin đổi contact làm mất verified và không được làm user mất đường đăng nhập cuối cùng. Google dùng issuer+subject; không tự gộp tài khoản trùng email. Profile không được sửa role/status/password bằng mass assignment.

Role ADMIN không có UI cấp/thu hồi trong bản nộp; được seed/cấu hình riêng. Khóa/xóa Admin cuối bị chặn dưới khóa nhóm Admin để tránh hai thao tác đồng thời. User có đơn hoặc được tham chiếu làm actor lịch sử không hard-delete; thay bằng khóa. User chưa có giao dịch có thể xóa credential/challenge/role trước user trong một transaction.

### BR02 — Phiên, credential và quyền

BCrypt; mật khẩu 8–72 byte UTF-8 để tránh cắt ngầm giới hạn BCrypt, confirm phải trùng. Đổi session ID khi login và giữ giỏ; logout invalidate giỏ và quote. Session lưu userId + sessionVersion; mỗi protected request kiểm ACTIVE/version. Reset password, đổi contact và gỡ identity tăng sessionVersion. Sensitive contact/link/unlink yêu cầu xác thực lại trong 10 phút bằng phương thức đang dùng; hiển thị lại A01/A03, không thêm màn hình.

OTP 6 số, hạn 5 phút, 5 lần sai, resend ≥60 giây, ≤5 lần gửi/giờ/phone+IP. Mã được digest có khóa và challengeId; challenge gắn purpose/session và single-use. Reset/email token ngẫu nhiên có entropy ≥256 bit, lưu hash, hạn 15 phút. Link reset có thể mở từ browser khác; OTP không được chuyển sang session khác. Local/demo gắn nhãn giả lập, không cấu hình mã cố định cho production.

Mutations từ browser dùng CSRF; thiếu quyền 403, thiếu login 401 hoặc redirect login cho MVC. Tài nguyên Customer không thuộc chủ trả 404 thống nhất. Callback gateway không dùng session/CSRF nhưng bắt buộc chữ ký. Ẩn nút không thay authorization.

### BR03 — Catalog và quyền xóa

Một SKU bất biến đại diện một PC. Slug unique; giá nguyên 1..1.000.000.000 VND. Tồn 0..10.000; warrantyMonths 0..60. `ACTIVE` mới xuất hiện ở danh sách, tư vấn và chi tiết công khai; URL sản phẩm không ACTIVE trả 404. Admin vẫn xem mọi trạng thái; lịch sử đơn vẫn có snapshot. Mặc định PC mới INACTIVE cho đến khi đủ cấu hình; cấu hình lớn thay bằng SKU mới.

Category một cấp, không có status; loại rỗng vẫn có thể quản lý. Không xóa category có bất kỳ PC nào. Không xóa PC có order item, kể cả đơn đã hủy/hết hạn. Ngừng bán không hủy đơn đã đặt và không thay reservation. Xóa PC chưa có đơn xóa dữ liệu phụ catalog trong transaction, dọn file sau commit. SKU đã xóa chỉ có thể tái sử dụng khi không có bất kỳ lịch sử order nào — điều kiện xóa đã bảo đảm điều này.

### BR04 — Giỏ session

Tối đa 20 SKU, 1..99 chiếc/dòng. Lưu productId, quantity và cartVersion; không lưu cart table. Mọi write phải so cartVersion dưới khóa session. Thêm trùng SKU cộng lượng; cập nhật quantity=0 bị từ chối, xóa dùng action riêng. Không giữ tồn khi thêm giỏ. Giá lấy lại từ catalog khi đọc, kèm trạng thái lỗi nếu PC thiếu/ngừng bán/không đủ tồn. Quote đã có giá cũ phải xác nhận lại; giỏ không có giá snapshot để cam kết giá.

Commit đặt hàng thành công mới dọn giỏ: nếu cartVersion vẫn bằng version đã checkout thì bỏ các dòng đã dùng; nếu giỏ đã thay đổi giữ giỏ hiện tại và báo đơn đã tạo để tránh mất thao tác mới. Không tự trừ số lượng của dòng đã thay đổi ở tab khác. Logout xóa giỏ trên máy dùng chung.

### BR05 — Quote, giá và idempotency

`subtotal = Σ(unitPriceSnapshot × quantity)`; `grandTotal = subtotal + shippingFee`. Phí 50.000 VND cố định được snapshot; money BigDecimal/NUMERIC(19,0), không float. Giá PC không gồm phí giao trong bộ tư vấn. Quote giữ trong session 10 phút, gắn user, cartVersion, items, giá, phí, địa chỉ, verified phone và method. Bất cứ trường nào thay đổi phải preview lại; server fingerprint canonical object, không tin hidden amount.

Create order khóa user để kiểm contact và tuần tự hóa submit cùng user; recheck idempotency sau khi lấy lock. Key scope `(userId,requestKey)`; hash request chứa quoteId/cartVersion/method. Cùng key/payload trả đúng order cũ, kể cả quote sau đó hết hạn hoặc giỏ đã dọn; khác payload trả 409. Order, items, attempt, history, record idempotency và reserved cùng transaction. Rollback không có order dở dang. Không tạo bảng reservations riêng.

### BR06 — Tồn kho và khóa

`available = onHand − reserved`, luôn `0 ≤ reserved ≤ onHand`. `orders.reservation_status` là marker: tạo đơn HELD; hủy/hết hạn HELD→RELEASED và reserved giảm; giao hàng HELD→CONSUMED, onHand và reserved cùng giảm. Mọi chuyển marker đi cùng order/payment/items trong một transaction. Request lặp không được trừ lần hai.

Thứ tự khóa: user nếu cần → order → payment attempt → products theo UUID tăng. Tạo order mới chưa có order lock; khóa user rồi products theo UUID tăng. Sửa lượng dùng cùng thứ tự order/attempt/products. Bảo hành khóa order item → request khi cần; không cập nhật tồn. Không thao tác nào khóa product rồi quay lại khóa order đã tồn tại. Retry deadlock tối đa 2 lần với cùng idempotency key; thất bại trả conflict/retry, không thông báo thành công giả.

Điều chỉnh thủ công chỉ thay onHand, có reason và version, ghi inventory_adjustments. Có thể điều chỉnh SKU INACTIVE; không được xuống dưới reserved. Bảng này không phải sổ cái đầy đủ của reserve/ship.

### BR07 — Vòng đời đơn và sửa lượng

| Trạng thái nguồn | Hành động/tác nhân | Điều kiện | Đích | Tồn |
|---|---|---|---|---|
| Chưa có | Customer đặt COD | Quote hợp lệ | AWAITING_CONFIRMATION | HELD |
| Chưa có | Customer đặt VNPAY | Quote hợp lệ | AWAITING_PAYMENT | HELD |
| AWAITING_PAYMENT | IPN thành công | Nhận trước expiresAt; HELD; payment chưa PAID | AWAITING_CONFIRMATION | Giữ |
| AWAITING_PAYMENT | Customer hủy | PENDING/FAILED; HELD | CANCELLED | RELEASED |
| AWAITING_PAYMENT | Job hoặc IPN đến sau hạn | now ≥ expiresAt, chưa PAID | EXPIRED | RELEASED |
| AWAITING_CONFIRMATION | Customer/Admin hủy COD | COD PENDING; HELD | CANCELLED | RELEASED |
| AWAITING_CONFIRMATION | Admin sửa lượng | COD PENDING; HELD | Giữ nguyên | Điều chỉnh delta |
| AWAITING_CONFIRMATION | Admin xác nhận | COD PENDING hoặc VNPAY PAID | CONFIRMED | Giữ |
| CONFIRMED | Admin giao | HELD | SHIPPING | CONSUMED |
| SHIPPING | Admin xác nhận giao xong | Đã giao thực tế | DELIVERED | Không đổi |

Không cho hủy online đã trả hoặc COD đã CONFIRMED trong bản nộp. Sửa lượng chỉ trên item hiện tại, giữ đơn giá snapshot và phí snapshot; quantity=0 bỏ dòng nhưng phải còn ít nhất một dòng dương. Ghi revision trước/sau; cập nhật `payment_attempts.amount` của COD theo grandTotal mới. Không tăng quantity PC không ACTIVE; giảm/bỏ dòng vẫn được phép. Không tự thêm SKU.

### BR08 — COD

COD tạo PENDING, chuyển PAID cùng transaction DELIVERED. Đây là giả định demo, không khẳng định cửa hàng ngoài thực tế đã đối soát ngay khi giao. Không public API/nút ép PAID riêng. Hủy COD trước xác nhận chuyển CANCELLED. `paidAt` là thời gian server xác nhận giao thành công.

### BR09 — VNPAY và tình huống cạnh tranh

Một order có **đúng một attempt** trong scope. Mở lại trang gateway khi PENDING dùng cùng reference và deadline; không tạo attempt mới. FAILED không mở giao dịch mới trên order đó: Customer hủy/đặt lại hoặc chờ hết hạn. Không kéo dài 15 phút giữ hàng. Return chỉ đọc trạng thái, IPN mới quyết định tài chính sau xác minh. Amount giao thức bằng tổng VND nhân 100; không nhầm mã phản hồi IPN của merchant với mã kết quả thanh toán của gateway. [Giao thức VNPAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html).

| IPN đã xác minh | Trạng thái hiện tại | Kết quả |
|---|---|---|
| Cùng event đã xử lý | Bất kỳ | Ack 02, không thay đổi |
| Thành công | Order đang chờ, HELD, now < expiresAt; payment PENDING hoặc FAILED | PAID; order AWAITING_CONFIRMATION |
| Thành công | Order CANCELLED/EXPIRED hoặc now ≥ expiresAt | REVIEW_REQUIRED; nếu còn chờ thì EXPIRED và release HELD; không hồi sinh order |
| Thành công khác event | Đã PAID cùng transactionNo | Giữ PAID; ghi nhận đã xử lý, Ack 02 |
| Thành công khác transactionNo | Đã PAID | Đặt REVIEW_REQUIRED để báo nghi ngờ thu trùng; không thay order/tồn |
| Thất bại | PENDING, order còn chờ | FAILED; giữ HELD đến hủy/hết hạn |
| Thất bại đến sau thành công | PAID hoặc REVIEW_REQUIRED | Không downgrade; ghi IGNORED_AFTER_PAID |
| Thất bại sau hủy/hết hạn | CANCELLED/EXPIRED | Giữ nguyên; lưu event, không giữ hàng lại |

Job và IPN khóa cùng order/attempt. Deadline theo thời gian **server nhận callback**, không theo browser hoặc vnp_PayDate; giao dịch thực tế đã trả nhưng callback muộn được đưa kiểm tra thủ công. Đây là lựa chọn bảo thủ cho đồ án, phải giải thích ở báo cáo. REVIEW_REQUIRED không có chức năng refund/đối soát UI; Admin thấy cảnh báo trong chi tiết đơn, liên hệ xử lý ngoài hệ thống.

### BR10 — Tư vấn xác định được

Lọc lần lượt ACTIVE/còn hàng/có usageProfile → giá ≤ maxBudget → RAM → storage. Không nới điều kiện âm thầm. Sort score giảm, giá tăng, SKU tăng; lấy tối đa 3. Lý do và hạn chế do Admin nhập, không tự suy hiệu năng từ tên CPU/GPU. Khi không có kết quả, trả số ứng viên sau mỗi bộ lọc và chỉ ra bước đầu làm tập rỗng; đề nghị sửa tiêu chí, không đảm bảo tăng ngân sách là có kết quả. Score5=Rất phù hợp, 3–4=Phù hợp, 1–2=Cân nhắc. Kết quả không reserve; khi mua phải kiểm lại.

### BR11 — Quyền bảo hành và thời gian

Chỉ đơn DELIVERED; start=deliveredAt. Chuyển start sang Asia/Ho_Chi_Minh, cộng warrantyMonthsSnapshot bằng tháng lịch, ngày không có trong tháng đích được chặn về ngày cuối tháng; giữ giờ phút, đổi lại UTC để lưu end. Có hiệu lực khi tháng >0 và `start ≤ now < end`. Ví dụ 31/01/2026 10:00 +1 tháng =28/02/2026 10:00 giờ Việt Nam; đúng thời điểm end không được tạo mới. Không làm tròn lên hết ngày.

Khách xem item của mình và vị trí máy 1..quantity. Lịch sử đơn không đổi khi catalog thay tháng bảo hành. Khi tạo request snapshot start/end; yêu cầu nhận hợp lệ trước end tiếp tục được xử lý sau end. Yêu cầu đã đóng luôn xem được. Được gửi yêu cầu mới sau đóng nếu còn hạn; không có hai yêu cầu REQUESTED/RECEIVED/PROCESSING cho cùng item/unit. Khóa item và partial unique index bảo vệ race.

### BR12 — Trạng thái bảo hành và ghi chú

| Nguồn | Đích | Actor | Nội dung bắt buộc |
|---|---|---|---|
| Chưa có | REQUESTED | Customer | Mô tả lỗi |
| REQUESTED | CANCELLED | Chủ yêu cầu | Lý do hủy |
| REQUESTED | RECEIVED | Admin | Ghi chú tiếp nhận |
| RECEIVED | PROCESSING | Admin | Cập nhật xử lý |
| RECEIVED/PROCESSING | REJECTED | Admin | Lý do công khai |
| PROCESSING | COMPLETED | Admin | Kết quả công khai |
| RECEIVED/PROCESSING | Không đổi | Admin | Ít nhất publicNote hoặc internalNote |

RECEIVED nghĩa là đã tiếp nhận máy trực tiếp theo trao đổi ngoài hệ thống. Không ghi nhận vận chuyển bảo hành. Public/internal notes là hai trường tách biệt; Customer DTO không có internal field. Events append-only; trạng thái kết thúc không reopen hoặc sửa kết luận. Hoàn tất không gia hạn, không đổi máy, không tác động tồn/tiền.

### BR13 — Media

JPEG/PNG/WebP, ≤5 MiB/ảnh, kiểm chữ ký file và decode nội dung, giới hạn kích thước ảnh giải mã 25 megapixel. Tên ngẫu nhiên server-side; không thực thi file upload. PC tối đa 8 ảnh, bảo hành tối đa 3. Metadata chỉ commit khi file staging sẵn sàng; file mồ côi được dọn bằng script vận hành, không thêm module. Ảnh bảo hành phải qua endpoint kiểm owner/Admin; trả Cache-Control private, không public URL. Đọc ảnh lỗi trả ảnh lỗi an toàn và cho thử lại, không lộ storage path.

### BR14 — Email và lỗi ngoại vi

Gửi sau commit nếu email đã xác minh; email đăng ký/xác minh là ngoại lệ gửi đến email đích chưa verified để chứng minh quyền sở hữu. Sau đăng ký tạo được user dù mail lỗi. Email đặt hàng/bảo hành không phải nguồn trạng thái; UI đọc DB. Timeout SMTP ghi log đã lọc, không rollback order/request. Bản nộp không có durable outbox; sự cố process giữa commit và send có thể làm mất email, người dùng vẫn xem được trạng thái trong tài khoản. Lỗi OTP/OIDC trước xác thực không được cấp phiên.

### BR15 — Danh sách và phản hồi

Page 0-based, size mặc định 12 ở storefront, 20 ở khu tài khoản/Admin, tối đa 100; số âm/enum sai trả 422. Sort theo allowlist và thêm ID ổn định. Không kết quả là 200 danh sách rỗng. GET ID không tồn tại/không thuộc chủ trả 404. Mutation MVC theo PRG với flash error; REST trả Problem Detail: type,title,status,detail,instance,errorCode,fieldErrors,traceId. Không gửi stacktrace/SQL/secret. 409 cho stale version, thiếu tồn hoặc sai trạng thái; lỗi hệ thống 503/500 không báo thành công.

## 4. Yêu cầu phi chức năng và kiểm chứng dự kiến

| Mã | Yêu cầu đo được | Cách nghiệm thu khi triển khai |
|---|---|---|
| NFR01 | Owner/RBAC/CSRF áp dụng mọi mutation và dữ liệu riêng | MockMvc: Guest/Customer khác/Admin; IDOR trả 404, CSRF thiếu trả 403 |
| NFR02 | Không oversell, không ghi đơn/payment hai lần | PostgreSQL Testcontainers: tranh chiếc cuối, submit cùng key, IPN/expiry đồng thời |
| NFR03 | UI responsive 390/1440 px, form có label và lỗi theo field | Playwright + kiểm bàn phím; 24 screen IDs, không overflow toàn trang |
| NFR04 | Với fixture 30 PC/1000 đơn, 20 phiên đồng thời: p95 list <2 giây, checkout nội bộ <3 giây | Đo môi trường demo và ghi cấu hình; không tính latency gateway, không tuyên bố đã đạt trước code |
| NFR05 | UTF-8, tiền VND, UTC lưu trữ và giờ Việt Nam hiển thị nhất quán | Test ngày cuối tháng, biên hết hạn và snapshot |
| NFR06 | Không lộ password/OTP/token/payment secret/private note | Kiểm response, HTML, log và media authorization |
| NFR07 | Một process và một DB, module gọi contract công khai | Review dependency; controller MVC/REST dùng chung application service |
| NFR08 | Có hướng dẫn dựng diagram lại và traceability đầy đủ | Kiểm nguồn, ảnh, liên kết, mã UC/ACT/ERD/UI và checksum công cụ |

## 5. Quyết định đồng bộ v2.1

1. S03 trả 404 cho PC không ACTIVE, thống nhất với PLAN; không thêm trang tombstone.
2. Một order/một attempt; tiếp tục PENDING cùng reference, FAILED phải hủy/đặt lại. Không thêm hệ thống payment retry nhiều attempt.
3. Reservation là marker trên orders và số dư trên products; không thêm bảng thứ 21.
4. Địa chỉ giữ province/ward/addressLine; district tùy chọn để tương thích dữ liệu, không thêm danh mục hành chính.
5. Category không status; M03 bỏ cột trạng thái. M06 không action đổi role.
6. Giá PC dương, tháng bảo hành 0..60; điều chỉnh tồn chỉ nhập delta để tránh input mâu thuẫn.
7. Bảo hành tính tháng lịch và biên loại trừ end; yêu cầu hợp lệ tiếp tục xử lý sau end.
8. Không xóa actor đã được tham chiếu; giữ lịch sử thay vì vô hiệu FK.
9. Sửa lượng COD cập nhật cả amount của payment PENDING; tăng lượng chỉ với PC ACTIVE.
10. Các giới hạn form/rate limit được chốt để hiện thực và test; không phải thêm tính năng kinh doanh.

## 6. Giới hạn thực tế cần trình bày trung thực

Không có serial nên không chống được việc khách khai nhầm vị trí máy. Tiếp nhận thực tế do Admin đối chiếu ngoài hệ thống. Không có nghiệp vụ giao thất bại/hoàn hàng, hoàn tiền, bảo hành nhà sản xuất hoặc chi phí sửa chữa; các ngoại lệ đó cần nhân sự xử lý ngoài phần mềm. Đây là ranh giới đồ án đã chốt, không phải cam kết quy trình bán lẻ đầy đủ. Nét nổi bật là tư vấn có giải thích và luồng mua–theo dõi–bảo hành nhất quán, không phải số lượng trang quản trị.
