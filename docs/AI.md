# AI trong PC Store

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
