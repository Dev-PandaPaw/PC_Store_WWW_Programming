# BÁO CÁO CHUYÊN ĐỀ & KỊCH BẢN THUYẾT TRÌNH
## ỨNG DỤNG SPRING AI, VECTOR DATABASE VÀ CƠ CHẾ USER TRACKING TRONG DỰ ÁN PC STORE

- **Đơn vị:** Trường Đại học Công nghiệp TP. Hồ Chí Minh (IUH) — Khoa Công nghệ Thông tin
- **Môn học:** Lập trình WWW (Java / Spring Boot + React)
- **Đề tài bài tập lớn:** Hệ thống Website Bán Máy tính & Linh kiện PC Store (`PC_Store_WWW_Programming`)
- **Sinh viên thực hiện:** Võ Văn Cảnh (Đại diện Nhóm 16)
- **Nội dung:** 
  1. Nghiên cứu lý thuyết chuyên sâu: Spring AI, Vector Databases, Thuật toán Cosine Similarity, Front-end User Tracking.
  2. Phương án thiết kế & áp dụng trực tiếp vào dự án PC Store.
  3. Kịch bản trả lời chất vấn & phản biện chi tiết (Code nằm ở đâu, tính toán ra sao, băm thế nào, lưu ở đâu).

---

## MỤC LỤC
1. [PHẦN 1: NỀN TẢNG LÝ THUYẾT CHUYÊN SÂU](#phần-1-nền-tảng-lý-thuyết-chuyên-sâu)
   - 1.1. Spring AI trong hệ sinh thái Spring Boot Enterprise
   - 1.2. Vector Embeddings & Vector Databases
   - 1.3. Kiến trúc RAG (Retrieval Augmented Generation) & Pipeline Ingestion (ETL)
   - 1.4. Thuật toán Cosine Similarity (Toán học & Ứng dụng)
   - 1.5. Cơ chế Tool Calling (Function Calling) và rào cản bảo mật (Guardrails)
   - 1.6. Cơ chế User Tracking trên Giao diện Front-end (UI)
2. [PHẦN 2: THIẾT KẾ VÀ ÁP DỤNG VÀO DỰ ÁN PC STORE](#phần-2-thiết-kế-và-áp-dụng-vào-dự-án-pc-store)
   - 2.1. Bài toán thực tế được lựa chọn
   - 2.2. Sơ đồ luồng xử lý tổng thể (Architecture Flow)
   - 2.3. Thiết kế Cơ sở dữ liệu: Vector Store & User Tracking Logs
   - 2.4. Thiết kế Ingestion (Băm và Vector hóa linh kiện PC)
   - 2.5. Thiết kế Front-end Tracker (Non-blocking Event Collector)
3. [PHẦN 3: BỘ CÂU HỎI PHẢN BIỆN CHẤT VẤN CỦA GIẢNG VIÊN (CHEAT SHEET)](#phần-3-bộ-câu-hỏi-phản-biện-chất-vấn-của-giảng-viên-cheat-sheet)
   - Câu 1: Code đoạn tính toán nằm ở đâu? Tính toán cái gì?
   - Câu 2: Lưu trữ ở đâu, bảng nào, cột nào, kiểu dữ liệu gì?
   - Câu 3: Băm văn bản (Chunking) ra sao? Tại sao phải băm?
   - Câu 4: Cơ chế User Tracking ở UI hoạt động thế nào? Có làm chậm web không?
   - Câu 5: Sự khác nhau giữa Tìm kiếm Vector (Semantic Search) và Tìm kiếm từ khóa (Keyword/SQL LIKE)?
   - Câu 6: Làm sao bảo đảm AI không "chém gió" giá bán và tồn kho (Hallucination)?
   - Câu 7: Khi nào dùng RAG, khi nào dùng Tool Calling?
4. [PHẦN 4: KẾ HOẠCH HIỆN THỰC VÀ BÀN GIAO MÃ NGUỒN](#phần-4-kế-hoạch-hiện-thực-và-bàn-giao-mã-nguồn)

---

# PHẦN 1: NỀN TẢNG LÝ THUYẾT CHUYÊN SÂU

### 1.1. Spring AI trong hệ sinh thái Spring Boot Enterprise
- **Bối cảnh:** Trước đây, hầu hết các ứng dụng Generative AI và RAG đều xây dựng bằng Python (qua LangChain, LlamaIndex). Điều này tạo ra rào cản lớn khi tích hợp vào các hệ thống doanh nghiệp (Enterprise) vốn vận hành trên nền tảng Java/Spring Boot (ngân hàng, tài chính, thương mại điện tử).
- **Mục tiêu của Spring AI:** Cung cấp lớp trừu tượng (Abstraction Layer) thân thuộc của Spring (như `Spring Data`, `Spring Security`, `RestClient`) cho các mô hình AI:
  - Cung cấp `ChatClient` theo phong cách Fluent API (tương tự `RestClient` hoặc `WebClient`).
  - Hỗ trợ đổi linh hoạt giữa các nhà cung cấp mô hình (Google Gemini, OpenAI, Ollama, Anthropic) chỉ qua cấu hình `application.yml` mà không phải sửa code nghiệp vụ.
  - Tích hợp chuẩn hoá với `PromptTemplate`, `Advisors`, `VectorStore`, và `@Tool Calling`.

---

### 1.2. Vector Embeddings & Vector Databases
- **Vector Embeddings là gì?**
  Là quá trình chuyển đổi dữ liệu phi cấu trúc (văn bản mô tả PC, thông số kỹ thuật card màn hình, câu hỏi của khách hàng) thành một **mảng số thực nhiều chiều (Dense Vector)**.
  - Mỗi chiều vector đại diện cho một thuộc tính ngữ nghĩa tiềm ẩn (Latent Semantic Feature).
  - Hai câu văn có từ ngữ khác nhau nhưng mang cùng ý nghĩa (ví dụ: *"PC chiến mượt Wukong"* và *"Máy tính chơi tốt game AAA nặng"*) sẽ có các vector nằm rất gần nhau trong không gian vector.
- **Vector Database là gì?**
  Khác với CSDL quan hệ truyền thống (tìm kiếm chính xác bằng B-Tree hoặc Index chuỗi), Vector Database là hệ cơ sở dữ liệu chuyên dụng để:
  - Lưu trữ các vector nhiều chiều kèm metadata (thông tin đính kèm).
  - Thực hiện tìm kiếm các vector lân cận gần nhất (**K-Nearest Neighbors - KNN** hoặc **Approximate Nearest Neighbors - ANN**) bằng các thuật toán HNSW (Hierarchical Navigable Small World) hoặc IVFFlat.
  - Các giải pháp phổ biến: **PostgreSQL với extension `pgvector`**, **Qdrant**, **Weaviate**, **Redis**.

---

### 1.3. Kiến trúc RAG (Retrieval Augmented Generation) & Ingestion Pipeline
RAG giải quyết bài toán lớn nhất của các mô hình ngôn ngữ lớn (LLM): **Hiện tượng ảo giác (Hallucination)** và **Thiếu kiến thức nội bộ / dữ liệu thời gian thực**.

Kiến trúc RAG bao gồm 2 chu trình độc lập (theo Slide 12):

```
[CHUYỂN HÓA DỮ LIỆU - INGESTION ETL]
Document Reader -> Transformer (Chunking/Băm) -> Embedding Model -> Vector Store

[TRUY VẤN THỜI GIAN THỰC - QUERY TIME]
User Query -> Embed Question -> VectorStore Search (Cosine) -> Augment Prompt -> LLM Answer
```

1. **Chu trình Ingestion (ETL Dữ liệu):**
   - **Document Reader:** Đọc toàn bộ danh mục sản phẩm từ Database (Tên linh kiện, thông số CPU/GPU/RAM, mức giá, nhu cầu phù hợp).
   - **Transformer (Chunking / Băm):** Cắt nhỏ văn bản thành các đoạn (chunks) có kích thước vừa phải (thường từ 300 - 500 tokens). Giữa các đoạn có một khoảng gối đầu (**Overlap**, khoảng 50 tokens) để không làm đứt mạch ngữ nghĩa.
   - **Embedding Model:** Đưa từng chunk qua model Embedding (như Google `text-embedding-004` với 768 chiều hoặc OpenAI `text-embedding-3-small` với 1536 chiều) để xuất ra Vector.
   - **Vector Store:** Ghi vector và metadata vào database (PostgreSQL `pgvector`).
2. **Chu trình Query Time (Truy vấn thời gian thực):**
   - Người dùng gửi câu hỏi: *"Tìm dàn PC tầm 20 triệu vừa vẽ đồ họa vừa chơi game"*.
   - Hệ thống dùng chính Embedding Model để biến câu hỏi thành **Vector truy vấn (Query Vector)**.
   - Tìm trong Vector Store các sản phẩm có vector tương đồng nhất bằng **Cosine Similarity**.
   - Lấy thông tin chi tiết các sản phẩm tìm được ghép vào Prompt (Context Augmentation).
   - LLM đọc Prompt đã có đầy đủ dữ liệu thực tế và sinh câu trả lời chính xác 100%.

---

### 1.4. Thuật toán Cosine Similarity (Toán học & Ứng dụng)

#### Công thức toán học (Slide 13):
Độ tương đồng Cosine đo góc $\theta$ giữa hai vector $A$ và $B$ trong không gian $n$ chiều:

$$\cos(\theta) = \frac{A \cdot B}{\|A\| \|B\|} = \frac{\sum_{i=1}^n A_i B_i}{\sqrt{\sum_{i=1}^n A_i^2} \cdot \sqrt{\sum_{i=1}^n B_i^2}}$$

- **Ý nghĩa hình học:**
  - $\theta \approx 0^\circ \implies \cos(\theta) \approx 1$: Hai vector chỉ cùng hướng $\rightarrow$ **Rất tương đồng về ngữ nghĩa**.
  - $\theta = 90^\circ \implies \cos(\theta) = 0$: Hai vector vuông góc $\rightarrow$ **Không liên quan gì đến nhau**.
  - $\theta = 180^\circ \implies \cos(\theta) = -1$: Hai vector đối nghịch hướng $\rightarrow$ **Trái ngược hoàn toàn**.

#### Ưu điểm vượt trội so với Khoảng cách Euclidean:
Cosine Similarity chỉ quan tâm đến **hướng của vector (ngữ nghĩa)** chứ không phụ thuộc vào **độ dài văn bản**. Một đoạn mô tả ngắn và một đoạn đánh giá dài có cùng ý nghĩa vẫn cho độ tương đồng gần bằng 1.

---

### 1.5. Cơ chế Tool Calling (Function Calling) và Guardrails Bảo mật
- **Nguyên tắc vàng (Slide 11 & 16):** LLM **không bao giờ** được kết nối trực tiếp vào CSDL hoặc gọi thẳng API nghiệp vụ.
- **Quy trình hoạt động:**
  1. Ứng dụng Spring Boot đăng ký danh sách các method mang annotation `@Tool` (ví dụ: `searchProducts`, `checkStock`, `calculatePowerSupply`).
  2. Spring AI gửi câu hỏi kèm định nghĩa các `@Tool` (tên hàm, tham số) cho LLM.
  3. LLM phân tích ý định và trả về yêu cầu: *"Cần gọi tool `searchProducts` với tham số `minPrice: 15000000, maxPrice: 20000000`"*.
  4. Spring Boot đón nhận yêu cầu, **chính mã Java** sẽ thực thi gọi xuống `ProductRepository` / `OrderService` an toàn dưới sự kiểm soát của Spring Security (`toolContext`).
  5. Kết quả thực tế từ DB được đưa ngược lại cho LLM để LLM trả lời khách hàng.
- **Rào cản an toàn (Guardrails - Slide 18):**
  - Danh tính người dùng lấy từ JWT/Authentication thông qua `toolContext`, không bao giờ để LLM tự quyết định ID tài khoản.
  - Giá và tồn kho luôn đọc lại từ DB thật, không tin giá do AI tự sinh.
  - Hành động nhạy cảm (tạo đơn hàng, trừ tiền) bắt buộc phải qua bước xác nhận người dùng (Confirm before commit).

---

### 1.6. Cơ chế User Tracking trên Giao diện Front-end (UI)
User Tracking là kỹ thuật thu thập hành vi tương tác của người dùng trên giao diện để phục vụ phân tích, tối ưu trải nghiệm và xây dựng hệ thống gợi ý cá nhân hóa (Personalization).

#### Các hành vi cốt lõi cần theo dõi trong E-commerce PC:
1. `VIEW_PRODUCT`: Khách vào xem trang chi tiết cấu hình máy tính nào.
2. `VIEW_DURATION`: Khách dừng lại đọc thông số kỹ thuật của linh kiện/PC đó trong bao lâu (tính bằng giây).
3. `SPEC_CLICK`: Khách có click mở rộng thông số kỹ thuật (ví dụ click xem card RTX 4060, click xem tản nhiệt nước) hay không.
4. `SEARCH_QUERY`: Các từ khóa tìm kiếm gần nhất.
5. `ADD_TO_CART`: Thêm vào giỏ hàng (thể hiện mức độ quan tâm cao nhất).

#### Kỹ thuật triển khai Front-end đảm bảo Performance (Non-blocking):
- **Không dùng blocking fetch:** Tuyệt đối không gửi request đồng bộ làm đơ giao diện hoặc chậm thao tác click của người dùng.
- **Sử dụng `navigator.sendBeacon()`:** 
  API chuyên dụng của trình duyệt giúp gửi dữ liệu thống kê ngầm về server. Đảm bảo dữ liệu **không bao giờ bị mất** ngay cả khi người dùng bấm tắt tab, chuyển trang hoặc F5 trình duyệt.
- **Cơ chế Batching / Throttling:** Gom nhóm các sự kiện (ví dụ sau mỗi 5 giây hoặc khi gom đủ 5 sự kiện) mới gửi 1 payload về server để tránh làm nghẽn mạng.
- **Quản lý danh tính (Session Tracking):**
  - Khách vãng lai: Tạo `anonymous_session_id` ngẫu nhiên lưu trong `sessionStorage`.
  - Khách đã đăng nhập: Tự động đính kèm `userId` lấy từ `AuthContext`.

---

# PHẦN 2: THIẾT KẾ VÀ ÁP DỤNG VÀO DỰ ÁN PC STORE

### 2.1. Bài toán thực tế được lựa chọn
Trong đề tài **PC Store**, chúng ta áp dụng vào 2 phân hệ:
1. **Trợ lý tư vấn cấu hình PC thông minh (AI PC Builder Assistant):**
   - Khách hàng không cần có kiến thức sâu về phần cứng (không cần biết socket 1700, DDR4/DDR5 hay công suất nguồn).
   - Chỉ cần chat nhu cầu tự nhiên: *"Tư vấn cho mình dàn PC tầm 18 triệu để học lập trình web, thỉnh thoảng chơi Valorant"*.
   - RAG + Vector Search sẽ tìm ra các bộ linh kiện phù hợp nhất trong CSDL thực tế.
2. **Gợi ý sản phẩm thông minh dựa trên hành vi (Behavioral Recommendation Engine):**
   - Front-end ghi nhận hành vi user đang xem nhiều card đồ họa Nvidia RTX 40-series và màn hình tần số quét 144Hz.
   - Hệ thống tổng hợp thành **User Interest Vector** $\rightarrow$ Dùng **Cosine Similarity** quét trong Vector DB $\rightarrow$ Hiển thị mục *"Cấu hình đề xuất cho nhu cầu Gaming của bạn"* ngay tại Trang chủ.

---

### 2.2. Sơ đồ luồng xử lý tổng thể (Architecture Flow)

```
+-----------------------------------------------------------------------------------+
|                                FRONT-END (REACT)                                  |
|  - UI Component: AdvisoryChatBox, ProductDetailPage, CatalogPage                 |
|  - Hook: useUserTracker (Bắt event VIEW, CLICK, TIME_SPENT)                       |
|  - Cơ chế gửi: navigator.sendBeacon('/api/tracking/events')                       |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼ (HTTP REST / JSON)
+-----------------------------------------------------------------------------------+
|                           BACKEND (SPRING BOOT 4.1.2)                             |
|                                                                                   |
|  [Spring Security] -> Xác thực JWT / Session -> Gán UserContext                   |
|                                                                                   |
|  [UserTrackingController] -----> [UserTrackingService] -> [user_activity_logs DB] |
|                                         │ (Tổng hợp hành vi)                      |
|                                         ▼                                         |
|  [AiAdvisoryController]  -----> [VectorSearchService]                             |
|          │                              │                                         |
|          │ (ChatClient)                 │ (Cosine Similarity: <=>)                |
|          ▼                              ▼                                         |
|  [Spring AI ChatClient] <-----> [PostgreSQL + PGVECTOR Extension]                 |
|          │                      - Bảng vector_store (embedding: vector(768))      |
|          │                                                                        |
|          ▼ (Tool Calling: checkStock, getLivePrice)                               |
|  [ProductTools] ----------> [ProductRepository] -> [products DB]                  |
|                                                                                   |
|          ▼                                                                        |
|  [Google Gemini Model] (genai-chat)                                               |
+-----------------------------------------------------------------------------------+
```

---

### 2.3. Thiết kế Cơ sở dữ liệu: Vector Store & User Tracking

#### 1. Bảng lưu trữ Vector (`vector_store` với PostgreSQL pgvector):
```sql
-- Kích hoạt extension pgvector trong PostgreSQL
CREATE EXTENSION IF NOT EXISTS vector;

-- Bảng Vector Store lưu embeddings của toàn bộ sản phẩm PC & Linh kiện
CREATE TABLE vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,                  -- Đoạn mô tả cấu hình đã băm (chunk text)
    metadata JSONB,                         -- Metadata: {"productId": 101, "price": 18500000, "category": "PC_GAMING"}
    embedding VECTOR(768)                   -- Vector 768 chiều (tương thích model text-embedding-004 của Google)
);

-- Tạo Index HNSW để tăng tốc độ tìm kiếm Cosine Similarity
CREATE INDEX idx_vector_store_embedding_cosine 
ON vector_store USING hnsw (embedding vector_cosine_ops);
```

#### 2. Bảng ghi nhận hành vi (`user_activity_logs`):
```sql
CREATE TABLE user_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT,                         -- NULL nếu là khách vãng lai
    event_type VARCHAR(32) NOT NULL,        -- 'VIEW_PRODUCT', 'TIME_SPENT', 'SPEC_CLICK', 'ADD_TO_CART'
    product_id BIGINT REFERENCES products(id),
    category_code VARCHAR(32),
    duration_seconds INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tracking_session_user ON user_activity_logs(session_id, user_id);
```

---

### 2.4. Thiết kế Ingestion (Băm và Vector hóa linh kiện PC)

File Java: `ProductEmbeddingService.java`
```java
@Service
public class ProductEmbeddingService {

    private final ProductRepository productRepository;
    private final VectorStore vectorStore; // Bean Spring AI PgVectorStore hoặc SimpleVectorStore

    public ProductEmbeddingService(ProductRepository productRepository, VectorStore vectorStore) {
        this.productRepository = productRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * Băm và nạp toàn bộ danh mục sản phẩm vào Vector Database
     */
    public void ingestAllProducts() {
        List<Product> products = productRepository.findAllActive();
        List<Document> documents = new ArrayList<>();

        for (Product p : products) {
            // 1. Tạo chuỗi nội dung ngữ nghĩa đầy đủ của sản phẩm
            String semanticText = String.format(
                "Sản phẩm: %s. Danh mục: %s. Giá: %d VNĐ. Cấu hình chi tiết: CPU %s, Card đồ họa VGA %s, " +
                "Bộ nhớ RAM %s, Ổ cứng lưu trữ %s. Nguồn PSU %s. Mục đích sử dụng phù hợp: %s.",
                p.getName(), p.getCategory().getName(), p.getPrice(),
                p.getSpec().getCpu(), p.getSpec().getVga(),
                p.getSpec().getRam(), p.getSpec().getStorage(),
                p.getSpec().getPsu(), p.getSpec().getTargetUsage()
            );

            // 2. Metadata đính kèm để lọc cứng (Filter) sau khi tìm kiếm vector
            Map<String, Object> metadata = Map.of(
                "productId", p.getId(),
                "sku", p.getSku(),
                "price", p.getPrice(),
                "category", p.getCategory().getCode()
            );

            documents.add(new Document(semanticText, metadata));
        }

        // 3. Băm văn bản (Chunking) bằng TokenTextSplitter
        TokenTextSplitter splitter = new TokenTextSplitter(500, 50, 10, 5000, true);
        List<Document> chunks = splitter.apply(documents);

        // 4. Đưa qua Embedding Model và lưu vào Vector Store
        vectorStore.add(chunks);
    }
}
```

---

### 2.5. Thiết kế Front-end Tracker (`useUserTracker.js`)

File React Hook: `frontend/src/hooks/useUserTracker.js`
```javascript
import { useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';

export function useUserTracker(productId, categoryCode) {
  const { user } = useAuth();
  const startTimeRef = useRef(Date.now());

  // Lấy hoặc tạo Anonymous Session ID
  const getSessionId = () => {
    let sid = sessionStorage.getItem('pc_store_tracker_session');
    if (!sid) {
      sid = 'sess_' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36);
      sessionStorage.setItem('pc_store_tracker_session', sid);
    }
    return sid;
  };

  const sendEvent = (eventType, extraData = {}) => {
    const payload = {
      sessionId: getSessionId(),
      userId: user?.id || null,
      eventType: eventType,
      productId: productId || null,
      categoryCode: categoryCode || null,
      ...extraData,
      timestamp: new Date().toISOString()
    };

    const blob = new Blob([JSON.stringify(payload)], { type: 'application/json' });
    
    // Ưu tiên dùng navigator.sendBeacon (Không chặn UI, chạy ngầm, an toàn khi đóng tab)
    if (navigator.sendBeacon) {
      navigator.sendBeacon('/api/tracking/events', blob);
    } else {
      fetch('/api/tracking/events', {
        method: 'POST',
        body: blob,
        keepalive: true
      }).catch(() => {});
    }
  };

  useEffect(() => {
    // 1. Ghi nhận sự kiện xem sản phẩm (VIEW_PRODUCT)
    if (productId) {
      sendEvent('VIEW_PRODUCT');
    }

    // 2. Ghi nhận thời gian xem trang (VIEW_DURATION) khi rời khỏi component
    return () => {
      const durationSeconds = Math.round((Date.now() - startTimeRef.current) / 1000);
      if (productId && durationSeconds > 2) {
        sendEvent('TIME_SPENT', { durationSeconds });
      }
    };
  }, [productId]);

  return { trackCustomEvent: sendEvent };
}
```

---

# PHẦN 3: BỘ CÂU HỎI PHẢN BIỆN CHẤT VẤN CỦA GIẢNG VIÊN (CHEAT SHEET)

Khi báo cáo, giảng viên thường sẽ hỏi rất sâu vào các chi tiết kỹ thuật. Cảnh hãy sử dụng bộ câu trả lời dưới đây để đối đáp chuẩn xác từng thuật ngữ:

---

### Câu 1: "Code đoạn tính toán nằm ở đâu? Tính toán cái gì?"
- **Trả lời ngắn gọn, dứt khoát:**
  > *"Dạ thưa cô, hệ thống tính toán **Cosine Similarity** giữa vector câu hỏi của khách hàng và vector sản phẩm lưu trong kho.*
  > - **Nếu dùng CSDL PostgreSQL (Extension `pgvector`):** Đoạn tính toán nằm ngay ở tầng **Database Engine** thông qua toán tử khoảng cách cosine `<=>` trong câu lệnh SQL. Bản chất toán tử này thực hiện phép tính Dot Product chia cho Norm L2 của 2 vector.
  > - **Nếu dùng Java In-memory (`SimpleVectorStore`):** Nằm tại class `SimpleVectorStore.java` của Spring AI trong method `cosineSimilarity(float[] v1, float[] v2)`:
  >   ```java
  >   float dotProduct = 0.0f, normA = 0.0f, normB = 0.0f;
  >   for (int i = 0; i < v1.length; i++) {
  >       dotProduct += v1[i] * v2[i];
  >       normA += v1[i] * v1[i];
  >       normB += v2[i] * v2[i];
  >   }
  >   return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
  >   ```
  >   Kết quả trả về giá trị từ -1 đến 1. Điểm càng gần 1 thì sản phẩm càng khớp với nhu cầu của khách."*

---

### Câu 2: "Lưu trữ ở đâu, chỗ nào, bảng nào, cột nào, kiểu dữ liệu gì?"
- **Trả lời:**
  > *"Dạ thưa cô, dữ liệu được lưu trữ tại cơ sở dữ liệu **PostgreSQL**:*
  > - *Bảng:* **`vector_store`**.
  > - *Cột chứa tọa độ ngữ nghĩa:* **`embedding`**, kiểu dữ liệu là **`VECTOR(768)`** (nếu dùng Google Gemini `text-embedding-004`) hoặc **`VECTOR(1536)`** (nếu dùng OpenAI `text-embedding-3-small`).
  > - *Cột chứa nội dung text gốc:* **`content`**, kiểu `TEXT` (lưu mô tả cấu hình đã băm).
  > - *Cột chứa thông tin lọc nghiệp vụ:* **`metadata`**, kiểu `JSONB` (chứa `productId`, `price`, `stock`, `category`).
  > - *Chỉ mục tăng tốc (Index):* Sử dụng giải thuật **`HNSW`** (Hierarchical Navigable Small World) để tìm kiếm lân cận cực nhanh mà không cần quét toàn bộ bảng."*

---

### Câu 3: "Băm văn bản (Chunking) ra sao? Tại sao phải băm?"
- **Trả lời:**
  > *"Dạ thưa cô, quá trình băm nằm trong bước **Ingestion ETL**:*
  > - **Tại sao phải băm?**
  >   1. Các model Embedding có giới hạn kích thước ngữ cảnh (Context Window limit, ví dụ 512 hoặc 2048 tokens).
  >   2. Nếu không băm mà để nguyên cả trang dài, ý nghĩa của các linh kiện nhỏ sẽ bị 'pha loãng' (diluted), làm giảm độ chính xác khi tìm kiếm. Băm nhỏ giúp mỗi vector nắm bắt chính xác một đặc tính cụ thể của máy tính.
  > - **Băm như thế nào?**
  >   Sử dụng class **`TokenTextSplitter`** của Spring AI:
  >   - `chunkSize`: **500 tokens** (đủ chứa toàn bộ thông số 1 bộ PC gồm CPU, RAM, VGA, Ổ cứng, Nguồn).
  >   - `chunkOverlap`: **50 tokens** (khoảng gối đầu 10% giữa 2 chunk liên tiếp để câu từ không bị chặt đứt nửa chừng và giữ được liên kết ngữ nghĩa)."*

---

### Câu 4: "Cơ chế User Tracking ở UI hoạt động thế nào? Có làm chậm web không?"
- **Trả lời:**
  > *"Dạ thưa cô, cơ chế User Tracking ở Front-end được thiết kế theo nguyên tắc **Non-blocking (không phong tỏa luồng giao diện)**:*
  > 1. Viết một custom hook React **`useUserTracker.js`** gắn vào `ProductDetailPage` và `CatalogPage`.
  > 2. Hook này lắng nghe các hành vi: click sản phẩm, xem cấu hình, và đếm thời gian khách dừng lại đọc thông số kỹ thuật (`durationSeconds`).
  > 3. **Giải pháp không làm chậm web:** Sử dụng phương thức **`navigator.sendBeacon('/api/tracking/events', data)`**. Trình duyệt sẽ xếp request vào hàng đợi ngầm ở tầng network của hệ điều hành để gửi đi, hoàn toàn không chiếm dụng Main Thread của React và không làm giảm FPS hay giật lag giao diện.
  > 4. Ngay cả khi người dùng bấm tắt tab hoặc chuyển trang đột ngột, `sendBeacon` vẫn đảm bảo gói tin tracking được gửi tới server thành công."*

---

### Câu 5: "Sự khác nhau giữa Tìm kiếm Vector (Semantic Search) và Tìm kiếm SQL truyền thống (LIKE / Full-Text Search)?"
- **Trả lời:**
  > *"Dạ thưa cô, sự khác biệt căn bản là:**Khớp từ khóa (Syntax) vs Hiểu ý nghĩa (Semantics)**:*
  > - **SQL LIKE / Full-Text Search:** Chỉ tìm thấy nếu người dùng gõ đúng từ có trong database. Nếu khách gõ *'máy tính làm đồ họa mượt'*, SQL LIKE `%đồ họa%` sẽ trả về rỗng nếu trong mô tả máy chỉ ghi thông số kỹ thuật *'CPU Intel Core i7 14700K, VGA RTX 4070 12GB'*.
  > - **Vector Semantic Search:** Chuyển câu hỏi của khách thành vector. Không gian vector hiểu rằng khái niệm *'làm đồ họa mượt'* có tọa độ rất gần với *'RTX 4070'* và *'i7 14700K'*. Do đó hệ thống vẫn gợi ý chính xác bộ máy tính đó dù trong bài viết không hề có chữ 'đồ họa'."*

---

### Câu 6: "Làm sao bảo đảm AI không 'chém gió' giá bán và tồn kho (Hallucination)?"
- **Trả lời:**
  > *"Dạ thưa cô, hệ thống áp dụng **Safe Ordering Guardrails (Slide 18)**:*
  > 1. **Prompt Grounding:** LLM được yêu cầu bằng System Prompt: *'Chỉ được tư vấn dựa trên danh sách sản phẩm được cung cấp trong Context, tuyệt đối không tự bịa mã sản phẩm hay giá bán'*.
  > 2. **Tool Calling xác thực lại:** Khi AI chuẩn bị chốt cấu hình hoặc khách muốn đặt hàng, AI bắt buộc phải gọi tool `@Tool checkProductAvailability(productId)` do Java thực thi.
  > 3. **Java kiểm soát 100% dữ liệu tài chính:** Giá tiền, số lượng tồn kho và tạo đơn hàng đều do Service của Spring Boot đọc trực tiếp từ database PostgreSQL thông qua `@Transactional`. AI chỉ đóng vai trò tư vấn ngôn ngữ, không có quyền can thiệp vào logic thanh toán."*

---

### Câu 7: "Khi nào dùng RAG, khi nào dùng Tool Calling?"
- **Trả lời:**
  > *"Dạ thưa cô:*
  > - **Dùng RAG khi:** Cần cung cấp **kiến thức, thông tin tham khảo, tài liệu tĩnh hoặc bán tĩnh** cho mô hình (ví dụ: cẩm nang hướng dẫn build PC, tài liệu yêu cầu cấu hình tối thiểu của các tựa game như Black Myth Wukong, chính sách bảo hành 1 đổi 1 của cửa hàng).
  > - **Dùng Tool Calling khi:** Cần **hành động thực tế (Action), tính toán logic hoặc truy vấn dữ liệu thời gian thực** (ví dụ: kiểm tra xem trong kho hiện tại còn bao nhiêu chiếc RTX 4060, tính tổng công suất nguồn cần thiết cho linh kiện, hoặc gọi API tạo đơn hàng COD)."*

---

# PHẦN 4: KẾ HOẠCH HIỆN THỰC VÀ BÀN GIAO MÃ NGUỒN

Để đưa toàn bộ phần này vào chạy mượt mà trong dự án `PC_Store_WWW_Programming`:

1. **Bước 1: Sửa các lỗi import test do đợt refactor package catalog gần đây** (`CartServiceTest`, `CartStorageServiceTest`, `GameAdviceTest`). Đảm bảo `mvn test-compile` pass 100%.
2. **Bước 2: Cấu hình Spring AI Vector Store:**
   - Khai báo dependency `spring-ai-pgvector-store` (hoặc `SimpleVectorStore` in-memory cho môi trường demo không cần cài extension CSDL).
   - Tạo `ProductEmbeddingService` thực hiện Ingestion toàn bộ catalog PC hiện có.
3. **Bước 3: Hiện thực Front-end Tracking:**
   - Tạo hook `frontend/src/hooks/useUserTracker.js`.
   - Gắn hook vào `ProductDetailPage.jsx` và `CatalogPage.jsx`.
   - Tạo controller `UserTrackingController.java` để hứng event.
4. **Bước 4: Nâng cấp AI Advisor:**
   - Kết nối `ChatClient` của Spring AI với Vector Store để kích hoạt Semantic Search theo đúng Slide 15 & 16.
5. **Bước 5: Kiểm thử và Demo:**
   - Chạy kịch bản hỏi đáp tự nhiên trên giao diện web.
   - Trình chiếu file báo cáo này cho Giảng viên.

