# AI trong PC Store

Smart search và chat dùng chung `ProductRetrievalService` để xếp hạng sản phẩm bằng Spring AI embedding/vector. API gợi ý linh kiện riêng vẫn có trong [Semantic Recommendations](SEMANTIC_RECOMMENDATIONS.md), bật bằng profile `semantic`; cá nhân hóa đọc lịch sử thật từ UserTrackingService qua RecentCategoryProvider.

Tất cả lệnh gọi model chạy trong Java: **React → Spring Boot → Spring AI ChatClient → GoogleGenAiChatModel → Gemini**. Dùng Spring AI 2.0.2 với Spring Boot 4.1.2. Service TypeScript cũ đã được thay thế; không chạy worker cổng 3001.

## Chạy

Yêu cầu Java 21+, Node.js cho frontend, database đã được cấu hình như trước. Đặt `GEMINI_API_KEY` trong `.env` ở thư mục gốc. Spring đọc file này qua `spring.config.import`; biến môi trường vẫn được ưu tiên. Không đưa key vào frontend hay Git.

```properties
GEMINI_API_KEY=your-server-side-key
GEMINI_MODEL=gemini-3.5-flash-lite
GEMINI_API_BASE_URL=https://generativelanguage.googleapis.com
AI_TIMEOUT_MS=20000
AI_RATE_LIMIT_PER_MINUTE=20
AI_RATE_LIMIT_PER_DAY=100
AI_GLOBAL_LIMIT_PER_DAY=1000
```

Base URL cũ kết thúc bằng `/v1beta` vẫn được chuẩn hóa để tương thích cấu hình trước đây. Khởi động lại backend sau khi đổi key/model.

```powershell
# Terminal 1, thư mục gốc
.\mvnw.cmd spring-boot:run

# Terminal 2
cd frontend
npm install
npm run dev
```

## Tìm sản phẩm

Hai ô tìm ở header và trang `/catalog` dùng chung `GET /api/catalog/search`. Ví dụ:

- `chuột giá rẻ chơi game`: khi chưa nêu ngân sách, chỉ trả chuột phù hợp có mức giá thấp nhất; không hiển thị tất cả chuột rồi sắp xếp.
- `chuột rẻ nhất dưới 1 triệu`: chọn mức giá thấp nhất trong giới hạn đã nêu. Nếu nhiều sản phẩm đồng giá thấp nhất, giữ các sản phẩm đó.
- `chuột giá rẻ dưới 1 triệu`: đã có ngân sách cụ thể nên có thể so sánh các mẫu trong mức giá đó; không tự đoán một mức “rẻ” khác.
- `PC cá nhân`: tìm PC bộ, không bắt buộc suy thành PC văn phòng.
- `chuot khong day duoi 1 trieu`: hỗ trợ không dấu, kết nối và mức giá.
- `PC chơi Valorant từ 20 đến 25 triệu`: phân loại nhu cầu và khoảng ngân sách.
- `Logitech G102`, `RTX 5060 Ti`: vẫn tìm hãng/model cụ thể.

Spring AI chuyển câu nhập thành `ProductCriteria`. Server xác minh tiêu chí, giữ giới hạn rõ ràng từ người dùng và lọc danh mục ACTIVE hiện tại từ database. Bộ lọc danh mục, khuyến mãi và sắp xếp trên giao diện vẫn được áp dụng. Khi chọn một danh mục không khớp câu nhập, kết quả có thể rỗng; chọn “Tất cả sản phẩm” để mở rộng.

Giao diện chờ 500 ms sau khi nhập và hủy request cũ khi đổi câu tìm. Cache tối đa 1.000 ý định trong 5 phút; không cache giá/tồn kho. Khi thiếu key, hết quota, timeout hoặc JSON model không hợp lệ, API chuyển sang phân tích từ khóa/đồng nghĩa/ngân sách trong Java (`mode=local`), vẫn trả sản phẩm thật. Nếu database lỗi, trả 503 để giao diện cho phép thử lại.

## Tư vấn cấu hình

### Gửi ảnh, file hoặc dán thông số

Ô chat có nút đính kèm, kéo thả một file, dán ảnh từ clipboard và tự nhận đoạn văn bản thông số nhiều dòng (`CPU: …`, `RAM: …`). Nhận PNG/JPG/WebP/PDF tối đa 5 MiB, TXT/CSV/JSON UTF-8 tối đa 32 KiB. `POST /api/ai/attachments/analyze` dùng multipart và CSRF như chat. File được xử lý trong request, không lưu lâu dài hay đưa vào chỉ mục vector sản phẩm.

