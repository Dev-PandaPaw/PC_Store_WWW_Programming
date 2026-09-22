# PC Store — Đặc tả định hướng và kế hoạch triển khai toàn dự án

## 1. Kết luận review và định hướng viết lại

**PC Store sẽ là website bán PC hoàn chỉnh, hỗ trợ tư vấn chọn máy có giải thích, quản lý bán hàng và theo dõi hậu mãi theo từng máy.** Hệ thống sử dụng **Java Spring Boot, Spring MVC, Thymeleaf**, triển khai dưới dạng **modular monolith**, phù hợp nhóm 5 sinh viên trong 10–14 tuần.

Ba kết quả cần đạt:

1. Đáp ứng đầy đủ chức năng đã đăng ký và thể hiện kiến thức môn học.
2. Xử lý đúng các nghiệp vụ quan trọng: tài khoản, giỏ hàng, tồn kho, đặt hàng, thanh toán, giao hàng và bảo hành.
3. Có điểm khác biệt demonstrable: khách hiểu **vì sao một PC phù hợp**, và sau mua có thể xem **hồ sơ chiếc máy thực tế mình nhận**.

Đây là bản kế hoạch và nội dung định hướng cho đặc tả thay thế; chưa chỉnh sửa các file gốc.

### 1.1. Căn cứ và thứ tự ưu tiên

| Nguồn | Cách sử dụng |
|---|---|
| Yêu cầu và lựa chọn của bạn | Chốt Thymeleaf, thời gian 10–14 tuần, tư vấn + hậu mãi, sandbox/giả lập |
| Phiếu đăng ký đề tài | Căn cứ chức năng nhóm đã đăng ký |
| Biểu mẫu môn học cung cấp ở lượt trước | Đối chiếu rubric, nội dung báo cáo và đánh giá cá nhân |
| Bản Markdown hiện tại | Bản nháp kỹ thuật để review, không mặc nhiên xem mọi đề xuất là yêu cầu bắt buộc |
| Tài liệu nhà cung cấp và cửa hàng thực tế | Căn cứ tham khảo nghiệp vụ, kỹ thuật; không sao chép chính sách thành cam kết của PC Store |

Phiếu đăng ký đã được đọc để đối chiếu các yêu cầu về tài khoản, Session, số điện thoại, quản trị và ràng buộc xóa. :codex-file-citation{path="C:/Users/Canh-dev/Downloads/23676641_DangkyDetai.pdf" purpose="source"}

### 1.2. Những điểm giữ, sửa và bổ sung

| Nội dung bản nháp | Quyết định viết lại |
|---|---|
| Modular monolith | Giữ; một ứng dụng, một cơ sở dữ liệu, module rõ trách nhiệm |
| React SPA và frontend riêng | Thay bằng Thymeleaf + Bootstrap + JavaScript theo lựa chọn của nhóm |
| Backend REST dùng lại về sau | Giữ application service độc lập với giao diện; chỉ mở REST cho các chức năng thực sự cần |
| Danh sách nhiều dịch vụ ngoài | Chốt VNPAY sandbox, Google; OTP và giao hàng có giả lập minh bạch |
| Trừ tồn kho khi tạo đơn/thanh toán | Sửa: giữ hàng khi đặt, xuất kho khi bàn giao vận chuyển |
| Clear giỏ hàng sau callback | Sửa: clear phần giỏ đã đặt sau khi transaction tạo đơn thành công |
| Sửa số lượng đơn chưa thanh toán | Thu hẹp có chủ đích: đơn COD đang chờ xác nhận, chưa đóng gói |
| Gửi email bằng async event | Bổ sung outbox để tránh mất thông báo khi ứng dụng dừng |
| Tài khoản trùng email Google | Bổ sung luồng liên kết có xác thực, không tự động gộp |
| Tư vấn, bảo hành để stretch | Đưa phiên bản vừa sức vào mục tiêu chính |
| Nhiều lựa chọn kiến trúc còn mở | Chốt một hướng thực hiện, ghi rõ phần mở rộng ngoài bản nộp |

**Lưu ý học thuật:** Thymeleaf không đồng nghĩa với JSP. Báo cáo phải trình bày đúng Spring MVC chạy trên Servlet và vai trò `DispatcherServlet`; không tuyên bố đã triển khai JSP. Trong tuần đầu, nhóm đối chiếu lựa chọn này với giảng viên vì rubric có nhắc JSP/Servlets.

---

## 2. Đặc tả sản phẩm được viết lại

### 2.1. Tên và mục tiêu

**Tên đề tài giữ nguyên:** Website giới thiệu, bán máy PC trực tuyến.

**Mô tả đề xuất:**

> PC Store là ứng dụng Web hỗ trợ khách hàng tìm hiểu, so sánh và lựa chọn máy PC hoàn chỉnh theo nhu cầu và ngân sách; quản lý giỏ hàng, xác thực tài khoản và đặt hàng trực tuyến. Hệ thống cung cấp khu vực quản trị sản phẩm, tồn kho, đơn hàng và hậu mãi. Mỗi máy được giao có hồ sơ serial và kết quả kiểm tra, giúp khách hàng theo dõi thông tin mua hàng và yêu cầu bảo hành. Ứng dụng được xây dựng bằng Java và Spring ecosystem, vận dụng Spring MVC, Spring Data JPA, Spring Security, Session, Web Services và kiểm thử nghiệp vụ.

### 2.2. Phạm vi kinh doanh

Chốt các mặc định:

- Một cửa hàng, một kho.
- Bán PC mới, cấu hình hoàn chỉnh, hàng có sẵn.
- Mỗi cấu hình bán là một SKU riêng.
- Một đơn được giao trọn gói trong một vận đơn.
- Ngôn ngữ tiếng Việt, tiền tệ VND.
- Không bán linh kiện rời hoặc nhận khách tự chọn linh kiện trong phiên bản này.
- Thông số linh kiện vẫn được lưu để tư vấn, so sánh và bảo hành.
- Không triển khai trả góp, đa người bán, đa chi nhánh, hóa đơn điện tử hoặc kế toán.
- Không xử lý tiền thật trong môi trường đồ án.

### 2.3. Phân tầng phạm vi

| Mức | Chức năng |
|---|---|
| **P0 — Bắt buộc theo đề tài** | Catalog, chi tiết PC, giỏ Session, đăng nhập Email/điện thoại/Google, hồ sơ, số điện thoại trước đặt hàng, tạo đơn, thông báo, quản trị sản phẩm/loại/tài khoản/đơn, sửa số lượng theo điều kiện, ràng buộc xóa |
| **P1 — Hoàn thiện nghiệp vụ và điểm khác biệt** | Giữ tồn kho, COD, VNPAY sandbox, lịch sử trạng thái, vận chuyển giả lập, tư vấn có giải thích, so sánh PC, serial, checklist kiểm tra, hồ sơ bảo hành và yêu cầu hậu mãi |
| **P2 — Sau khi bản nộp ổn định** | SMS thật, GHN thật, hoàn tiền tự động, khuyến mãi, tích điểm, đánh giá, bảo hành từng linh kiện nâng cao, dashboard vận hành mở rộng |

P0 và P1 là đích của kế hoạch 14 tuần. P2 không nằm trên đường găng.

### 2.4. Vai trò

| Vai trò | Quyền chính |
|---|---|
| Guest | Xem, tìm kiếm, lọc, so sánh, nhận tư vấn, sử dụng giỏ Session, đăng ký/đăng nhập |
| Customer | Quyền Guest; quản lý hồ sơ, xác minh điện thoại, đặt hàng, thanh toán, theo dõi đơn, xem máy đã mua, gửi yêu cầu bảo hành |
| Admin | Có quyền Customer theo phiếu đăng ký; quản lý catalog, tài khoản, kho, đơn, giao hàng, serial, bảo hành và nhật ký thao tác |

Trong bản nộp chỉ có ba vai trò. Các màn hình quản trị chia theo công việc để có thể tách Sales/Warehouse/Technician về sau.

---

## 3. Nghiên cứu nghiệp vụ và quy tắc vận hành

### 3.1. Những gì rút ra từ thực tế

