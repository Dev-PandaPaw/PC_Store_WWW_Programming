# KẾ HOẠCH PHÂN CHIA CÔNG VIỆC VÀ TÀI LIỆU PHẢN BIỆN CHUYÊN ĐỀ SPRING AI & USER TRACKING
## DỰ ÁN BÀI TẬP LỚN: HỆ THỐNG PC STORE (`PC_Store_WWW_Programming`)
**Học phần:** Lập trình WWW — Đại học Công nghiệp TP. Hồ Chí Minh (IUH)  
**Nhóm thực hiện:** Nhóm 16  
**Căn cứ tài liệu:** Bài giảng Slide Chapter 7 — *Spring AI: Connecting Enterprise Data & APIs with AI Models* & Yêu cầu thực tế của Giảng viên.

---

## MỤC LỤC
1. [TỔNG QUAN VÀ MA TRẬN PHỦ 100% SLIDE CHAPTER 7](#1-tổng-quan-và-ma-trận-phủ-100-slide-chapter-7)
2. [SƠ ĐỒ KIẾN TRÚC VÀ TỔ CHỨC THƯ MỤC CODE](#2-sơ-đồ-kiến-trúc-và-tổ-chức-thư-mục-code)
3. [BẢN MÔ TẢ CHI TIẾT CÔNG VIỆC 5 THÀNH VIÊN](#3-bản-mô-tả-chi-tiết-công-việc-5-thành-viên)
   - 3.1. Võ Văn Cảnh — Front-end User Tracking & Event Hook
   - 3.2. Huỳnh Đoàn Nhân — Backend Activity Storage & User Profile API
   - 3.3. Văn Nhựt — Spring AI Vector Store & Ingestion Pipeline (Chunking & Embedding)
   - 3.4. Lê Thị Kim Ngân — Semantic Search, Cosine Similarity Engine & Guardrails
   - 3.5. Bùi Ngọc Bửu — Front-end Recommendations Widget & Admin Sync UI
4. [BỘ CÂU HỎI VÀ KỊCH BẢN TRẢ LỜI CHẤT VẤN CỦA GIẢNG VIÊN (CHEAT SHEET)](#4-bộ-câu-hỏi-và-kịch-bản-trả-lời-chất-vấn-của-giảng-viên-cheat-sheet)
5. [QUY TRÌNH PHỐI HỢP GIT ĐẢM BẢO ZERO CONFLICT](#5-quy-trình-phối-hợp-git-đảm-bảo-zero-conflict)

---

## 1. TỔNG QUAN VÀ MA TRẬN PHỦ 100% SLIDE CHAPTER 7

Dự án áp dụng toàn diện các kiến thức chuẩn trong Slide bài giảng Chapter 7 vào bài toán thực tế của Website PC Store:
- **Bài toán áp dụng:** **Hệ thống Gợi ý Linh kiện & Cấu hình PC Cá nhân hóa (Personalized PC Recommendation)** kết hợp theo dõi hành vi người dùng (User Tracking UI) và tìm kiếm tương đồng ngữ nghĩa (Semantic Similarity Search).

### BẢNG ĐỐI CHIẾU TRỰC TIẾP VỚI CÁC TRANG SLIDE CHAPTER 7:

| Slide # | Chủ đề trong Slide bài giảng | Ứng dụng cụ thể trong Code PC Store | Thành viên phụ trách |
| :---: | :--- | :--- | :--- |
| **Yêu cầu riêng** | **Front-end User tracking trên UI của hệ thống** | Hook bắt sự kiện xem hàng, thời gian onsite, từ khóa tìm kiếm (`useUserTracker.js`) | **Võ Văn Cảnh** |
| **Slide 6 & 16** | **Database & Entity:** Lưu trữ dữ liệu hệ thống, bảo toàn session/user identity | Bảng `user_activity_logs` (Flyway `V15`), `UserActivityLog.java`, Spring Data JPA | **Huỳnh Đoàn Nhân** |
| **Slide 8, 9, 12** | **Ingestion (ETL) & VectorStore:** Document Reader $\rightarrow$ Chunking $\rightarrow$ Embedding Model $\rightarrow$ Vector Store | `VectorStoreConfig.java`, băm thông số `ProductChunker.java`, `ProductVectorIngestionService.java` | **Văn Nhựt** |
| **Slide 13 & 14** | **Cosine Similarity & Semantic Search:** Công thức $\cos(\theta) = \frac{A \cdot B}{\|A\| \|B\|}$, tìm kiếm theo ngữ nghĩa | `ProductSemanticRecommendationService.java`, truy vấn `similaritySearch` với threshold $\ge 0.7$ | **Lê Thị Kim Ngân** |
| **Slide 11, 16, 18** | **Tool Calling & Safe Ordering Guardrails:** Kiểm tra giá, tồn kho thực tế từ DB, chặn Hallucination | Tích hợp kiểm tra tồn kho (`stockOnHand`), xác minh socket tương thích CPU/Mainboard trước khi gợi ý | **Lê Thị Kim Ngân** |
| **Slide 10, 15** | **UI Display & Admin Operations:** Giao diện hiển thị gợi ý thông minh và kích hoạt đồng bộ | Component `PersonalizedRecommendations.jsx`, Nút bấm trigger re-index trên `AdminProductsPage.jsx` | **Bùi Ngọc Bửu** |
| **Slide 19, 20** | **Best practices & DevOps:** Config API key trong `.env`, Modular interfaces, Mock AI trong Unit test | Cấu hình Spring profile `local`, biến môi trường `GEMINI_API_KEY`, viết unit test có mock | **Cả 5 thành viên** |

---

## 2. SƠ ĐỒ KIẾN TRÚC VÀ TỔ CHỨC THƯ MỤC CODE

### 2.1. Sơ đồ luồng dữ liệu liên thông giữa 5 thành viên:

```
[ FRONT-END BROWSER ]
   │
   ├─► (1. Cảnh) useUserTracker.js (Bắt event: VIEW_PRODUCT, DWELL_TIME >= 30s)
   │         │ (POST /api/tracking/events)
   │         ▼
[ BACKEND SPRING BOOT ]
   │
   ├─► (2. Nhân) UserTrackingController.java ──► Lưu vào PostgreSQL: user_activity_logs
   │                                                       │
   │                                                       ▼ (Lấy top danh mục user quan tâm)
   ├─► (3. Nhựt) ProductChunker.java                       │
   │         │ (Băm CPU/VGA/RAM)                           │
   │         ▼                                             │
   │     EmbeddingModel (Gemini)                           │
   │         │ (Sinh Vector 768 chiều)                     │
   │         ▼                                             │
   │     VectorStore (Simple/PgVector)                     │
   │         │                                             │
   │         └──────────────────────┐                      │
   │                                ▼                      ▼
   ├─► (4. Ngân) ProductSemanticRecommendationService.java (Cosine Similarity Search)
   │         │ (Lọc threshold >= 0.7 + Kiểm tra tồn kho & tương thích socket)
   │         ▼
   │     RecommendationController.java (GET /api/v1/recommendations/personalized)
   │         │
[ FRONT-END UI ]
   │         ▼
   ├─► (5. Bửu) PersonalizedRecommendations.jsx (Render Carousel sản phẩm gợi ý ra HomePage & CartPage)
   │     VÀ AdminProductsPage.jsx (Nút bấm gọi POST /api/admin/vector/reindex sang code của Nhựt)
```

### 2.2. Tổ chức thư mục code (Folder & Package Structure):

```text
PC_Store_WWW_Programming/
├── frontend/src/
│   ├── hooks/
│   │   └── useUserTracker.js                     <-- [CẢNH] Hook lắng nghe hành vi người dùng
│   ├── services/
│   │   ├── tracking.js                           <-- [CẢNH] Axios client gửi tracking event
│   │   └── recommendations.js                    <-- [BỬU] Axios client lấy sản phẩm gợi ý
│   ├── components/
│   │   └── PersonalizedRecommendations.jsx       <-- [BỬU] Widget hiển thị sản phẩm đề xuất
│   └── pages/
│       ├── AdminProductsPage.jsx                 <-- [BỬU] Thêm nút "Đồng bộ Vector AI"
│       ├── ProductDetailPage.jsx                 <-- [CẢNH] Gắn hook tracking xem chi tiết
│       └── HomePage.jsx                          <-- [BỬU] Nhúng widget đề xuất
│
└── src/main/
    ├── java/iuh/fit/nhom16/pcstore/
    │   │
    │   ├── tracking/                             <-- [NHÂN] Package quản lý nhật ký hành vi
    │   │   ├── entity/UserActivityLog.java
    │   │   ├── dto/TrackingEventRequest.java
    │   │   ├── repository/UserActivityLogRepository.java
    │   │   ├── service/UserTrackingService.java
    │   │   └── controller/UserTrackingController.java
    │   │
    │   ├── ai/
    │   │   ├── vector/                           <-- [NHỰT] Package Spring AI Vector & Ingestion
    │   │   │   ├── VectorStoreConfig.java
    │   │   │   ├── ProductChunker.java
    │   │   │   └── ProductVectorIngestionService.java
    │   │   │
    │   │   └── recommendation/                   <-- [NGÂN] Package gợi ý Cosine Similarity & RAG
    │   │       ├── dto/RecommendationResponse.java
    │   │       ├── service/ProductSemanticRecommendationService.java
    │   │       └── controller/RecommendationController.java
    │   │
    └── resources/
        └── db/migration/
            └── V15__create_user_activity_logs.sql <-- [NHÂN] Script tạo bảng nhật ký hành vi
```

---

## 3. BẢN MÔ TẢ CHI TIẾT CÔNG VIỆC 5 THÀNH VIÊN

### 3.1. VÕ VĂN CẢNH — Front-end User Tracking & Event Hook
- **Mục tiêu kỹ thuật:** Xây dựng cơ chế theo dõi hành vi tương tác của khách hàng trên giao diện một cách nhẹ nhàng, phi đồng bộ (non-blocking), không làm giảm hiệu năng load trang.
- **Chi tiết mã nguồn cần viết:**
  1. `frontend/src/services/tracking.js`:
     - Hàm `sendTrackingEvent({ eventType, productId, category, metadata })`.
     - Sử dụng `navigator.sendBeacon` (hoặc Axios fallback) để gửi request nền an toàn kể cả khi người dùng chuyển trang hoặc đóng tab.
  2. `frontend/src/hooks/useUserTracker.js`:
     - Theo dõi thời gian người dùng dừng lại xem một sản phẩm (**dwell time**). Nếu thời gian onsite $\ge 30$ giây $\rightarrow$ tự động kích hoạt event `INTEREST_CONFIRMED`.
     - Bắt các sự kiện: `VIEW_PRODUCT`, `SEARCH_KEYWORD`, `FILTER_CATEGORY`.
     - Áp dụng cơ chế **Debounce 500ms** đối với các thao tác gõ tìm kiếm để không spam request lên server.
  3. Tích hợp:
     - Gọi `useUserTracker()` bên trong [`ProductDetailPage.jsx`](file:///d:/PC_Store_WWW_Programming/frontend/src/pages/ProductDetailPage.jsx).

---

### 3.2. HUỲNH ĐOÀN NHÂN — Backend Activity Storage & User Profile API
- **Mục tiêu kỹ thuật:** Lưu trữ bền vững dữ liệu hành vi người dùng vào cơ sở dữ liệu PostgreSQL, liên kết chặt chẽ với Session ID của khách vãng lai và User ID của tài khoản đã đăng nhập.
- **Chi tiết mã nguồn cần viết:**
  1. `src/main/resources/db/migration/V15__create_user_activity_logs.sql`:
     ```sql
     CREATE TABLE user_activity_logs (
         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
         user_id UUID,
         session_id VARCHAR(100) NOT NULL,
         event_type VARCHAR(50) NOT NULL,
         product_id UUID,
         category_slug VARCHAR(100),
         metadata TEXT,
         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
     );
     CREATE INDEX idx_user_activity_session ON user_activity_logs(session_id);
     CREATE INDEX idx_user_activity_user ON user_activity_logs(user_id);
     ```
  2. `UserActivityLog.java` & `UserActivityLogRepository.java`:
     - Viết truy vấn tìm top danh mục/sản phẩm người dùng quan tâm nhiều nhất trong 7 ngày gần nhất:
       `findTopCategoriesBySessionOrUser(...)`.
  3. `UserTrackingController.java`:
     - Endpoint: `POST /api/tracking/events`
     - Nhận payload từ Cảnh gửi lên, kiểm tra session/auth hiện tại và lưu vào DB.

---

### 3.3. VĂN NHỰT — Spring AI Vector Store & Ingestion Pipeline
- **Mục tiêu kỹ thuật:** Hiện thực hóa quy trình Ingestion ETL (Extract - Transform/Chunk - Embed - Load) theo đúng **Slide 12** bài giảng, lưu trữ dữ liệu vector vào `VectorStore`.
- **Chi tiết mã nguồn cần viết:**
  1. `VectorStoreConfig.java`:
     - Khởi tạo Spring Bean `VectorStore` (dùng `SimpleVectorStore` có sẵn trong Spring AI cho môi trường dev, hoặc kết nối `PgVectorStore`).
     - Khởi tạo `EmbeddingModel` tích hợp với Google Gemini qua API Key đã cấu hình trong `.env`.
  2. `ProductChunker.java` (Logic băm văn bản linh kiện):
     - Chuyển đổi một Entity `Product` thành văn bản bán cấu trúc (Semi-structured text chunk) chứa đầy đủ ngữ nghĩa:
       ```text
       Sản phẩm: Bộ vi xử lý Intel Core i5 12400F
       Danh mục: CPU
       Thương hiệu: Intel
       Thông số: Socket LGA1700, 6 nhân 12 luồng, Xung boost 4.4GHz, Điện áp TDP 65W
       Mô tả: Phù hợp chơi game, làm việc văn phòng, ghép tốt với mainboard H610/B760 và VGA RTX 3050/4060.
       ```
  3. `ProductVectorIngestionService.java`:
     - Đọc toàn bộ danh sách sản phẩm từ `ProductRepository`.
     - Đưa từng sản phẩm qua `ProductChunker` $\rightarrow$ tạo `org.springframework.ai.document.Document` kèm Metadata (`productId`, `price`, `category`, `stockOnHand`).
     - Gọi `vectorStore.add(documents)` để sinh embedding vector và lưu trữ.
     - Cung cấp phương thức `reindexAll()` để Admin có thể kích hoạt nạp lại vector khi cần.

---

### 3.4. LÊ THỊ KIM NGÂN — Semantic Search, Cosine Similarity Engine & Guardrails
- **Mục tiêu kỹ thuật:** Triển khai thuật toán tính toán Cosine Similarity truy vấn Vector Database theo đúng **Slide 13, 14**, kết hợp rào chắn an toàn (Safe Guardrails theo **Slide 18**) kiểm tra tồn kho và tương thích linh kiện.
- **Chi tiết mã nguồn cần viết:**
  1. `ProductSemanticRecommendationService.java`:
     - **Tính toán Cosine Similarity:**
       Sử dụng `vectorStore.similaritySearch(SearchRequest.query(userPrompt).withTopK(5).withSimilarityThreshold(0.70))`.
       *(Chỉ chấp nhận các linh kiện có độ tương đồng $\cos(\theta) \ge 0.70$)*.
     - **Gợi ý cá nhân hóa dựa trên hành vi:**
       Lấy top danh mục người dùng xem gần đây từ service của Nhân $\rightarrow$ dựng câu truy vấn ngữ cảnh (contextual prompt) $\rightarrow$ tìm linh kiện đề xuất tương đồng.
     - **Safe Ordering Guardrails (Slide 18):**
       Lọc bỏ các sản phẩm có `stockOnHand <= 0` (hết hàng) và kiểm tra tương thích cơ bản (ví dụ CPU socket LGA1700 chỉ đi với Mainboard socket LGA1700).
  2. `RecommendationController.java`:
     - `GET /api/v1/recommendations/personalized`: Trả về danh sách linh kiện gợi ý riêng cho user hiện tại.
     - `GET /api/v1/recommendations/similar/{productId}`: Trả về các sản phẩm tương đồng về hiệu năng/tầm giá với sản phẩm đang xem.
  3. `RecommendationServiceTest.java`:
     - Viết Unit Test kiểm thử thuật toán và ngưỡng similarity threshold.

---

### 3.5. BÙI NGỌC BỬU — Front-end Recommendations Widget & Admin Sync UI
- **Mục tiêu kỹ thuật:** Hoàn thiện giao diện hiển thị danh sách sản phẩm thông minh đến người dùng cuối và bổ sung công cụ quản trị dữ liệu vector cho Admin.
- **Chi tiết mã nguồn cần viết:**
  1. `frontend/src/services/recommendations.js`:
     - Hàm `getPersonalizedRecommendations()` và `getSimilarProducts(productId)`.
  2. `frontend/src/components/PersonalizedRecommendations.jsx`:
     - Component giao diện hiển thị dạng Carousel / Grid thẻ sản phẩm: *"Gợi ý dành riêng cho bạn (AI Semantic Match)"*.
     - Hiển thị badge độ tương đồng (ví dụ: `Phù hợp 95%`) nếu có điểm similarity score.
     - Nhúng component này vào trang chủ [`HomePage.jsx`](file:///d:/PC_Store_WWW_Programming/frontend/src/pages/HomePage.jsx) và trang giỏ hàng [`CartPage.jsx`](file:///d:/PC_Store_WWW_Programming/frontend/src/pages/CartPage.jsx).
  3. `frontend/src/pages/AdminProductsPage.jsx`:
     - Thêm nút bấm **"Đồng bộ Vector AI"** trên thanh toolbar quản trị sản phẩm.
     - Khi Admin nhấn nút $\rightarrow$ gửi request `POST /api/admin/vector/reindex` sang service của Văn Nhựt $\rightarrow$ hiển thị thông báo toast: *"Đã vector hoá thành công X sản phẩm vào Vector Store"*.

---

## 4. BỘ CÂU HỎI VÀ KỊCH BẢN TRẢ LỜI CHẤT VẤN CỦA GIẢNG VIÊN (CHEAT SHEET)

Bộ câu hỏi này được soạn sẵn nhằm rào trước tất cả những câu hỏi hóc búa, kỹ thuật mà giảng viên hay chất vấn khi bảo vệ đồ án:

---

### CÂU HỎI 1 (DÀNH CHO VÕ VĂN CẢNH):
**Giảng viên hỏi:** *"Cơ chế User tracking trên giao diện Front-end hoạt động như thế nào? Bắt sự kiện gì? Có làm đơ hoặc chậm trang web của khách không?"*

> **Kịch bản trả lời lưu loát của Cảnh:**  
> *"Dạ thưa Cô, cơ chế User Tracking của nhóm em được đóng gói trong custom hook `useUserTracker.js` tại thư mục `frontend/src/hooks/`.*  
> *1. **Về sự kiện thu thập:** Bắt các hành vi có giá trị định hình sở thích khách hàng gồm: sự kiện xem sản phẩm (`VIEW_PRODUCT`), từ khóa tìm kiếm (`SEARCH_KEYWORD`), và đặc biệt là thời gian onsite thực tế (`DWELL_TIME`). Nếu khách dừng lại xem một trang sản phẩm từ 30 giây trở lên, hệ thống sẽ xác nhận khách có độ quan tâm cao đến nhóm linh kiện đó.*  
> *2. **Về mặt hiệu năng:** Cơ chế này hoàn toàn **không làm chậm web** vì 2 lý do kỹ thuật:*  
> *- Thứ nhất, toàn bộ việc gửi dữ liệu được thực hiện bất đồng bộ (asynchronous non-blocking), tách biệt hoàn toàn với luồng render giao diện React.*  
> *- Thứ hai, với các sự kiện gõ tìm kiếm, em áp dụng kỹ thuật **Debounce 500ms** để gom sự kiện, tránh tình trạng mỗi ký tự gõ vào lại spam 1 request HTTP lên máy chủ."*

---

### CÂU HỎI 2 (DÀNH CHO HUỲNH ĐOÀN NHÂN):
**Giảng viên hỏi:** *"Dữ liệu hành vi người dùng được lưu trữ ở đâu? Lưu vào bảng nào, cột nào? Khách chưa đăng nhập (khách vãng lai) thì lưu kiểu gì?"*

> **Kịch bản trả lời lưu loát của Nhân:**  
> *"Dạ thưa Cô, toàn bộ nhật ký hành vi được lưu trữ trong cơ sở dữ liệu PostgreSQL (trên Supabase Cloud) thông qua file migration `V15__create_user_activity_logs.sql` và Entity `UserActivityLog.java` do em phụ trách.*  
> *- **Về cấu trúc bảng:** Bảng có tên `user_activity_logs` gồm các trường chính:*  
> `id` *(UUID khóa chính)*, `event_type` *(loại hành vi)*, `product_id` *(mã linh kiện)*, `category_slug` *(danh mục)*, và `created_at` *(thời điểm xảy ra sự kiện)*.  
> *- **Về khách vãng lai:** Bảng thiết kế tách biệt 2 cột: `user_id` (chứa UUID tài khoản nếu đã đăng nhập) và `session_id` (chuỗi định danh session từ cookie trình duyệt). Đối với khách chưa đăng nhập, `user_id` sẽ mang giá trị `NULL`, hệ thống sẽ gom nhóm và phân tích sở thích dựa trên `session_id`. Khi khách đăng nhập, hệ thống có thể gộp vết hành vi từ session vào tài khoản chính thức."*

---

### CÂU HỎI 3 (DÀNH CHO VĂN NHỰT):
**Giảng viên hỏi:** *"Spring AI và Vector Database trong dự án hoạt động ra sao? Đoạn code băm (chunking) ở đâu, băm cái gì? Vector sinh ra thế nào và lưu ở đâu?"* (Thuộc Slide 8, 9, 12)

> **Kịch bản trả lời lưu loát của Nhựt:**  
> *"Dạ thưa Cô, phần hạ tầng Spring AI và Vector Store nằm tại package `iuh.fit.nhom16.pcstore.ai.vector` do em xây dựng theo đúng quy trình Ingestion ETL ở **Slide 12** bài giảng:*  
> *1. **Đoạn code băm (Chunking) nằm ở file `ProductChunker.java`:** Để mô hình AI hiểu được ý nghĩa linh kiện, em không băm text ngẫu nhiên mà trích xuất có cấu trúc: Tên linh kiện + Danh mục + Hãng sản xuất + Thông số kỹ thuật (Socket, Số nhân, Xung nhịp, Chuẩn RAM, Công suất nguồn) + Mô tả sử dụng.*  
> *2. **Về sinh Vector (Embedding):** Đoạn text sau khi băm được đưa vào `EmbeddingModel` của Spring AI (cấu hình trong `VectorStoreConfig.java`). Mô hình toán học sẽ chuyển đổi chuỗi văn bản thành một mảng vector số thực nhiều chiều (Vector Embedding) đại diện cho ngữ nghĩa của linh kiện.*  
> *3. **Về nơi lưu trữ:** Em cấu hình lưu trữ trong `VectorStore` của Spring AI. Mỗi bản ghi trong VectorStore bao gồm vector nhúng, ID sản phẩm và Metadata đính kèm (giá, tồn kho) để phục vụ việc tra cứu tương đồng sau này."*

---

### CÂU HỎI 4 (DÀNH CHO LÊ THỊ KIM NGÂN):
**Giảng viên hỏi:** *"Đoạn code tính toán nằm ở đâu? Tính toán cái gì? Công thức ra sao? Làm sao để AI không gợi ý linh kiện hết hàng hoặc sai thông số kỹ thuật?"* (Thuộc Slide 13, 14, 18)

> **Kịch bản trả lời lưu loát của Ngân:**  
> *"Dạ thưa Cô, toàn bộ logic tính toán nằm tại file `ProductSemanticRecommendationService.java` do em đảm nhiệm:*  
> *1. **Code tính toán cái gì:** Đoạn code thực hiện tính toán độ tương đồng ngữ nghĩa bằng thuật toán **Cosine Similarity** theo đúng công thức ở **Slide 13** của trường:*  
> $$\cos(\theta) = \frac{A \cdot B}{\|A\| \|B\|}$$  
> *Hàm `similaritySearch` so sánh góc $\theta$ giữa vector nhu cầu của khách hàng (Vector $A$) với vector của các linh kiện trong cơ sở dữ liệu (Vector $B$). Nếu $\theta \approx 0^\circ \rightarrow \cos \approx 1$, nghĩa là linh kiện hoàn toàn khớp với nhu cầu. Em thiết lập ngưỡng `similarityThreshold = 0.70`, loại bỏ toàn bộ linh kiện có điểm tương đồng dưới 70%.*  
> *2. **Về rào chắn chống Hallucination (Safe Guardrails theo Slide 18):** AI chỉ đóng vai trò tìm kiếm tương đồng ngữ nghĩa. Sau khi lấy được danh sách ứng viên, code Java của em trực tiếp đọc giá bán và tồn kho thực tế (`stockOnHand`) từ cơ sở dữ liệu qua Spring Data JPA, đồng thời kiểm tra tương thích phần cứng (ví dụ: Mainboard socket LGA1700 thì không bao giờ ghép với CPU socket AM5). Do đó, hệ thống không bao giờ bị tình trạng AI bịa giá hoặc đề xuất linh kiện hết hàng."*

---

### CÂU HỎI 5 (DÀNH CHO BÙI NGỌC BỬU):
**Giảng viên hỏi:** *"Sản phẩm gợi ý hiển thị ở đâu trên giao diện? Nếu Admin thêm mới hoặc sửa giá linh kiện thì dữ liệu Vector có được cập nhật không?"* (Thuộc Slide 10, 15)

> **Kịch bản trả lời lưu loát của Bửu:**  
> *"Dạ thưa Cô, phần hiển thị và quản trị dữ liệu vector do em phụ trách:*  
> *1. **Về giao diện người dùng:** Em xây dựng component `PersonalizedRecommendations.jsx` đặt tại trang chủ `HomePage.jsx` và trang `CartPage.jsx`. Khi khách truy cập, component tự động gọi API `/api/v1/recommendations/personalized` để lấy danh sách linh kiện gợi ý phù hợp nhất với hành vi duyệt web của khách và render dưới dạng thẻ sản phẩm mượt mà kèm nhãn độ tương thích.*  
> *2. **Về cập nhật dữ liệu Vector phía Admin:** Tại trang Quản trị sản phẩm `AdminProductsPage.jsx`, em đã tích hợp thêm một nút chức năng **'Đồng bộ Vector AI'**. Khi người quản trị thêm một loạt CPU hoặc VGA mới, chỉ cần nhấn nút này, hệ thống sẽ kích hoạt hàm `reindexAll()` bên service của bạn Văn Nhựt để băm lại thông số và nạp vector mới vào Vector Store ngay lập tức."*

---

## 5. QUY TRÌNH PHỐI HỢP GIT ĐẢM BẢO ZERO CONFLICT

Để cả 5 thành viên cùng commit và push code mà **không bao giờ bị Conflict mã nguồn**, nhóm tuân thủ nghiêm ngặt quy tắc sau:

1. **Nguyên tắc tạo nhánh độc lập từ `development`:**
   - Võ Văn Cảnh: `feature/canh-user-tracking-ui`
   - Huỳnh Đoàn Nhân: `feature/nhan-tracking-storage`
   - Văn Nhựt: `feature/nhut-spring-ai-vector`
   - Lê Thị Kim Ngân: `feature/ngan-semantic-recommendation`
   - Bùi Ngọc Bửu: `feature/buu-recommendation-ui`

2. **Ranh giới file cấm sửa chéo:**
   - Mỗi thành viên chỉ tạo mới file trong package/thư mục được phân công ở mục 2.2.
   - Không ai được chỉnh sửa các file thuộc trách nhiệm của bạn khác khi chưa có sự đồng thuận.

3. **Thứ tự tích hợp (Merge Order) về nhánh `development`:**
   - **Bước 1:** Nhân merge migration `V15` và Entity Tracking $\rightarrow$ Nhựt merge `VectorStoreConfig` và `ProductChunker`.
   - **Bước 2:** Cảnh merge `useUserTracker.js` $\rightarrow$ Ngân merge `ProductSemanticRecommendationService.java`.
   - **Bước 3:** Bửu merge `PersonalizedRecommendations.jsx` và nút Admin.
   - **Bước 4:** Cả nhóm chạy `.\mvnw.cmd test-compile` và chạy test giao diện trước khi bàn giao hoàn tất.