Ảnh và PDF gửi dạng inline `Media` qua **Spring AI ChatClient → Google GenAI**, theo [tài liệu multimodal của Spring AI](https://docs.spring.io/spring-ai/reference/api/chat/google-genai-chat.html). Văn bản nằm trong dữ liệu người dùng; nội dung file không được coi là system instruction. Bộ trích xuất chỉ đọc thông số hiện diện, không suy diễn socket từ model, không lấy serial/license hay thông tin cá nhân. Định dạng được kiểm tra bằng đuôi file và chữ ký ảnh/PDF, giới hạn dung lượng được kiểm tra ở frontend và backend. Trường chưa rõ giữ rỗng/0, chữ mờ có `confidence=low`; output sai cấu trúc bị chặn.

Chọn/dán file chỉ tạo bản xem trước gọn trong composer, chưa gọi model. Khi nhấn gửi, ảnh gốc xuất hiện ngay trong tin nhắn; giao diện lần lượt báo đang đọc ảnh và đang tìm linh kiện. Sau khi trích xuất, ứng dụng tự gọi tư vấn và hiển thị thẻ sản phẩm, không mở bảng nhập thông số. Có thể chọn **Máy hiện tại** hoặc **Cấu hình tham khảo** trước khi gửi. Thông số giữ riêng trong `ChatRequest.hardware`, không trộn model máy cũ vào tiêu chí cần mua. Mỗi hội thoại giữ context riêng cho các câu tiếp theo; ảnh/file mới thay context cũ. Tin nhắn lỗi giữ nguyên ảnh để thử lại; file không đọc được không tiếp tục gọi tư vấn. Ảnh xem trước dùng object URL trong bộ nhớ, được giải phóng khi component rời màn hình; không lưu file vào localStorage.

`HardwareMatchAdvisor` dùng vector xếp hạng sản phẩm hiện có, loại xung đột socket/DDR và yêu cầu dung lượng/ngân sách đã biết trước khi Spring AI chọn ID. Đọc lại danh mục và thông số sau inference. Model không được tạo sản phẩm, giá hay URL. Khi ảnh máy hiện tại có RAM/ổ lưu trữ và chưa nêu món cần thay, hệ thống đề xuất hướng nâng cấp những nhóm đó; RAM tự đề xuất phải có dung lượng lớn hơn RAM hiện tại. Ảnh tham khảo mặc định tìm các nhóm linh kiện có trong ảnh, thay vì tự chuyển sang PC bộ. Yêu cầu build mới đầy đủ dùng ảnh làm thông số mục tiêu; trường hợp chưa có ngân sách thì hỏi tổng ngân sách. “Từng linh kiện, không phải PC bộ” giữ đúng ý định build và các câu tiếp theo như “hiệu năng” tiếp tục ngữ cảnh. Model đọc chưa rõ yêu cầu ảnh rõ hơn hoặc tên linh kiện bằng văn bản. Dữ liệu tương thích còn thiếu/nháp luôn ghi đối chiếu sơ bộ, không cam kết lắp được toàn bộ.

Kiểm thử liên quan: `HardwareAttachmentServiceTest`, `HardwareMatchAdvisorTest`, `HardwareAttachmentHttpTest` (với `ai.simulation.url` của Postman advisor). HTTP tests kiểm tra byte ảnh/PDF inline, trích xuất văn bản, lựa chọn linh kiện theo máy cũ, file không hỗ trợ, output không hợp lệ và quota. Fixture không chứng minh OCR thực tế. Ngày 26/11/2026, Gemini thật đã đọc ảnh thông số thử và trả RAM DDR4 trong danh mục dưới 4 triệu.

### Build PC từ linh kiện rời

Câu như `Build PC từ linh kiện rời để chơi game dưới 35 triệu` chuyển sang `ComponentBuildAdvisor` trước nhánh tư vấn PC bộ/theo tựa game. Ngữ cảnh build và ngân sách được kế thừa từ tin nhắn user; yêu cầu PC bộ lắp sẵn hoặc chuột/bàn phím riêng sẽ chuyển lại luồng tương ứng. Chưa có mức ngân sách tối đa thì hỏi lại, chưa gọi model.

Luồng: **Spring AI EmbeddingModel → VectorStore cosine → danh mục database hiện tại → solver kiểm tra tổng ngân sách và xung đột → Spring AI ChatClient chọn một phương án hợp lệ → đọc lại giá/tồn kho/thông số → phản hồi `build` và 8 thẻ linh kiện**. Các nhóm gồm CPU, mainboard, RAM, VGA, lưu trữ, nguồn, case và tản nhiệt CPU; mỗi nhóm một sản phẩm/kit. Chưa hỗ trợ giữ lại linh kiện qua chat, hệ điều hành và thiết bị ngoại vi không nằm trong tổng giá.

Socket và chuẩn RAM không khớp bị loại; các giới hạn nguồn, kích thước, loại kết nối được kiểm tra khi có dữ liệu cấu trúc tương ứng. Không suy diễn thông số từ model. Dữ liệu thiếu hoặc trạng thái `draft` được hiển thị `needs_verification`; không coi cùng socket là chứng nhận BIOS hay khả năng lắp hoàn chỉnh. Không đủ nhóm, vượt tiền hoặc không ghép được thì trả `no_match`, không thay bằng PC bộ và không hiển thị bộ thiếu như PC hoàn chỉnh. Model chỉ được trả `optionId` từ danh sách đã kiểm tra. Giá/tồn kho thay đổi sau inference trả 409.

Vector chỉ xếp hạng; giá, ảnh, tồn kho và thông số lấy từ database. RAM index được tạo khi truy vấn đầu tiên nếu đang rỗng, các lần sau dùng lại snapshot đến khi reindex hoặc khởi động lại. Profile `semantic` dùng PgVector và đồng bộ có kiểm tra content hash ở lần truy vấn đầu. Chỉ mục tự đồng bộ này là dữ liệu dẫn xuất, không sửa sản phẩm. Search/tư vấn thông thường vẫn dùng bộ lọc Java nếu vector không khả dụng; build yêu cầu vector hoạt động và trả lỗi rõ ràng khi embedding lỗi. `mode` của search tiếp tục mô tả bước phân tích ý định, không phải trạng thái vector.

Giao diện có danh sách đủ 8 nhóm, tổng tiền, ngân sách còn lại, kiểm tra tương thích, tải danh sách và tab **Xem ảnh cấu hình** dùng ảnh thật từng món/case. Đây là bảng ảnh minh họa, không phải ảnh dựng 3D hay hình PC đã lắp.

Kiểm thử luồng mới:

```powershell
.\mvnw.cmd test '-Dtest=ComponentBuildAdvisorTest,ProductRetrievalServiceTest'
# Chạy hai Postman simulation hiện có tại cổng 4511 và 4512 trước:
.\mvnw.cmd test '-Dtest=ComponentBuildHttpTest' '-Dai.simulation.url=http://localhost:4511/gemini' '-Dvector.simulation.url=http://localhost:4512/embedding'
node scripts/check-component-build.mjs
```

Simulation kiểm tra giao thức embedding, truy xuất và lựa chọn JSON cùng nhánh quota/ID giả; vector fixture không chứng minh chất lượng ngữ nghĩa thực tế. Ngày 26/11/2026, local profile với Gemini thật và danh mục thật đã trả đủ 8 linh kiện, tổng 31.320.000 đ trong ngân sách 35 triệu. Yêu cầu dưới 30 triệu trả no-match; các thông số danh mục hiện tại còn nháp nên kết quả cần xác minh trước khi lắp.

Giữ nguyên trang `/advisory`, giao diện hội thoại và hợp đồng `/api/ai/status`, `/api/ai/chat`. CSRF vẫn bắt buộc cho POST. Lịch sử tối đa 8 lượt, tin nhắn tối đa 1.200 ký tự, body tối đa 24 KiB. Chỉ tin nhắn user được dùng để kế thừa nhóm sản phẩm/ngân sách.

Java chọn tối đa 8 ứng viên, Spring AI yêu cầu Gemini chọn ID và mã lý do. Server kiểm tra ID, tổng ngân sách, nhóm sản phẩm, đọc lại database sau inference và tự dựng phản hồi tiếng Việt. Giá, ảnh, URL và tồn kho không lấy từ model. Tồn kho bằng 0 không được đổi thành còn hàng.

### Tư vấn theo tựa game

`GameRequirementsService` nhận diện tên từ câu “máy tính có cấu hình chơi Black Myth: Wukong?”, “máy chạy Elden Ring” hoặc các tên game khác; tên game được kế thừa từ lịch sử user. Khi chuyển sang hỏi chuột/bàn phím, ngữ cảnh game cũ dừng áp dụng.

- Với Black Myth: Wukong, đọc dữ liệu yêu cầu tối thiểu/khuyến nghị đã đối chiếu trang Steam của Game Science, có URL nguồn và ngày kiểm tra trong `src/main/resources/ai/games/black-myth-wukong.json`. Đây là dữ liệu tham chiếu có ngày, không giả là một lần tra cứu trực tiếp.
- Các tựa khác tra qua `AiGateway.askGrounded`: Spring AI bật Google Search trên model đang cấu hình. `SourcedGoogleChatModel` giữ lại `groundingMetadata` từ phản hồi nhà cung cấp để URL nguồn không do model tự viết. Không tự đổi model hay đăng ký dịch vụ tìm kiếm khác. Khi nhà cung cấp hết quota thì trả 429 với Retry-After; thiếu nguồn/không rõ tên thì hỏi lại, không đoán cấu hình.
- `GameAdvisor` yêu cầu Spring AI so sánh riêng CPU/GPU của từng PC với minimum/recommended. Mã Java kiểm tra RAM, dung lượng ổ và yêu cầu SSD bằng thông số thật, loại máy thiếu thông tin/dưới minimum, kiểm tra lại database sau inference. Không suy “gaming” là đủ chạy game; không so số model GPU một cách máy móc.
- CPU/GPU là đối chiếu sơ bộ từ AI, không phải benchmark. Dung lượng ổ đã lắp không chứng minh dung lượng trống; giao diện nhắc khách chừa dung lượng game và kiểm tra Windows/DirectX/driver. Không hứa FPS hay thiết lập 2K/4K.
- Mặc định ưu tiên máy đạt khuyến nghị, nếu chỉ có máy đạt tối thiểu thì nêu rõ. Với “rẻ nhất”, chọn mức giá thấp nhất trong những máy đã được đánh giá phù hợp. Ngân sách và tồn kho vẫn là điều kiện bắt buộc.
- Cache yêu cầu có nguồn và kết quả so sánh CPU/GPU theo tựa game + thông số trong 24 giờ. Giá/tồn kho luôn đọc lại; đổi thông số thì đánh giá lại. Chat hiển thị hai mức cấu hình, nguồn, ngày kiểm tra và phần đối chiếu trên thẻ máy.

Orbit đã được tra cho endpoint yêu cầu cấu hình Steam nhưng không có kết quả phù hợp. Truy cập trực tiếp Steam từ máy phát triển không thành công; vì vậy không triển khai phụ thuộc vào API Steam chưa được tài liệu hóa. Phần tra cứu mở rộng dùng khả năng Google Search của Spring AI, kiểm tra bằng mock generateContent có grounding và kiểm tra live khi quota cho phép.

Tìm kiếm và chat chia sẻ giới hạn theo địa chỉ kết nối thực tế, tổng lượt ngày, cooldown quota và tối đa 2 lệnh model đồng thời. Cache hit không tính lượt. Bộ đếm nằm trong bộ nhớ và đặt lại khi restart. Không tự retry hoặc đổi sang model trả phí.

## Kiểm tra

```powershell
.\mvnw.cmd test
cd frontend
npm run build
npm run test:catalog
```

Kiểm tra hợp đồng HTTP thật của Spring AI/Google SDK với Postman simulation đã lưu trong repository:

```powershell
# Terminal 1, thư mục gốc
postman simulation run postman/simulations/pixelcore-advisor.sim.yaml --port 4900 --no-history

# Terminal 2
.\mvnw.cmd test "-Dai.simulation.url=http://127.0.0.1:4900/gemini"
```

`SpringAiHttpTest` gọi thật qua Spring AI và SDK tới mock HTTP, kiểm tra tìm theo nghĩa, cache ý định, giới hạn ngân sách, tư vấn tiếp nối, dữ liệu model giả, 403/429/503 và timeout. Test dùng snapshot catalog tại `postman/mocks/pixelcore-catalog/catalog.json` thay cho database; không chứng minh chất lượng model thật. Không có cấu hình simulation thì nhóm test này được skip có chủ đích.

Để kiểm tra backend đang chạy với database và Gemini thật (5 lệnh inference tối đa, một số câu có thể đã có cache):

```powershell
node scripts/check-ai.mjs --live
# Có thể chọn cổng backend khác
$env:AI_TEST_BACKEND_URL = 'http://127.0.0.1:8082'
node scripts/check-ai.mjs --live
# Kiểm tra rẻ nhất + đối chiếu Wukong, không yêu cầu tìm kiếm web thêm
node scripts/check-game-advisor.mjs
# Thêm tựa game ngoài dữ liệu tham chiếu, cần quota Google Search của model
node scripts/check-game-advisor.mjs --grounded
```

API specs: `postman/specs/smart-search.openapi.yaml`, `postman/specs/ai-advisor.openapi.yaml`. Mock chỉ hỗ trợ subset `generateContent` mà ứng dụng dùng, không mô phỏng toàn bộ Google API.

Tài liệu nền tảng: [Spring AI và phiên bản Boot](https://docs.spring.io/spring-ai/reference/getting-started.html), [Google GenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/google-genai-chat.html), [Google structured output](https://ai.google.dev/gemini-api/docs/generate-content/structured-output).
### Danh mục linh kiện bổ sung (26/11/2026)

Flyway V21 và V22 bổ sung 34 sản phẩm mẫu, đưa 6 linh kiện cũ vào storefront và thêm bảng thông số cho toàn bộ 34 sản phẩm cửa hàng đã nhập. Sau áp dụng trên database hiện tại: 78 sản phẩm, 74 sản phẩm hiển thị; gồm 11 CPU, 5 mainboard, 9 RAM, 9 VGA, 8 SSD, 6 PSU, 5 case, 4 tản nhiệt và 7 màn hình. RAM DDR5 có lựa chọn 16/32/64 GB; các nền tảng gồm LGA1700, AM4 và AM5.

Giá/tồn kho của 44 bản ghi cũ được giữ nguyên. Các SKU `DEMO-*` mới dùng giá và tồn kho mẫu (20 sản phẩm/SKU), có cờ `demoCatalog`; giao diện chi tiết và danh sách build hiển thị thông tin này. Không xem đây là giá hoặc tồn kho đang bán của GearVN/Phong Vũ. Nguồn nhập cũ được giữ trong `storefront_products.provenance.source`; thông số và nguồn đối chiếu lưu riêng trong `technicalSpecs`, `specSources`, `specNotes`.

`product_component_specs.specs.verified_fields` ghi rõ từng trường có bằng chứng; `verification_status` của cả bản ghi không tự chứng nhận những trường còn thiếu. Bộ kiểm tra build đối chiếu socket/dòng CPU, DDR/UDIMM/dung lượng/số thanh, mức nguồn GPU khuyến nghị và đầu ATX/EPS/PCIe, chiều dài GPU/PSU và khổ main, socket/chiều cao tản khí/khoảng trống RAM, SSD NVMe/kích thước/khe M.2. Xung đột đã biết bị loại; ưu tiên phương án ít mục chưa xác minh nhất trong ngân sách. Các model có hậu tố khác nhau (12400/12400F, RTX 5060/5060 Ti) được phân biệt.

Không suy ra công suất đỉnh từ TDP, không tự điền mã linh kiện cho PC bộ, và không dùng cỡ radiator danh nghĩa để chứng nhận lắp vừa. BIOS thực tế, QVL/XMP/EXPO, đường đi cáp và thử tải vẫn cần kiểm tra khi lắp. Các bản nhập không có mã module RAM, revision bộ cáp PSU hoặc tổng độ dày AIO vẫn giữ ghi chú cần đối chiếu. Đặc biệt, nguồn GearVN của bản PC i5-12400F/RTX3050 chứa URL i5-13400F: dữ liệu lịch sử được giữ và đánh dấu khác biệt.

Manifest nguồn: `src/main/resources/catalog/build-catalog-2026-11.json` (snapshot V21), `build-catalog-ddr5-extension.json` và `catalog-presentation-patch.sql` (V22). V23 thay toàn bộ 31 ảnh chờ còn lại bằng ảnh sản phẩm đã tải vào `frontend/src/assets/products`; 74 sản phẩm storefront đều có ảnh thật. `product-photo-catalog.json` lưu URL ảnh, trang nguồn, ghi chú phiên bản và SHA-256; nguồn gồm hãng, GearVN và nhà phân phối. RAM dùng ảnh đúng thế hệ, màu sắc và số thanh; ảnh dòng RAM dùng chung giữa các dung lượng được ghi rõ trong manifest. Ảnh được Vite đóng gói, không phụ thuộc vào việc trình duyệt truy cập được website nguồn. V23 chỉ thay ảnh chờ tương ứng và bổ sung nguồn ảnh; giữ nguyên giá, tồn kho và thông số. `scripts/sync-product-photos.py --output <migration-mới.sql>` kiểm tra ảnh và tạo migration ảnh mới. `scripts/generate-build-catalog-migration.py --manifest <manifest.json> --output <migration-mới.sql>` tạo migration danh mục mới; không sinh lại V21/V22/V23 đã áp dụng. V22 thêm `--append-sql src/main/resources/catalog/catalog-presentation-patch.sql` khi được chuẩn bị ban đầu.

Reindex sau thay đổi: `scripts/inspect-vectors.ps1 -Reindex`. Nội dung embedding gồm bảng thông số và đánh dấu dữ liệu cũ chưa đối chiếu; local profile dùng RAM VectorStore, semantic profile dùng chỉ mục bền vững như các phần trước. Chi tiết kiểm tra tại `docs/testing/catalog-enrichment-verification.json`.