Phong Vũ mô tả quy trình kiểm tra linh kiện, phần mềm và driver cho PC trước giao. Điều này là cơ sở tham khảo để PC Store có checklist chuẩn bị máy, thay vì chuyển đơn từ “đã trả tiền” sang “đã giao” trực tiếp. [Hướng dẫn mua hàng Phong Vũ](https://help.phongvu.vn/chinh-sach-ban-hang/huong-dan-mua-hang-online)

Phí giao hàng cần được thể hiện trước khi khách xác nhận thanh toán. PC Store sẽ lưu báo giá giao hàng cùng đơn, tránh thay đổi số tiền âm thầm sau đặt hàng. [Giá và hình thức thanh toán Phong Vũ](https://help.phongvu.vn/chinh-sach-chung/gia-ca-va-hinh-thuc-thanh-toan)

Chính sách GEARVN có bước kiểm tra và điều kiện tiếp nhận bảo hành. Từ đó, PC Store tách **khách yêu cầu bảo hành** khỏi **kết luận được bảo hành**, đồng thời lưu người kiểm tra, kết quả và phương án xử lý. [Chính sách bảo hành GEARVN](https://www.gearvn.com/pages/chinh-sach-bao-hanh)

Các quy tắc dưới đây là **chính sách thiết kế của đồ án**, không phải khẳng định mọi cửa hàng đều áp dụng như vậy.

### 3.2. Tài khoản và xác thực

**Email/mật khẩu**

- Email được trim, chuẩn hóa chữ thường theo chính sách hệ thống và kiểm tra duy nhất.
- Không tự bỏ dấu chấm hoặc phần `+suffix` của email.
- Mật khẩu được hash bằng BCrypt.
- Cho đăng nhập sau đăng ký; các thao tác xác nhận quyền sở hữu email dùng token có hạn.
- Có luồng quên mật khẩu; không gửi mật khẩu qua email.
- Không trả password hash trong DTO, log hoặc màn hình Admin.

**Điện thoại**

- Chuẩn hóa định dạng trước kiểm tra duy nhất.
- Đăng ký/đăng nhập bằng OTP là một luồng riêng, không chỉ là xác minh số điện thoại trong hồ sơ.
- OTP gắn với mục đích: đăng nhập, liên kết hoặc thay số.
- Mặc định OTP có hạn 5 phút, tối đa 5 lần thử, gửi lại sau ít nhất 60 giây; giới hạn thêm theo số điện thoại và IP.
- OTP chỉ dùng một lần.
- Thay số điện thoại yêu cầu xác thực lại phiên và xác minh số mới.
- Số điện thoại là thông tin liên hệ; `user_id` mới là khóa sở hữu dữ liệu.

**Google**

- Định danh tài khoản ngoài bằng cặp `issuer + subject`.
- Nếu Google trả email đã có trên một tài khoản khác, yêu cầu đăng nhập tài khoản hiện hữu trước khi liên kết.
- Không tự gộp tài khoản chỉ vì trùng email.
- Không cho gỡ phương thức đăng nhập cuối cùng.

**Phân quyền**

- Admin không tự nâng quyền qua form cập nhật hồ sơ.
- Không vô hiệu hóa hoặc xóa Admin hoạt động cuối cùng.
- Tài khoản bị khóa phải mất khả năng thực hiện thao tác được bảo vệ, kể cả đang có Session.

**Thông báo**

- Tài khoản chỉ có điện thoại không bị ép nhập email để hoàn tất đăng ký.
- Có email đã xác minh thì gửi email; mọi tài khoản đều có thông báo trong hệ thống.
- Giả lập OTP phải có nhãn môi trường và không được bật trên cấu hình production.

### 3.3. Catalog và cấu hình PC

Mỗi sản phẩm gồm:

- SKU, tên, slug, loại, hãng hoặc đơn vị lắp ráp.
- Giá bán, trạng thái kinh doanh, hình ảnh, mô tả.
- CPU, GPU, RAM, lưu trữ, mainboard, PSU, case, hệ điều hành.
- Thời hạn bảo hành.
- Khối lượng và kích thước **sau đóng gói**.
- Các thuộc tính phục vụ tư vấn và thông tin nguồn.

Nguyên tắc:

- Không sửa trực tiếp trường tồn kho trong form sản phẩm.
- Giá và nội dung có thể cập nhật; đơn cũ giữ snapshot.
- Thay đổi đáng kể cấu hình phần cứng phải tạo SKU mới.
- Không hiển thị “Windows bản quyền” nếu dữ liệu không xác nhận.
- Sản phẩm ngừng bán vẫn tồn tại để phục vụ lịch sử đơn và hậu mãi.
- Không dùng tên CPU/GPU dạng chuỗi để suy luận hiệu năng tự động.

### 3.4. Giỏ hàng Session

- Giỏ chứa `productId`, số lượng và phiên bản giỏ.
- Không có bảng giỏ hàng trong PostgreSQL.
- Redis lưu HTTP Session, không trở thành nguồn dữ liệu giá hoặc tồn kho.
- Số lượng phải là số nguyên dương; xóa sản phẩm là thao tác riêng.
- Thêm giỏ không giữ hàng.
- Giá và tình trạng bán được đọc lại khi xem giỏ và checkout.
- Đăng nhập giữ giỏ hiện tại; thay session ID để phòng session fixation.
- Logout hủy Session, bao gồm giỏ, để tránh lộ dữ liệu trên máy dùng chung.

**Sau tạo đơn:** loại khỏi giỏ đúng những dòng/phiên bản đã checkout. Nếu người dùng mở tab khác và thay giỏ trong lúc đặt hàng, không xóa mù toàn bộ dữ liệu mới.

### 3.5. Giá và checkout

Công thức bản đầu:

```text
Tổng tiền = Tổng(đơn giá snapshot × số lượng) + phí vận chuyển snapshot
```

- Dùng `BigDecimal` và `NUMERIC(19,0)` cho VND.
- Giá niêm yết là số tiền khách trả cho sản phẩm; chưa triển khai nghiệp vụ tính thuế riêng.
- Server tính mọi giá trị tiền.
- Checkout yêu cầu đăng nhập, giỏ hợp lệ, số điện thoại đã xác minh và địa chỉ nhận hàng.
- Số điện thoại liên hệ đơn hàng lấy từ tài khoản tại thời điểm đặt.
- Tên người nhận có thể khác chủ tài khoản.

**Preview**

- Không giữ hàng.
- Trả tổng tiền, lỗi từng dòng và thời hạn báo giá 10 phút.
- Gắn với người dùng, phiên bản giỏ và dữ liệu địa chỉ.

**Submit**

- Bắt buộc khóa chống gửi lặp; HTML form dùng hidden token, REST dùng `Idempotency-Key`.
- Cùng khóa/cùng nội dung trả lại cùng đơn.
- Cùng khóa/khác nội dung trả lỗi xung đột.
- Giá hoặc phí thay đổi so với preview thì yêu cầu khách xác nhận lại.
- Khóa tồn kho và kiểm tra lần cuối trong transaction.
- Lưu order, items, giữ hàng, payment intent và outbox cùng transaction.
- Xóa phần giỏ đã đặt sau commit.
- Lỗi gửi email không làm thất bại đơn đã tạo.

### 3.6. Tồn kho

Chốt định nghĩa:

```text
on_hand   = số máy còn trong kho và đạt điều kiện bán
reserved  = số máy đang giữ cho đơn
available = on_hand - reserved
```

| Sự kiện | Tác động |
|---|---|
| Nhập máy đạt kiểm tra | Tăng `on_hand` |
| Đặt đơn COD hoặc online | Tăng `reserved` |
| Online thanh toán thành công | Giữ reservation, bỏ thời hạn chờ thanh toán |
| Hủy đơn trước bàn giao | Giảm `reserved` |
| Đơn online hết hạn | Giải phóng reservation |
| Bàn giao vận chuyển | Giảm cả `on_hand` và `reserved` |
| Nhận hàng hoàn | Đưa vào kiểm tra; chưa tăng tồn có thể bán |
| Hàng hoàn đạt kiểm tra | Tăng lại `on_hand` |

Quy tắc kỹ thuật:

- PostgreSQL là nguồn sự thật.
- Khóa các dòng tồn kho theo thứ tự `productId` cố định.
- Kiểm tra `available >= requestedQuantity` trong transaction.
- Có ledger cho mọi biến động: số lượng, lý do, chứng từ, người thực hiện.
- Không dùng Redis lock thay cho transaction tồn kho.
- Không cho điều chỉnh tồn xuống dưới lượng đang giữ.
- Có kiểm tra đối chiếu số dư và ledger để phát hiện lệch.

### 3.7. Vòng đời đơn hàng

```text
Online:
AWAITING_PAYMENT → AWAITING_CONFIRMATION → CONFIRMED
→ PREPARING → READY_TO_SHIP → SHIPPED → DELIVERED

COD:
AWAITING_CONFIRMATION → CONFIRMED
→ PREPARING → READY_TO_SHIP → SHIPPED → DELIVERED
```

Nhánh ngoại lệ:

```text
AWAITING_PAYMENT → EXPIRED
Trước SHIPPED → CANCELLED, theo điều kiện
SHIPPED → RETURNING → RETURNED
```

Không dùng một enum duy nhất cho cả đơn, tiền và vận chuyển.

- Online giữ hàng 15 phút.
- COD cần Admin xác nhận trong 24 giờ; quá hạn thì hủy và trả hàng giữ.
- Customer tự hủy ở `AWAITING_PAYMENT` hoặc `AWAITING_CONFIRMATION`.
- Sau xác nhận, Customer gửi yêu cầu; Admin xử lý trước bàn giao.
- Đơn đã trả tiền và bị hủy phải sinh nghĩa vụ hoàn tiền.
- Đã giao không quay ngược về “đang chuẩn bị”; đổi trả/bảo hành là hồ sơ riêng.
- Mỗi chuyển trạng thái lưu actor, thời điểm, lý do và trạng thái trước/sau.

**Sửa số lượng theo yêu cầu đề tài**

Chỉ cho Admin sửa đơn **COD, chưa thanh toán, đang `AWAITING_CONFIRMATION`**:

1. Nhập lý do và ghi nhận khách đã đồng ý.
2. Khóa order và tồn kho.
3. Tăng/giảm reservation tương ứng.
4. Giữ đơn giá đã chốt; tính lại tổng và phí giao theo chính sách.
5. Ghi revision và thông báo khách.
6. Không cho toàn bộ đơn trở thành rỗng; dùng thao tác hủy đơn.

Đơn online không được sửa số lượng trực tiếp vì liên quan tham chiếu và số tiền thanh toán.

### 3.8. Thanh toán

**COD**

- Tạo đơn không đồng nghĩa đã thu tiền.
- Phân biệt chưa thu, đơn vị giao hàng đã thu và cửa hàng đã đối soát nhận tiền.
- Trạng thái giao thành công không tự chứng minh tiền đã về cửa hàng.
- Bản giả lập có thao tác ghi nhận thu và đối soát riêng.

**VNPAY sandbox**

- Mỗi payment attempt có mã tham chiếu duy nhất.
- Kiểm tra chữ ký, merchant, mã giao dịch, số tiền và kết quả.
- Return URL phục vụ hiển thị; IPN hoặc truy vấn server được xác minh mới cập nhật thanh toán.
- Theo giao thức PAY đã khảo sát, nhận IPN bằng GET và phản hồi đúng hợp đồng của VNPAY.
- Số tiền gửi sang gateway được nhân 100 theo tài liệu. [Tài liệu tích hợp VNPAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html)

**Các trường hợp phải xử lý**

| Tình huống | Quy tắc |
|---|---|
| Callback trùng | Ghi nhận một lần, không giữ/trừ hàng lần hai |
| Callback sai chữ ký hoặc số tiền | Không đổi trạng thái tài chính |
| Khách đóng trình duyệt | IPN vẫn xử lý bình thường |
| Return về trước IPN | Hiển thị đang xác nhận, truy vấn lại trạng thái |
| Hết hạn và callback chạy đồng thời | Cùng khóa order/payment để tuần tự hóa |
| Thanh toán thành công sau đơn hết hạn | Ghi nhận tiền thực tế; tạo hồ sơ đối soát/hoàn tiền, không tự phục hồi đơn |
| Attempt cũ chưa rõ kết quả | Query/reconcile trước khi mở attempt mới |
| Gateway không truy cập được | Không giả báo thành công; hiển thị trạng thái chờ và hướng xử lý |

Hoàn tiền trong bản nộp là **workflow có sổ theo dõi và mô phỏng sandbox**; không tuyên bố đã hoàn tiền thật.

### 3.9. Giao hàng

Bản nộp dùng `SimulatedShippingProvider` với kịch bản xác định:

- Báo phí.
- Tạo mã vận đơn.
- Nhận hàng.
- Giao thành công.
- Giao thất bại.
- Hoàn hàng.

Mặc định demo:

- Phí 50.000 VND/đơn, công khai là chính sách dữ liệu mẫu.
- Không chia đơn thành nhiều kiện/vận đơn.
- Trạng thái do Admin điều khiển qua màn hình mô phỏng có nhãn rõ ràng.
- Không cam kết ngày giao thực tế.

Dữ liệu địa chỉ lưu nội dung chuẩn hóa và mapping provider riêng; không gắn toàn bộ mô hình địa chỉ vào mã GHN.

Nếu triển khai GHN ở P2:

- Kiểm chứng lại API, cơ chế xác thực webhook, hạn mức COD và điều kiện nhận PC trước khi tích hợp.
- Khi tạo vận đơn timeout, tra cứu theo mã khách hàng trước khi retry để tránh tạo đôi.
- Callback chưa đủ cơ sở xác thực chỉ là tín hiệu để backend truy vấn lại provider.
- Không tự giả định GHN có cùng cơ chế chữ ký với VNPAY.

### 3.10. Serial, kiểm tra và bảo hành

**Hồ sơ máy**

- Mỗi máy vật lý có serial hoặc mã nội bộ duy nhất.
- Gắn với SKU, phiếu nhập, order item và lịch sử xuất/hoàn.
- Một order item số lượng 2 phải được phân bổ 2 máy khác nhau.
- Serial chỉ được gán cho một đơn đang hoạt động tại một thời điểm.

**Checklist trước giao**

- Đúng cấu hình.
- Kiểm tra ngoại hình.
- Khởi động thành công.
- Nhận đủ RAM và ổ lưu trữ.
- Kiểm tra driver.
- Kiểm tra tải cơ bản.
- Ghi nhận phụ kiện.
- Đóng gói.

Checklist có người kiểm tra và thời điểm. Chỉ chuyển `READY_TO_SHIP` khi đủ serial và checklist đạt.

**Hồ sơ bảo hành**

- Kích hoạt khi giao thành công.
- Chụp lại thời hạn và phiên bản chính sách tại thời điểm mua.
- Ngày hết hạn tính từ ngày giao theo số tháng đã chốt.
- Lưu báo lỗi, ảnh, lịch sử tiếp nhận và kết quả xử lý.
- Phiên bản đầu quản lý bảo hành toàn máy; chưa tự động phân xử điều kiện từng hãng linh kiện.

```text
REQUESTED → RECEIVED → DIAGNOSING
→ REPAIRING → READY_FOR_RETURN → CLOSED

DIAGNOSING → REJECTED → CLOSED
```

- Customer chỉ truy cập máy thuộc tài khoản mình.
- Không công khai lịch sử mua hoặc bảo hành chỉ bằng số điện thoại.
- QR dẫn tới trang yêu cầu đăng nhập và kiểm tra quyền.
- Hết thời hạn vẫn có thể tiếp nhận yêu cầu hỗ trợ nhưng phải ghi rõ ngoài bảo hành.
- Thay máy nguyên chiếc và hoàn tiền tự động nằm ngoài P1; không dùng trạng thái “đã sửa” để che nghiệp vụ thay máy chưa hỗ trợ.

---

## 4. Điểm khác biệt của sản phẩm

### 4.1. Tư vấn PC có giải thích

Đầu vào:

- Ngân sách tối đa.
- Một nhu cầu chính: văn phòng/học tập, lập trình, gaming, sáng tạo nội dung.
- RAM và lưu trữ tối thiểu nếu khách có yêu cầu.
- Yêu cầu hệ điều hành hoặc GPU rời.

Cơ chế phiên bản đầu:

1. Lọc sản phẩm đang bán, còn hàng, không vượt ngân sách.
2. Áp dụng điều kiện bắt buộc của hồ sơ nhu cầu.
3. Xếp hạng bằng điểm phù hợp do nhóm biên soạn có nguồn và phiên bản.
4. Ưu tiên điểm phù hợp cao hơn; bằng điểm thì giá thấp hơn; sau cùng SKU để kết quả ổn định.
5. Trả tối đa ba máy.

Mỗi gợi ý phải hiển thị:

- Lý do phù hợp.
- Điểm đánh đổi.
- Giá và phần ngân sách còn lại.
- Những dữ liệu chưa có.
- Nút so sánh và thêm giỏ.

**Không có kết quả:** thông báo rõ không có PC thỏa điều kiện; cho khách tự điều chỉnh ngân sách hoặc bộ lọc. Không âm thầm vượt ngân sách.

**Không tuyên bố:** điểm phù hợp là benchmark, FPS thực tế hoặc cam kết chạy tốt mọi phần mềm.

### 4.2. Quản trị dữ liệu tư vấn

Bảng đánh giá lưu:

- SKU và nhu cầu.
- Mức phù hợp từ 1–5.
- Lý do và hạn chế.
- URL nguồn, ngày tham khảo.
- Người biên soạn, người kiểm tra.
- Phiên bản bộ quy tắc.

Danh sách 20–30 cấu hình mẫu phải được một thành viên nhập và một thành viên kiểm tra. Dữ liệu thiếu bằng chứng không được sử dụng để đưa ra lời khẳng định hiệu năng.

### 4.3. So sánh và hồ sơ máy

- So sánh tối đa ba PC.
- Đồng nhất đơn vị RAM, lưu trữ và công suất.
- Làm nổi bật khác biệt cấu hình; không tô “mạnh hơn” khi chưa có căn cứ.
- Sau mua, khách xem serial, cấu hình đã mua, checklist, bảo hành và lịch sử hỗ trợ.

Giá trị thực tế nằm ở việc nối liền **chọn đúng máy → nhận đúng máy → có dữ liệu hậu mãi**.

---

## 5. Kiến trúc và công nghệ

### 5.1. Kiến trúc tổng thể

```text
Browser
  │
  ├── Trang HTML / Form / JavaScript
  ▼
Spring MVC Controllers + Thymeleaf
  │
  ├── REST Controllers cho AJAX và Web Services
  ▼
Application Services
  ▼
Domain Rules + Repositories + Provider Ports
  │
  ├── PostgreSQL
  ├── Redis Session
  ├── Google OIDC
  ├── VNPAY Sandbox
  ├── SMTP / Mailpit
  └── OTP và Shipping adapters
```

Controller MVC và REST gọi chung application service. Không để MVC controller gọi HTTP vào REST controller của chính ứng dụng.

### 5.2. Stack chốt

| Thành phần | Công nghệ | Vai trò |
|---|---|---|
| Ngôn ngữ | Java 21 | Backend |
| Framework | Spring Boot 4.1.1 | Cấu hình và runtime |
| Web | Spring MVC | Routing, binding, controller, REST |
| View | Thymeleaf theo BOM của Boot | Server-rendered HTML |
| UI | Bootstrap 5.3, CSS, JavaScript ES modules | Responsive, form, tương tác |
| Security | Spring Security | Session, RBAC, CSRF, BCrypt |
| Google | Spring OAuth2 Client | OIDC login |
| Validation | Jakarta Bean Validation | Validate DTO/form |
| Persistence | Spring Data JPA/Hibernate | Mapping và truy vấn |
| Database | PostgreSQL 18 | Dữ liệu giao dịch |
| Migration | Flyway | Lịch sử schema |
| Session | Spring Session Data Redis | Session đăng nhập và giỏ |
| Module checks | Spring Modulith 2.1.x, test scope | Kiểm tra ranh giới module |
| HTTP client | Spring RestClient | Tích hợp provider |
| Email | Spring Mail + Mailpit local | SMTP và demo email |
| Jobs | Spring scheduling + DB job records | Hết hạn, retry, đối soát |
| Theo dõi | Actuator, Micrometer, structured logs | Health và chỉ số vận hành |
| Kiểm thử | JUnit theo BOM, Mockito, MockMvc, Testcontainers | Unit/integration |
| E2E | Playwright | Luồng trình duyệt |
| Build | Maven Wrapper | Build lặp lại được |
| Vận hành | Docker Compose, Caddy, GitHub Actions | Local/demo, HTTPS, CI |

Tài liệu Spring hiện được kiểm tra công bố Boot 4.1.1 hỗ trợ Java 21. Dependency của Spring phải đi theo BOM thay vì tự pin từng thư viện. [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)

Thymeleaf là lựa chọn tích hợp với Spring MVC; phiên bản cụ thể dùng từ dependency management và xác minh bằng trang form/security thử nghiệm ở tuần đầu. [Thymeleaf documentation](https://www.thymeleaf.org/documentation)

Spring Modulith chỉ dùng phần kiểm tra cấu trúc và tài liệu hóa cần thiết; không cần đưa toàn bộ cơ chế runtime của thư viện vào đồ án. [Spring Modulith](https://docs.spring.io/spring-modulith/reference/)

### 5.3. Những công nghệ không đưa vào bản đầu

- React, Vite, TanStack Query.
- JWT cho browser.
- Microservices, Kafka, Kubernetes.
- Elasticsearch.
- LLM hoặc Spring AI cho bộ tư vấn.
- Nhiều cổng thanh toán.
- Redis product cache khi chưa có bằng chứng cần tối ưu.
- MapStruct nếu mapping thủ công vẫn ngắn và rõ.

### 5.4. Module và trách nhiệm

| Module | Sở hữu nghiệp vụ | Thành viên chủ trì hiện thực |
|---|---|---|
| `identity` | User, credentials, external identity, OTP, hồ sơ và phân quyền | Võ Văn Nhựt |
| `catalog` | Product, category, brand, specification và ảnh | Bùi Ngọc Bửu |
| `advisory` | Hồ sơ nhu cầu, đánh giá, gợi ý và so sánh | Huỳnh Đoàn Nhân |
| `inventory` | Số dư, ledger, reservation và máy vật lý | Bùi Ngọc Bửu |
| `cart` | Giỏ hàng trong HTTP Session | Huỳnh Đoàn Nhân |
| `sales` | Checkout, order, revision và điều phối giao dịch | Võ Văn Cảnh |
| `payment` | Attempts, receipts, events, reconciliation và refund cases | Võ Văn Cảnh |
| `fulfillment` | Phân bổ serial, checklist, shipment và hàng hoàn | Lê Thị Kim Ngân |
| `aftersales` | Warranty entitlement và service request | Lê Thị Kim Ngân |
| `notification` | In-app notification, outbox và email | Huỳnh Đoàn Nhân |
| `audit` | Nhật ký thay đổi nghiệp vụ | Võ Văn Nhựt |

“Chủ trì” nghĩa là chịu trách nhiệm trọn chiều dọc của module: thiết kế, code, migration thuộc module, giao diện liên quan, test, tài liệu và demo. Chi tiết phân bổ và ranh giới phối hợp nằm tại mục 8.1.

Quy tắc phụ thuộc:

- Module gọi public service/port của nhau; không truy cập repository nội bộ.
- `sales` điều phối checkout; không dồn nghiệp vụ vào `common`.
- Giao dịch quan trọng dùng lời gọi đồng bộ trong transaction.
- Thông báo dùng outbox được ghi cùng transaction.
- Tác vụ mạng không chạy khi đang giữ khóa tồn kho.
- Callback thanh toán chuyển sự kiện đã xác minh cho bộ điều phối xử lý order và inventory.

### 5.5. Cấu trúc repository

```text
pc-store/
├── pom.xml
├── mvnw / mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/iuh/fit/nhom16/pcstore/
│   │   │   ├── identity/
│   │   │   ├── catalog/
│   │   │   ├── advisory/
│   │   │   ├── inventory/
│   │   │   ├── cart/
│   │   │   ├── sales/
│   │   │   ├── payment/
│   │   │   ├── fulfillment/
│   │   │   ├── aftersales/
│   │   │   ├── notification/
│   │   │   ├── audit/
│   │   │   └── configuration/
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── fragments/
│   │       │   ├── storefront/
│   │       │   ├── account/
│   │       │   └── admin/
│   │       ├── static/
│   │       └── db/migration/
│   └── test/
├── e2e/
├── docs/
├── ops/
├── compose.yaml
├── .env.example
└── .github/workflows/
```

Trong module: `api`, `application`, `domain`, `infrastructure`, `web`. Chỉ tạo lớp khi có trách nhiệm thực tế; không tạo interface cho mọi service theo thói quen.

---

## 6. Dữ liệu và giao diện hệ thống

### 6.1. Nhóm bảng

| Nhóm | Bảng chính |
|---|---|
| Identity | `users`, `user_roles`, `external_identities`, `user_addresses`, `verification_tokens` |
| Catalog | `products`, `categories`, `brands`, `product_specs`, `product_images` |
| Advisory | `usage_profiles`, `product_suitability`, `advisory_rule_versions` |
| Inventory | `inventory_balances`, `inventory_movements`, `stock_reservations`, `product_units` |
| Sales | `orders`, `order_items`, `order_revisions`, `order_status_history`, `idempotency_records` |
| Payment | `payment_attempts`, `payment_receipts`, `payment_events`, `refund_cases` |
| Fulfillment | `unit_allocations`, `inspection_records`, `shipments`, `shipment_events` |
| Aftersales | `warranty_entitlements`, `service_requests`, `service_request_events` |
| Platform | `outbox_events`, `notifications`, `audit_logs` |

### 6.2. Quy ước dữ liệu

- ID nghiệp vụ nội bộ dùng UUID.
- Mã đơn hiển thị duy nhất; không thay thế kiểm tra quyền.
- Thời gian lưu UTC, hiển thị múi giờ Việt Nam.
- Order snapshot tên, SKU, cấu hình, giá, chính sách bảo hành, điện thoại và địa chỉ.
- Đơn cũ không phụ thuộc giá hoặc hồ sơ hiện tại.
- Số dư kho cập nhật qua service chuyên trách.
- FK không cascade xóa lịch sử đơn hàng.
- Trường cần lọc thường xuyên dùng cột có kiểu rõ ràng; JSONB dành cho snapshot và payload đã lọc nhạy cảm.
- Không dùng stored procedure, function hoặc CHECK để thực hiện validation nghiệp vụ theo ràng buộc đề.
- Dùng PK, FK, UNIQUE, NOT NULL và index bảo vệ toàn vẹn.
- Production/demo bền vững dùng `ddl-auto=validate`.

### 6.3. Quan hệ quan trọng

```text
User 1—N Order 1—N OrderItem N—1 Product
Product 1—N ProductUnit
OrderItem 1—N UnitAllocation N—1 ProductUnit
Order 1—N PaymentAttempt
Order 1—N StockReservation
Order 1—0..1 Shipment
ProductUnit 1—N WarrantyEntitlement
WarrantyEntitlement 1—N ServiceRequest
```

Phân bổ máy và bảo hành giữ lịch sử, không ghi đè mất quan hệ khi có hàng hoàn.

### 6.4. Hợp đồng API tối thiểu

Trang Thymeleaf dùng MVC form. REST phục vụ AJAX, Web Services và tích hợp.

| Nhóm | Endpoint tiêu biểu |
|---|---|
| Catalog | `GET /api/v1/products`, `GET /api/v1/products/{id}` |
| Advisory | `POST /api/v1/recommendations`, `GET /api/v1/comparisons` |
| Cart | `GET /api/v1/cart`, `POST /api/v1/cart/items`, `PATCH/DELETE /api/v1/cart/items/{id}` |
| Checkout | `POST /api/v1/checkout/preview`, `POST /api/v1/orders` |
| Orders | `GET /api/v1/me/orders`, `GET /api/v1/me/orders/{code}`, `POST .../{code}/cancel` |
| OTP | `POST /api/v1/auth/otp/challenges`, `POST /api/v1/auth/otp/verifications` |
| VNPAY | `GET /integrations/vnpay/ipn`, `GET /payments/vnpay/return` |
| Warranty | `GET /api/v1/me/devices`, `POST /api/v1/me/service-requests` |

Hợp đồng checkout:

```text
CheckoutCommand
- quoteId
- cartVersion
- paymentMethod
- recipientName
- addressId hoặc địa chỉ mới
- customerNote
- idempotencyKey
```

```text
CheckoutResult
- orderCode
- orderStatus
- paymentStatus
- total
- paymentRedirectUrl nếu có
```

Client không gửi giá có thẩm quyền, chủ sở hữu đơn hoặc vai trò.

Các port cần tách:

```text
OtpProvider
PaymentGateway
ShippingProvider
MediaStorage
MailSender
```

Adapter giả lập và adapter thật phải cùng contract, có bộ test chung cho hành vi cơ bản.

### 6.5. Lỗi và bảo mật

- REST trả `ProblemDetail` kèm `errorCode`, `fieldErrors`, `traceId`.
- MVC hiện lỗi trên form, giữ dữ liệu hợp lệ đã nhập.
- Sau POST thành công dùng Post/Redirect/Get.
- Kiểm tra quyền ở endpoint và application service.
- Giữ CSRF cho form và API dùng cookie; chỉ loại trừ endpoint tích hợp cần thiết.
- Không đưa exception stack trace ra khách.
- Render nội dung người dùng bằng escaping mặc định của Thymeleaf.
- Ảnh upload giới hạn 5 MB, kiểm tra định dạng thực, đặt tên ngẫu nhiên, không cho SVG ở bản đầu.
- Cookie `HttpOnly`, `Secure` trên HTTPS, `SameSite=Lax`.
- Không log OTP, mật khẩu, session cookie hoặc secret.

Spring Security bảo vệ CSRF cho các phương thức thay đổi dữ liệu; đây là cấu hình phải giữ khi dùng Session. [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

### 6.6. Màn hình

**Storefront:** trang chủ, catalog, chi tiết, tư vấn, kết quả tư vấn, so sánh, giỏ.

**Tài khoản:** đăng ký, login, OTP, quên mật khẩu, hồ sơ, địa chỉ, liên kết phương thức đăng nhập.

**Mua hàng:** checkout, kết quả thanh toán, danh sách đơn, chi tiết đơn.

**Hậu mãi:** máy đã mua, hồ sơ máy, tạo yêu cầu, theo dõi yêu cầu.

**Admin:** dashboard, catalog, kho, serial, đơn, checklist, vận đơn, đối soát, tài khoản, bảo hành, quy tắc tư vấn, audit.

Mỗi màn hình phải thiết kế đủ trạng thái rỗng, lỗi, dữ liệu không hợp lệ, hết quyền và thao tác thành công.

---

## 7. Kế hoạch thực hiện 14 tuần

Tuần dưới đây là **tuần dự án**, cần ánh xạ sang lịch môn học. Mỗi tuần phải có phần chạy được hoặc tài liệu có thể review.

| Tuần | Công việc chính | Điều kiện hoàn thành |
|---|---|---|
| **1** | Chốt phạm vi, ma trận yêu cầu, stack spike, đăng ký Google/VNPAY, wireframe sơ bộ | Boot + Thymeleaf + Security + DB chạy; ghi rõ provider đã có/thiếu |
| **2** | Use case, ERD, trạng thái đơn/tiền/kho, UI flow, Maven/Compose/CI/Flyway | CI xanh; migration chạy DB rỗng; review được các luồng chính |
| **3** | Email auth, OTP adapter, Google, profile, CSRF, Session | Ba phương thức login; kiểm thử tài khoản trùng và phân quyền |
| **4** | Catalog, thông số, ảnh, category/brand, search/filter; giỏ Session | Guest duyệt và thêm giỏ; Admin CRUD đúng ràng buộc |
| **5** | Nhập kho, ledger, reservation, checkout preview, tạo đơn COD | Luồng mua COD chạy xuyên suốt; không oversell |
| **6** | Admin xác nhận/sửa lượng/hủy, customer orders, snapshot, outbox | P0 cơ bản hoàn thành; có email/thông báo và audit |
| **7** | VNPAY sandbox, IPN, expiry, idempotency, late payment | Test callback trùng/sai/đến muộn; trả hàng giữ chính xác |
| **8** | Serial, allocation, checklist, shipment giả lập, hàng hoàn | Chỉ xuất khi đủ serial và kiểm tra; ledger đúng khi giao/hoàn |
| **9** | Hồ sơ nhu cầu, dữ liệu tư vấn, xếp hạng, giải thích, so sánh | Có bộ dữ liệu được review và kết quả xác định |
| **10** | Kích hoạt bảo hành, hồ sơ máy, QR có kiểm soát, service request | Customer theo dõi một yêu cầu bảo hành hoàn chỉnh |
| **11** | E2E, kiểm thử phân quyền/cạnh tranh, dữ liệu demo, chỉnh UX | Các luồng P0/P1 pass; không còn lỗi nghiêm trọng |
| **12** | Triển khai demo, HTTPS, backup/restore, load smoke, tài liệu vận hành | Người khác dựng được; phục hồi DB và ảnh thành công |
| **13** | Báo cáo, sơ đồ cuối, bảng truy vết, diễn tập và regression | Chứng cứ cho từng chức năng và từng thành viên |
| **14** | Đóng băng tính năng, sửa lỗi cuối, release, đóng gói nộp | Tag final; source, báo cáo, hướng dẫn và kịch bản demo thống nhất |

### 7.1. Đường găng

```text
Identity + Catalog
→ Cart + Inventory
→ Checkout COD
→ Order lifecycle
→ Payment
→ Fulfillment + Serial
→ Warranty
→ Regression + Release
```

Advisory triển khai song song sau khi catalog và bộ thông số ổn định.

### 7.2. Nếu chỉ còn 10 tuần

- Gộp tuần 1–2.
- Làm identity và catalog song song.
- Gộp phần hoàn thiện COD và quản trị đơn.
- Advisory và warranty giao cho hai luồng khác nhau.
- Hai tuần cuối dành riêng cho kiểm thử, báo cáo và release.
- Giữ giao hàng/OTP giả lập; bỏ P2, biểu đồ dashboard và tùy biến giao diện không cần thiết.
- Không cắt kiểm thử tồn kho, phân quyền, thanh toán hoặc yêu cầu bắt buộc.

### 7.3. Cấu trúc mỗi task

Mỗi ticket phải có:

```text
Mã yêu cầu → mục tiêu → đầu vào/đầu ra
→ quy tắc nghiệp vụ → người làm/người review
→ phụ thuộc → tiêu chí nghiệm thu → bằng chứng kiểm thử
```

Một task nên hoàn thành trong 0,5–2 ngày làm việc tập trung. Task lớn hơn phải tách theo kết quả người dùng nhìn thấy.

---

## 8. Phân công và quy trình phối hợp

### 8.1. Phân công theo thành viên đã đăng ký

Theo nhận xét của giảng viên, phần “phụ trách” được chi tiết hóa thành đầu ra có thể kiểm tra. Mỗi thành viên phải trực tiếp hiện thực module được giao, không chỉ phân tích, thiết kế giao diện, kiểm thử hoặc viết báo cáo.

#### 8.1.1. Nguyên tắc phân chia

- Mỗi module có đúng một người chủ trì để tránh bỏ sót hoặc đùn đẩy trách nhiệm.
- Người chủ trì sở hữu trọn luồng `api` → `application` → `domain` → `infrastructure` → `web`, cùng migration, template/static asset, test, tài liệu và demo tương ứng.
- Số module không chia đều máy móc. Module có concurrency, security, payment hoặc nhiều trạng thái được tính nặng hơn; các module nhỏ hoặc không có database được ghép thành một gói rộng hơn.
- Vai trò điều phối CI, migration, UI/UX, E2E hoặc báo cáo không có nghĩa người điều phối làm thay phần của thành viên khác.
- Mỗi thành viên đều phải có đóng góp nhìn thấy được ở backend, dữ liệu hoặc tích hợp, giao diện, kiểm thử và tài liệu.
- Công việc liên module phải tách ticket theo contract và đầu ra của từng owner; không dùng một ticket chung để che khuất đóng góp cá nhân.

#### 8.1.2. Bảng phân công tổng quan

| Thành viên | Module chủ trì | Trách nhiệm xuyên suốt | Người review chính |
|---|---|---|---|
| **Võ Văn Cảnh** | `sales`, `payment` | Kiến trúc nền tảng, checkout/payment, VNPAY, cấu hình tích hợp và CI | Bùi Ngọc Bửu |
| **Bùi Ngọc Bửu** | `catalog`, `inventory` | Mô hình dữ liệu sản phẩm/kho, Flyway, PostgreSQL, media và dữ liệu demo | Lê Thị Kim Ngân |
| **Huỳnh Đoàn Nhân** | `cart`, `advisory`, `notification` | Thymeleaf storefront, UI/UX chung, Redis cart, outbox/email | Võ Văn Nhựt |
| **Võ Văn Nhựt** | `identity`, `audit` | Spring Security, RBAC, CSRF, OTP/Google, hồ sơ và audit | Võ Văn Cảnh |
| **Lê Thị Kim Ngân** | `fulfillment`, `aftersales` | Serial allocation, checklist, shipment, warranty, E2E và báo cáo | Huỳnh Đoàn Nhân |

Vòng review chính là: **Bửu review Cảnh → Ngân review Bửu → Nhựt review Nhân → Cảnh review Nhựt → Nhân review Ngân**. Như vậy mỗi người vừa có một reviewer chính, vừa chịu trách nhiệm review chính cho một thành viên khác.

#### 8.1.3. Võ Văn Cảnh — `sales`, `payment` và nền tảng tích hợp

**Hiện thực module**

- `sales`: checkout preview, tạo đơn idempotent, order item snapshot, sửa số lượng COD theo điều kiện, hủy đơn, lịch sử trạng thái và điều phối transaction với inventory/payment/notification.
- `payment`: payment intent/attempt, receipt, payment event, expiry, callback hợp lệ/trùng/muộn, đối soát và hồ sơ refund cần xử lý thủ công.
- Xác định public contract của `sales` và `payment`; không để controller gọi trực tiếp repository hoặc gọi HTTP nội bộ giữa MVC và REST.
- Chịu trách nhiệm các quy tắc tổng tiền, phí vận chuyển snapshot, idempotency, late payment và trạng thái order/payment tương thích nhau.

**Database và migration**

- Sở hữu schema và migration cho `orders`, `order_items`, `order_revisions`, `order_status_history`, `idempotency_records`.
- Sở hữu schema và migration cho `payment_attempts`, `payment_receipts`, `payment_events`, `refund_cases`.
- Chịu trách nhiệm index theo order code, customer, trạng thái, expiry và provider transaction; giữ snapshot đơn hàng độc lập với dữ liệu hiện tại.

**Template, UI/UX và API**

- Hiện thực `templates/account/checkout/`, `templates/account/orders/`, `templates/admin/orders/`, `templates/admin/payments/` và màn hình kết quả/return thanh toán.
- Xử lý đầy đủ trạng thái quote hết hạn, giá thay đổi, thiếu tồn, submit lặp, thanh toán chờ/không thành công và đơn không có quyền truy cập.
- Hiện thực MVC form và REST contract cho checkout, order, cancellation, VNPAY IPN/return; client không được gửi giá hoặc vai trò có thẩm quyền.

**Tích hợp và nền tảng**

- Sở hữu port `PaymentGateway`, adapter VNPAY sandbox, xác minh chữ ký và cấu hình callback theo môi trường.
- Bootstrap Maven, Docker Compose, profile môi trường, Actuator cơ bản và GitHub Actions; phối hợp nhưng không viết thay cấu hình riêng của module khác.
- Bảo đảm tác vụ mạng không chạy trong lúc giữ khóa kho và lỗi provider không phá transaction đã commit.

**Kiểm thử, tài liệu và demo**

- Test checkout transaction, idempotency, sửa/hủy COD, payment callback sai/trùng/muộn, expiry race và return trước IPN.
- Viết contract checkout/payment, ADR điều phối transaction, hướng dẫn cấu hình VNPAY và bằng chứng CI.
- Demo luồng COD và VNPAY từ checkout đến trạng thái đơn/thanh toán; giải thích được rollback và xử lý callback lặp.

#### 8.1.4. Bùi Ngọc Bửu — `catalog`, `inventory` và nền tảng dữ liệu

**Hiện thực module**

- `catalog`: product, category, brand, specification, image, trạng thái kinh doanh, search/filter và ràng buộc ngừng bán/xóa.
- `inventory`: on-hand/reserved/available, inventory movement, reservation, expiry/release và `product_units` đại diện máy vật lý/serial trong kho.
- Hiện thực khóa tồn kho, optimistic/pessimistic control phù hợp, ledger bất biến và quy tắc không sửa tồn trực tiếp từ form sản phẩm.
- Phân định rõ: Bửu sở hữu máy vật lý và trạng thái kho; Ngân sở hữu phân bổ máy cho order, checklist, shipment và luồng hàng hoàn.

**Database và migration**

- Sở hữu schema và migration cho `products`, `categories`, `brands`, `product_specs`, `product_images`.
- Sở hữu schema và migration cho `inventory_balances`, `inventory_movements`, `stock_reservations`, `product_units`.
- Chịu trách nhiệm quy ước UUID, `NUMERIC(19,0)`, index/filter catalog, unique SKU/serial, khóa ngoại và kiểm tra `ddl-auto=validate`.
- Điều phối thứ tự version Flyway và review migration liên module; mỗi owner vẫn phải tự viết migration của module mình.

**Template, UI/UX và dữ liệu**

- Hiện thực `templates/storefront/catalog/`, `templates/admin/catalog/`, `templates/admin/inventory/`, `templates/admin/units/` cùng JavaScript đặc thù của các màn hình này.
- Xử lý catalog rỗng, không có kết quả lọc, sản phẩm ngừng bán, upload ảnh lỗi, thiếu hàng và xung đột cập nhật kho.
- Chuẩn bị bộ 20–30 SKU, ít nhất 40 máy vật lý, nguồn/ngày tham khảo cấu hình và dữ liệu kho ở nhiều trạng thái.

**Tích hợp và hạ tầng dữ liệu**

- Sở hữu port `MediaStorage`, adapter lưu file local, kiểm tra MIME/kích thước/tên file và đường dẫn media có thể chuyển sang cloud.
- Duy trì ERD, data dictionary, migration strategy và dữ liệu seed/demo; hỗ trợ chuẩn PostgreSQL/Testcontainers cho cả nhóm.

**Kiểm thử, tài liệu và demo**

- Test CRUD/ràng buộc catalog, dữ liệu đang được order tham chiếu, serial trùng, ledger, reservation và hai khách tranh sản phẩm cuối.
- Test rollback không làm lệch tồn, release/hủy idempotent và hàng hoàn chưa kiểm tra không quay lại tồn bán được.
- Demo nhập kho, thay đổi số dư qua ledger, reservation và truy vết một máy vật lý từ SKU đến trạng thái kho.

#### 8.1.5. Huỳnh Đoàn Nhân — `cart`, `advisory`, `notification` và UI/UX chung

**Hiện thực module**

- `cart`: session cart, cart version, thêm/sửa/xóa riêng từng dòng, giữ giỏ khi login, kiểm tra lại giá/tình trạng bán và chỉ xóa phần đã checkout.
- `advisory`: usage profile, suitability data, rule version, xếp hạng xác định, giải thích lý do gợi ý và so sánh PC.
- `notification`: in-app notification, outbox dispatcher, retry, trạng thái gửi email và luồng không rollback đơn hàng khi gửi thất bại.
- Cart không có bảng PostgreSQL; dữ liệu giỏ nằm trong Redis-backed HTTP Session và không trở thành nguồn giá/tồn kho.

**Database và dữ liệu tư vấn**

- Sở hữu schema và migration cho `usage_profiles`, `product_suitability`, `advisory_rule_versions`.
- Sở hữu schema và migration cho `outbox_events`, `notifications`; payload phải loại dữ liệu nhạy cảm và hỗ trợ retry/idempotency.
- Chuẩn bị bốn nhóm nhu cầu, thang điểm, lý do giải thích và bộ dữ liệu advisory có version/người duyệt.

**Template, UI/UX và frontend**

- Hiện thực `templates/fragments/`, `templates/storefront/home/`, `templates/storefront/advisory/`, `templates/storefront/comparison/`, `templates/storefront/cart/`, `templates/admin/advisory/` và `templates/mail/`.
- Xây dựng layout chung, navigation, Bootstrap theme, responsive behavior, accessibility cơ bản và quy ước CSS/JavaScript ES modules.
- Thiết kế trạng thái loading, empty, error, validation, success và no-permission nhất quán; review tính nhất quán UI của template do các thành viên khác sở hữu.
- Hiện thực trung tâm/fragment thông báo trong ứng dụng và badge chưa đọc mà không làm lộ dữ liệu người dùng khác.

**Tích hợp**

- Sở hữu port `MailSender`, adapter SMTP/Mailpit và template email; không gửi email trực tiếp trong transaction nghiệp vụ.
- Phối hợp với Cảnh về Redis/Compose: Cảnh cấu hình nền tảng, Nhân chịu trách nhiệm hành vi session cart và kiểm thử nhiều tab/login/logout.

**Kiểm thử, tài liệu và demo**

- Test số lượng âm, sản phẩm ngừng bán, giá đổi, hai tab sửa giỏ, login/logout và partial clear sau checkout.
- Test advisory không vượt ngân sách, không gợi ý hàng hết, dữ liệu thiếu, thứ tự ổn định và giải thích đúng.
- Test outbox retry/app dừng sau commit/email lỗi; viết wireframe, UI flow, style guide và tài liệu dữ liệu tư vấn.
- Demo luồng guest từ tư vấn → so sánh → giỏ, cùng một trường hợp outbox/email được retry.

#### 8.1.6. Võ Văn Nhựt — `identity`, `audit` và bảo mật

**Hiện thực module**

- `identity`: user, role, credential, external identity, address, verification token, email/password, OTP, Google linking, quên mật khẩu, profile và khóa tài khoản.
- `audit`: ghi nhận ai làm gì, thời điểm, đối tượng, before/after đã lọc nhạy cảm; hỗ trợ tra cứu có phân quyền.
- Hiện thực kiểm tra quyền ở endpoint và application service; khóa user phải vô hiệu thao tác được bảo vệ kể cả session đã tồn tại.
- Chịu trách nhiệm quy tắc không tự gộp Google theo email, không gỡ phương thức đăng nhập cuối và không vô hiệu hóa Admin hoạt động cuối cùng.

**Database và migration**

- Sở hữu schema và migration cho `users`, `user_roles`, `external_identities`, `user_addresses`, `verification_tokens`.
- Sở hữu schema và migration cho `audit_logs`; xác định index phục vụ tra cứu actor, action, target và thời gian.
- Bảo đảm password hash, OTP, token, session identifier và secret không xuất hiện trong DTO, audit payload hoặc log.

**Template, UI/UX và bảo mật web**

- Hiện thực `templates/account/auth/`, `templates/account/profile/`, `templates/account/addresses/`, `templates/admin/users/`, `templates/admin/audit/` và các trang lỗi xác thực/phân quyền trong `templates/errors/`.
- Hiện thực register/login, OTP challenge, quên/đặt lại mật khẩu, liên kết Google, đổi số điện thoại, quản lý địa chỉ và khóa/mở user.
- Cấu hình Spring Security, RBAC, CSRF, session fixation protection, cookie policy, password encoder và authorization rule cho MVC/REST.

**Tích hợp**

- Sở hữu port `OtpProvider`, adapter OTP giả lập có nhãn môi trường và contract cho provider thật.
- Sở hữu Google OIDC client, callback, liên kết tài khoản có xác thực và cấu hình secret theo biến môi trường.

**Kiểm thử, tài liệu và demo**

- Test email/phone trùng, OTP hết hạn/dùng lại/sai mục đích, rate limit, Google trùng email, đổi số và phương thức đăng nhập cuối.
- Test RBAC, CSRF, xem dữ liệu người khác, khóa user đang đăng nhập, Admin cuối cùng và lọc dữ liệu nhạy cảm trong audit.
- Viết auth flow, ma trận quyền, threat/security checklist và hướng dẫn cấu hình Google/OTP.
- Demo ba phương thức đăng nhập, bước xác minh điện thoại trước checkout và truy vết một thao tác Admin trong audit.

#### 8.1.7. Lê Thị Kim Ngân — `fulfillment`, `aftersales` và chất lượng đầu cuối

**Hiện thực module**

- `fulfillment`: unit allocation, kiểm tra serial, inspection checklist, shipment, shipment event, bàn giao vận chuyển và tiếp nhận hàng hoàn.
- `aftersales`: warranty entitlement, hồ sơ máy khách đã nhận, QR có kiểm soát, service request, kiểm tra điều kiện và lịch sử xử lý.
- Chịu trách nhiệm state transition fulfillment/warranty; chỉ cho giao khi đủ serial/checklist và chỉ kích hoạt bảo hành khi giao thành công.
- Phân định rõ: Ngân gọi public contract để cập nhật order/inventory; không sửa trực tiếp repository của `sales` hoặc `inventory`.

**Database và migration**

- Sở hữu schema và migration cho `unit_allocations`, `inspection_records`, `shipments`, `shipment_events`.
- Sở hữu schema và migration cho `warranty_entitlements`, `service_requests`, `service_request_events`.
- Bảo toàn lịch sử phân bổ, giao hàng và bảo hành; không ghi đè quan hệ cũ khi hoàn hàng hoặc thay máy.

**Template, UI/UX và vận hành**

- Hiện thực `templates/account/devices/`, `templates/account/service-requests/`, `templates/admin/dashboard/`, `templates/admin/inspections/`, `templates/admin/shipments/`, `templates/admin/aftersales/`.
- Hiện thực màn hình phân bổ serial/checklist qua luồng fulfillment, theo dõi shipment, hồ sơ máy, tạo/theo dõi service request và xử lý bảo hành.
- Thiết kế trạng thái thiếu serial, checklist chưa đạt, event giao hàng trùng, hàng hoàn cách ly, hết bảo hành và chuyển trạng thái không hợp lệ.

**Tích hợp và E2E**

- Sở hữu port `ShippingProvider`, adapter giao hàng giả lập và contract cho provider thật; callback/event phải idempotent.
- Điều phối Playwright, dữ liệu/kịch bản E2E và test report; từng owner vẫn phải viết hoặc hỗ trợ scenario thuộc module mình.
- Điều phối báo cáo và kịch bản bảo vệ từ artifact đã có; không viết thay tài liệu kỹ thuật của module khác.

**Kiểm thử, tài liệu và demo**

- Test serial trùng/thiếu, phân bổ một order item nhiều máy, chưa checklist vẫn giao, shipment event trùng và hàng hoàn chưa đạt kiểm tra.
- Test quyền sở hữu thiết bị, QR khó đoán, thời hạn bảo hành, service request trùng và state transition sai.
- Duy trì E2E matrix, test evidence, runbook demo và bảng truy vết yêu cầu → module → màn hình/API → test → người phụ trách.
- Demo luồng Admin phân bổ máy → kiểm tra → giao; Customer xem đúng serial → gửi bảo hành; Admin xử lý và đóng yêu cầu.

#### 8.1.8. Ranh giới phối hợp liên module

| Luồng | Owner điều phối | Trách nhiệm phối hợp |
|---|---|---|
| Checkout và giữ tồn | Cảnh (`sales`) | Bửu cung cấp contract khóa/giữ/giải phóng tồn; Nhân cung cấp cart snapshot; Nhựt bảo vệ identity/authorization |
| Thanh toán và thông báo | Cảnh (`payment`) | Cảnh xác minh provider event; Nhân ghi/dispatch outbox và notification; không gọi SMTP trong payment transaction |
| Xuất kho và giao hàng | Ngân (`fulfillment`) | Bửu quản lý product unit/ledger; Cảnh quản lý order transition; Ngân quản lý allocation/checklist/shipment |
| Hàng hoàn | Ngân (`fulfillment`) | Ngân tiếp nhận và inspection; Bửu chỉ tăng lại `on_hand` sau kết quả đạt; Cảnh cập nhật trạng thái order khi cần |
| Kích hoạt bảo hành | Ngân (`aftersales`) | Ngân tạo entitlement từ unit đã giao; Bửu cung cấp thông tin product unit; Cảnh cung cấp order snapshot |
| Audit thao tác Admin | Nhựt (`audit`) | Mỗi module phát thông tin audit đã lọc; Nhựt lưu, bảo vệ và cung cấp màn hình tra cứu |

#### 8.1.9. Kiểm soát cân bằng đóng góp

- Năm gói công việc được cân theo độ khó, không theo số module: `sales/payment`, `catalog/inventory`, `cart/advisory/notification`, `identity/audit`, `fulfillment/aftersales`.
- Mỗi gói đều có nghiệp vụ P0/P1, database hoặc state storage, MVC/REST, template/UI, integration, test và tài liệu/demo.
- `cart` không có PostgreSQL và `audit` nhỏ hơn các module giao dịch, nên được ghép lần lượt với advisory/notification và identity/security.
- Cuối tuần 2, 6 và 10, nhóm ước lượng lại ticket theo độ phức tạp và thời gian; mục tiêu mỗi thành viên giữ khoảng 18–22% tổng effort còn lại.
- Nếu một người vượt ngưỡng, chuyển một feature trọn chiều dọc hoặc một nhóm test/UI có acceptance criteria rõ ràng; không chia đôi entity/repository của một module cho hai người cùng sở hữu.
- Đóng góp được đánh giá bằng feature hoàn thành, chất lượng PR/review, migration, test, tài liệu và khả năng demo/giải thích; số commit hoặc số dòng code không phải thước đo chính.

### 8.2. Git và review

- `main` luôn build được.
- Feature branch ngắn theo ticket.
- Một PR cần ít nhất một người review và CI pass; tác giả không tự approve PR của mình.
- PR thay đổi module phải có owner hoặc reviewer chính của module tham gia review; PR liên module cần review từ các owner bị ảnh hưởng.
- Không chờ hoàn thành cả module mới tích hợp.
- Migration thuộc trách nhiệm owner của module; Bửu điều phối version và hỗ trợ review migration liên module. Migration đã merge không sửa lại; tạo migration tiếp theo.
- Nhân điều phối tính nhất quán UI/UX, Ngân điều phối E2E/báo cáo và Cảnh điều phối CI/tích hợp; mỗi thành viên vẫn tự hoàn thành UI, test, tài liệu và sửa build của phần mình.
- Mỗi tuần có demo tích hợp và cập nhật rủi ro.
- Đánh giá đóng góp bằng chức năng, PR, test, tài liệu và khả năng trình bày; không chỉ đếm commit.

### 8.3. Definition of Done

Một chức năng hoàn thành khi:

- Đạt acceptance criteria.
- UI và backend hoạt động cùng nhau.
- Validation, phân quyền và trạng thái lỗi được xử lý.
- Migration và tài liệu liên quan được cập nhật.
- Test phù hợp pass.
- Không có secret trong Git.
- PR được review.
- Chạy được trên nhánh chung.
- Người phụ trách giải thích được dữ liệu và luồng xử lý.

---

## 9. Kiểm thử, triển khai và bàn giao

### 9.1. Kiểm thử bắt buộc

| Nhóm | Kịch bản quan trọng |
|---|---|
| Identity | Email/phone trùng; OTP hết hạn, dùng lại, sai mục đích; Google trùng email; khóa user đang đăng nhập |
| Cart | Giữ giỏ khi login; lượng âm; sản phẩm ngừng bán; giá đổi; hai tab thay đổi giỏ |
| Checkout | Guest bị chặn; thiếu điện thoại; preview hết hạn; submit lặp; cùng key khác payload |
| Inventory | Hai khách tranh máy cuối; tăng số lượng thiếu hàng; hủy hai lần; rollback không lệch ledger |
| Payment | Chữ ký sai; tiền sai; callback trùng; callback muộn; expiry race; return trước IPN |
| Orders | Xem đơn người khác; sửa đơn paid bị chặn; sửa COD đúng tồn/tiền; snapshot không đổi |
| Fulfillment | Serial trùng; chưa checklist vẫn xuất; ship event trùng; hàng hoàn chưa đạt không bán lại |
| Advisory | Không vượt ngân sách; không gợi ý hàng hết; dữ liệu thiếu; thứ tự ổn định; giải thích đúng |
| Warranty | Người khác đoán QR; một dòng nhiều máy; hết hạn; yêu cầu trùng; chuyển trạng thái sai |
| Reliability | Email lỗi không rollback đơn; app dừng sau commit; outbox retry; phục hồi backup |

Dùng PostgreSQL Testcontainers cho kiểm thử transaction; không dùng H2 để kết luận tính đúng của khóa và concurrency.

E2E tối thiểu:

1. Guest → tư vấn → so sánh → giỏ.
2. Email login → xác minh điện thoại → COD.
3. Google login → bổ sung điện thoại → VNPAY sandbox.
4. Admin sửa lượng COD → xác nhận → phân bổ máy → giao.
5. Customer mở hồ sơ máy → tạo bảo hành.
6. Admin tiếp nhận → xử lý → đóng yêu cầu.
7. Admin thử xóa dữ liệu đang được đơn tham chiếu và nhận lỗi đúng.

### 9.2. Dữ liệu demo

- 20–30 SKU PC.
- Bốn nhóm nhu cầu tư vấn.
- Ít nhất 40 máy vật lý có mã riêng.
- Customer bằng email, điện thoại và Google.
- Đơn ở các trạng thái chờ, đã trả tiền, hết hạn, đang giao, hoàn.
- Một trường hợp thanh toán muộn cần đối soát.
- Một hàng hoàn đang cách ly kiểm tra.
- Một máy còn bảo hành và một máy hết hạn.

Dữ liệu giá/cấu hình phải ghi ngày tham khảo; tài khoản và khách hàng là dữ liệu giả.

### 9.3. Triển khai

**Local bắt buộc**

```text
Spring Boot
PostgreSQL
Redis
Mailpit
Thư mục media có volume
```

**Demo public khi có máy chủ**

- Cùng Docker Compose trên máy chủ Linux.
- Caddy làm HTTPS reverse proxy.
- Không mở PostgreSQL/Redis ra Internet.
- Google/VNPAY dùng URL callback của môi trường demo.
- Secrets lấy từ biến môi trường.
- Media lưu volume; `MediaStorage` cho phép chuyển cloud về sau.

Không gắn điều kiện hoàn thành đồ án vào việc mua hosting. Bản local phải chạy độc lập; Google và VNPAY là những luồng cần mạng.

### 9.4. Vận hành tối thiểu

- Theo dõi health, request error, outbox thất bại, payment chưa đối soát, reservation quá hạn.
- Backup PostgreSQL và media trước mỗi release/demo.
- Thử restore ít nhất một lần.
- Release lưu image tag, migration version và cấu hình cần thiết.
- Migration ưu tiên thêm mới tương thích; không giả định rollback code có thể tự hoàn tác schema.

Mục tiêu hiệu năng kiểm thử: 20 người dùng đồng thời, dữ liệu 1.000 sản phẩm giả lập; p95 API đọc nội bộ dưới 800 ms trên cấu hình máy được ghi nhận, không tính thời gian provider ngoài. Đây là mục tiêu đo, không phải kết quả đã đạt.

### 9.5. Bộ tài liệu bàn giao

Bản đặc tả viết lại cần có:

1. Mục tiêu, phạm vi và bảng đối chiếu tài liệu nguồn.
2. Actor, functional requirements và acceptance criteria.
3. Business rules và state machines.
4. Kiến trúc, stack và quyết định thiết kế.
5. ERD, data dictionary và migration strategy.
6. MVC routes, REST contracts và provider ports.
7. Wireframe và UI flows.
8. Backlog, phụ thuộc, phân công và lịch triển khai.
9. Test plan, dữ liệu demo, runbook và kịch bản bảo vệ.
10. Nguồn nghiên cứu và các giới hạn còn lại.

Bảng truy vết dùng dạng:

```text
Yêu cầu đăng ký
→ Use case
→ Business rule
→ Module
→ Màn hình/API
→ Test case
→ Người phụ trách
→ Bằng chứng demo
```

Báo cáo môn học lấy dữ liệu từ bộ đặc tả này để tránh sơ đồ, code và mô tả mâu thuẫn.

### 9.6. Kịch bản bảo vệ

Trong khoảng 12–15 phút:

1. Nêu vấn đề chọn PC và hậu mãi.
2. Tư vấn một nhu cầu trong ngân sách, giải thích kết quả.
3. So sánh, thêm giỏ và đăng nhập.
4. Chứng minh bắt buộc xác minh điện thoại.
5. Đặt đơn, trình bày giữ tồn kho.
6. Demo thanh toán sandbox hoặc COD.
7. Admin gán serial, hoàn tất checklist và giao.
8. Customer xem đúng chiếc máy của mình và gửi bảo hành.
9. Trình bày một lỗi nghiệp vụ đã được xử lý: callback trùng hoặc tranh sản phẩm cuối.
10. Mở test report, CI và bảng đóng góp.

**Tiêu chí hoàn thành cuối cùng:** một người ngoài nhóm có thể dựng ứng dụng theo README, thực hiện trọn vẹn luồng mua và hậu mãi bằng dữ liệu demo, đồng thời nhóm giải thích được vì sao các quy tắc tồn kho, thanh toán, xác thực và bảo hành hoạt động đúng.
