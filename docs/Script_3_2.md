# 📋 Script Báo Cáo — Nhiệm Vụ 3.2: Backend Activity Storage & User Profile API

> **Sinh viên:** Huỳnh Đoàn Nhân  
> **Nhiệm vụ:** 3.2 — Lưu trữ hoạt động người dùng & API thống kê hành vi  
> **Ngôn ngữ:** Java / Spring Boot / Spring Data JPA / PostgreSQL (Supabase)

---

## 🗺️ Tổng quan luồng dữ liệu

```
[Trình duyệt người dùng]
        |
        | HTTP POST /api/tracking/events
        ↓
[UserTrackingController]  ← Điểm giao với Frontend (3.1 - Võ Văn Cảnh)
        |
        | Gọi logEvent()
        ↓
[UserTrackingService]     ← Bộ não xử lý nghiệp vụ
        |
        | Gọi save() / findTop...()
        ↓
[UserActivityLogRepository] ← Interface chứa các truy vấn SQL
        |
        | Hibernate dịch → INSERT INTO / SELECT
        ↓
[Database: Supabase PostgreSQL]
   Bảng: user_activity_logs
```

---

## 📌 PHẦN 1 — Class giao tiếp với Frontend (3.1)

### Class: `UserTrackingController.java`

📁 **Đường dẫn:** `src/main/java/.../tracking/controller/UserTrackingController.java`

**Đây là điểm giao duy nhất giữa nhiệm vụ 3.2 (backend) và 3.1 (frontend của Võ Văn Cảnh).**

Controller này mở ra **2 endpoint REST API**:

| Endpoint | Method | Chức năng |
|---|---|---|
| `/api/tracking/events` | `POST` | Nhận event từ `tracking.js` của Cảnh gửi lên |
| `/api/tracking/profile` | `GET` | Trả về thống kê hành vi người dùng (top categories, top products) |

### Cách Controller phân biệt khách hay người dùng đăng nhập:

```java
// Nếu chưa đăng nhập → dùng sessionId từ cookie/header
String sessionId = request.getSession().getId();

// Nếu đã đăng nhập → lấy userId qua CartUserIdentity
UUID userId = cartUserIdentity.resolveUserId(); // null nếu khách
```

Controller **không tự xử lý nghiệp vụ** — nó chỉ nhận request, xác định danh tính người dùng, rồi **gọi xuống Service** để xử lý.

---

### ❓ Q&A — Phần 1 (Giáo viên có thể hỏi)

**Q: Class nào nhận request từ Cảnh (3.1) gửi lên?**  
> A: Dạ, class `UserTrackingController` ạ. Đây là REST Controller duy nhất trong nhiệm vụ 3.2 lộ ra endpoint `/api/tracking/events` để frontend gửi event lên.

**Q: Tại sao trong F12 thấy 2 request "events" khi vào trang sản phẩm?**  
> A: Dạ, vì khi vào trang sản phẩm, frontend (tracking.js của Cảnh) gửi 2 event riêng biệt: một event `VIEW_PRODUCT` cho sản phẩm, và một event `FILTER_CATEGORY` cho danh mục sản phẩm. Mỗi event là một HTTP POST riêng nên F12 thấy 2 request ạ.

**Q: Controller có xử lý logic không?**  
> A: Dạ không ạ. Controller chỉ nhận request, validate input, xác định danh tính người dùng rồi chuyển xuống Service. Việc xử lý nghiệp vụ là do `UserTrackingService` đảm nhiệm ạ.

---

## 📌 PHẦN 2 — Các Class Còn Lại (Theo Thứ Tự Luồng Dữ Liệu)

### 2.1 — `TrackingEventRequest.java` — DTO (Bao thư dữ liệu)

📁 `src/main/java/.../tracking/dto/TrackingEventRequest.java`

**DTO = Data Transfer Object** — đây là "bao thư" đóng gói dữ liệu mà frontend gửi lên.

```java
public record TrackingEventRequest(
    @NotBlank String eventType,       // Loại sự kiện: "VIEW_PRODUCT", "FILTER_CATEGORY",...
    String productId,                  // UUID sản phẩm (nếu có)
    String categorySlug,               // Slug danh mục: "storage", "cpu",...
    String metadata,                   // JSON thêm: tên sản phẩm, giá,...
    String clientTimestamp             // Thời điểm sự kiện xảy ra ở client
)
```

**Nhiệm vụ:** Nhận dữ liệu từ HTTP request → validate (kiểm tra `eventType` không được rỗng) → chuyển vào Controller. Nếu dữ liệu không hợp lệ thì Spring tự trả lỗi `400 Bad Request` trước khi vào Controller.

---

### 2.2 — `UserActivityLog.java` — Entity (Khuôn mẫu 1 dòng trong Database)

📁 `src/main/java/.../tracking/entity/UserActivityLog.java`

**Entity** = khuôn mẫu ánh xạ 1-1 với 1 dòng trong bảng `user_activity_logs` trên Supabase.

Mỗi object `UserActivityLog` tương ứng với **1 hành động của người dùng** được lưu vào database, với các trường:

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| `id` | UUID | Khóa chính, tự sinh |
| `sessionId` | String | ID phiên — dùng cho khách chưa đăng nhập |
| `userId` | UUID | ID tài khoản — `null` nếu là khách |
| `eventType` | String | Loại sự kiện (xem Phần 7 bên dưới) |
| `productId` | UUID | Sản phẩm liên quan (nếu có) |
| `categorySlug` | String | Danh mục liên quan (nếu có) |
| `metadata` | String | JSON phụ: tên, giá,... |
| `createdAt` | Instant | Thời điểm lưu — hiển thị múi giờ `+07:00` |

> **Lưu ý:** `userId = null` khi khách chưa đăng nhập là **hoàn toàn bình thường và có chủ đích** — hệ thống vẫn theo dõi được hành vi qua `sessionId`.

---

### 2.3 — `UserTrackingService.java` — Service (Bộ Não Xử Lý)

📁 `src/main/java/.../tracking/service/UserTrackingService.java`

**Đây là class quan trọng nhất trong nhiệm vụ 3.2.**

Service làm các việc sau:

#### Khi nhận event (`logEvent()`):
1. **Chuẩn hóa dữ liệu:**
   - `eventType` → `.trim().toUpperCase()` (bỏ khoảng trắng, viết hoa)
   - `categorySlug` → `.trim().toLowerCase()` (bỏ khoảng trắng, viết thường)
2. **Tạo object `UserActivityLog`** từ dữ liệu đã chuẩn hóa
3. **Gọi Repository để lưu xuống DB:**
   ```java
   activityLogRepository.save(activityLog); // ← ĐÂY LÀ NƠI LƯU VÀO DATABASE
   ```

#### Khi lấy profile (`getUserProfile()`):
1. Tính mốc thời gian 7 ngày trước (dùng múi giờ `Asia/Ho_Chi_Minh`)
2. Gọi `findTopCategoriesBySessionOrUser()` → lấy top danh mục
3. Gọi `findTopProductIdsBySessionOrUser()` → lấy top sản phẩm
4. Trả kết quả về Controller → trả về Frontend

#### Phương thức để lại cho Ngân (3.4):
```java
public List<String> getTopViewedCategories(UUID userId, Instant since, int limit)
```
→ Ngân gọi hàm này để lấy danh mục ưa thích của user, đưa vào Spring AI prompt.

**Service CÓ gọi Repository** — cụ thể gọi 3 phương thức của `UserActivityLogRepository`.

---

### 2.4 — `V15__create_user_activity_logs.sql` — Script Tạo Bảng

📁 `src/main/resources/db/migration/V15__create_user_activity_logs.sql`

**Đây là file Flyway migration** — Spring Boot tự động chạy file này khi khởi động để tạo bảng trên Supabase.

```sql
CREATE TABLE user_activity_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  VARCHAR(255) NOT NULL,
    user_id     UUID,                    -- NULL nếu khách chưa đăng nhập
    event_type  VARCHAR(100) NOT NULL,
    product_id  UUID,
    category_slug VARCHAR(100),
    metadata    TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**3 Index tạo kèm để tăng tốc truy vấn:**

| Index | Cột | Mục đích |
|---|---|---|
| `idx_user_activity_session` | `session_id` | Tìm nhanh theo phiên khách |
| `idx_user_activity_user` | `user_id` | Tìm nhanh theo tài khoản |
| `idx_user_activity_created` | `created_at` | Lọc nhanh theo thời gian |

---

### ❓ Q&A — Phần 2 (Giáo viên có thể hỏi)

**Q: Tại sao cần DTO riêng (`TrackingEventRequest`) thay vì dùng thẳng Entity?**  
> A: Dạ, DTO và Entity có mục đích khác nhau ạ. DTO là bao thư nhận dữ liệu từ bên ngoài vào — chỉ chứa đúng những gì frontend gửi. Entity là khuôn mẫu để lưu vào DB — có thêm id, createdAt, ... Tách ra để bảo mật: frontend không thể tự gán id hay timestamp ạ.

**Q: `userId = null` trong DB có phải lỗi không?**  
> A: Dạ không phải lỗi ạ. Đó là thiết kế có chủ đích. Khách chưa đăng nhập không có userId, nên hệ thống theo dõi qua sessionId. Khi khách đăng nhập thì các event sau sẽ có userId đầy đủ ạ.

**Q: Flyway migration là gì?**  
> A: Dạ, Flyway là công cụ quản lý phiên bản database ạ. Mỗi file SQL có số version (V15, V16,...). Khi Spring Boot khởi động, Flyway tự động chạy các file chưa chạy để cập nhật schema. Giống như Git cho database ạ.

**Q: Index là gì, tại sao cần?**  
> A: Dạ, index là cấu trúc dữ liệu phụ giúp database tìm kiếm nhanh hơn, giống như mục lục trong sách ạ. Không có index, DB phải đọc từng dòng (table scan). Có index thì tìm theo session_id hay user_id chỉ mất O(log n) ạ.

---

## 📌 PHẦN 3 — Repository: Interface Chứa Các Truy Vấn SQL

### Class: `UserActivityLogRepository.java`

📁 `src/main/java/.../tracking/repository/UserActivityLogRepository.java`

> **⚠️ Quan trọng:** Đây là **INTERFACE**, **không phải class**. Em không cần viết code implement. Spring Data JPA tự động tạo code thực thi dựa trên tên phương thức và annotation `@Query`.

```java
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, UUID> {
    // 3 truy vấn tùy chỉnh bên dưới
}
```

---

### Truy Vấn 1 — `findTopCategoriesBySessionOrUser()`

**Dùng cho:** `GET /api/tracking/profile` → hiển thị top danh mục của người dùng

```java
@Query("""
    SELECT u.categorySlug, COUNT(u) as cnt
    FROM UserActivityLog u
    WHERE (u.sessionId = :sessionId OR u.userId = :userId)
      AND u.categorySlug IS NOT NULL
      AND u.createdAt >= :since
    GROUP BY u.categorySlug
    ORDER BY cnt DESC
    """)
List<Object[]> findTopCategoriesBySessionOrUser(
    String sessionId, UUID userId, Instant since, Pageable pageable);
```

**Ý nghĩa:** Nhóm tất cả log theo `categorySlug`, đếm số lần xuất hiện, sắp xếp từ nhiều → ít, lấy top N. Tìm theo cả `sessionId` lẫn `userId` để bao gồm cả lịch sử trước và sau đăng nhập.

---

### Truy Vấn 2 — `findTopViewedCategoriesByUser()` ⭐ (Quan trọng nhất)

**Dùng cho:** Ngân (3.4) gọi để lấy danh mục ưa thích → đưa vào Spring AI prompt gợi ý sản phẩm

```java
@Query("""
    SELECT u.categorySlug
    FROM UserActivityLog u
    JOIN Product p ON u.productId = p.id
    WHERE u.userId = :userId
      AND u.createdAt >= :since
      AND u.categorySlug IS NOT NULL
      AND p.status = 'ACTIVE'
      AND u.categorySlug IN ('cpu','ram','gpu','storage','psu','case','cooling','motherboard')
    GROUP BY u.categorySlug
    ORDER BY COUNT(u) DESC
    """)
List<String> findTopViewedCategoriesByUser(
    UUID userId, Instant since, Pageable pageable);
```

**Ý nghĩa:**
- `JOIN Product p ON u.productId = p.id` → chỉ lấy log có sản phẩm thực trong DB (chống giả mạo)
- `p.status = 'ACTIVE'` → chỉ tính sản phẩm đang bán
- `categorySlug IN ('cpu','ram',...)` → chỉ nhận danh mục linh kiện PC hợp lệ
- Kết quả: danh sách slug `["gpu", "cpu", "storage"]` — đưa vào AI prompt

---

### Truy Vấn 3 — `findTopProductIdsBySessionOrUser()`

**Dùng cho:** `GET /api/tracking/profile` → hiển thị top sản phẩm đã xem

```java
@Query("""
    SELECT u.productId
    FROM UserActivityLog u
    WHERE (u.sessionId = :sessionId OR u.userId = :userId)
      AND u.productId IS NOT NULL
      AND u.createdAt >= :since
    GROUP BY u.productId
    ORDER BY COUNT(u) DESC
    """)
List<UUID> findTopProductIdsBySessionOrUser(
    String sessionId, UUID userId, Instant since, Pageable pageable);
```

**Ý nghĩa:** Lấy top sản phẩm được xem nhiều nhất trong 7 ngày qua theo cả sessionId và userId. Trả về danh sách UUID → Service dùng để fetch thông tin sản phẩm.

---

### ❓ Q&A — Phần 3 (Giáo viên có thể hỏi)

**Q: Sao không lấy thẳng categorySlug mà phải JOIN với bảng Product (truy vấn 2)?**  
> A: Dạ vì nếu lấy thẳng categorySlug từ log thì có thể bị giả mạo ạ. Frontend gửi lên `categorySlug = "abc123"` thì hệ thống cũng lưu. Nhưng khi JOIN với bảng Product, chỉ những log nào có `productId` trỏ đến sản phẩm thực trong DB mới được tính. Thêm điều kiện `p.status = 'ACTIVE'` đảm bảo chỉ tính sản phẩm đang bán ạ.

**Q: Repository là class hay interface? Ai viết code implement?**  
> A: Dạ là interface ạ. Em chỉ cần khai báo chữ ký hàm và viết câu JPQL trong annotation `@Query`. Spring Data JPA sẽ tự động sinh ra code thực thi khi khởi động ạ.

**Q: JPQL khác SQL thường như thế nào?**  
> A: Dạ, SQL thông thường truy vấn theo tên bảng và tên cột trong database. JPQL truy vấn theo tên class Java và tên field Java. Hibernate sẽ dịch JPQL sang SQL thực tế phù hợp với từng loại database (PostgreSQL, MySQL,...) ạ.

---

## 📌 PHẦN 4 — Nơi Lưu Dữ Liệu Xuống Database

### Chỉ có **1 dòng code** lưu xuống DB:

```java
// Trong UserTrackingService.java — phương thức logEvent()

@Transactional
public TrackingEventResponse logEvent(String sessionId, UUID userId, TrackingEventRequest req) {
    // 1. Chuẩn hóa dữ liệu
    String eventType = req.eventType().trim().toUpperCase();

    // 2. Tạo entity
    UserActivityLog activityLog = new UserActivityLog();
    activityLog.setSessionId(sessionId);
    activityLog.setUserId(userId);
    activityLog.setEventType(eventType);
    // ... set các field khác

    // 3. ← ĐÂY LÀ NƠI LƯU VÀO DATABASE
    UserActivityLog saved = activityLogRepository.save(activityLog);

    return new TrackingEventResponse("success", saved.getId(), eventType);
}
```

### Luồng lưu dữ liệu chi tiết:

```
activityLogRepository.save(activityLog)
        |
        ↓ Spring Data JPA gọi Hibernate
        |
        ↓ Hibernate dịch sang SQL:
        |   INSERT INTO user_activity_logs
        |   (id, session_id, user_id, event_type, product_id, category_slug, metadata, created_at)
        |   VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, now())
        |
        ↓ Supabase PostgreSQL thực thi → Dữ liệu được lưu
```

### `@Transactional` đảm bảo gì?

Annotation `@Transactional` đảm bảo toàn bộ phương thức `logEvent()` chạy trong **1 transaction**:
- Nếu có lỗi xảy ra giữa chừng → database tự động **rollback** (hủy tất cả thay đổi)
- Nếu thành công → database **commit** (lưu vĩnh viễn)
- Đảm bảo dữ liệu **toàn vẹn** — không có trạng thái nửa vời

---

### ❓ Q&A — Phần 4 (Giáo viên có thể hỏi)

**Q: Em có làm gì liên quan đến Database không?**  
> A: Dạ có ạ. Em tạo bảng `user_activity_logs` trên Supabase thông qua Flyway migration (file V15). Em cũng viết 3 index để tối ưu truy vấn. Và trong code Java, em dùng `activityLogRepository.save()` trong Service để lưu từng event vào database mỗi khi người dùng thao tác trên web ạ.

**Q: Hibernate là gì trong luồng này?**  
> A: Dạ, Hibernate là ORM (Object-Relational Mapping) ạ. Nó làm cầu nối giữa Java và database. Em viết code Java với object `UserActivityLog`, Hibernate tự dịch sang câu lệnh SQL `INSERT INTO user_activity_logs ...` và gửi lên Supabase ạ.

**Q: Nếu Supabase bị mất kết nối thì data có bị mất không?**  
> A: Dạ, nhờ `@Transactional`, nếu kết nối bị ngắt giữa chừng thì transaction sẽ rollback ạ. Dữ liệu không bị lưu một nửa. Frontend sẽ nhận lỗi 500 và tracking.js có thể retry ạ.

---

## 📌 PHẦN 5 — Nhiệm Vụ 3.2 Để Lại Gì Cho Người Sau

### Bảng tổng kết những thứ để lại:

| Thứ để lại | Dành cho ai | Cách dùng |
|---|---|---|
| Phương thức `getTopViewedCategories()` | **Ngân (3.4)** — Spring AI | Gọi hàm → lấy danh mục ưa thích → đưa vào AI prompt |
| REST API `GET /api/tracking/profile` | **Bửu (3.5)** — Frontend profile | Gọi API → nhận JSON → hiển thị "Bạn hay xem: GPU, CPU" |
| Bảng `user_activity_logs` trên Supabase | **Toàn team** | Nguồn dữ liệu hành vi người dùng |
| `RecentCategoryConfiguration.java` | **Ngân (3.4)** | Spring Bean tự động inject vào service của Ngân |

---

### Chi tiết từng thứ để lại:

#### ① Phương thức `getTopViewedCategories()` → Ngân (3.4)

```java
// Trong UserTrackingService.java
public List<String> getTopViewedCategories(UUID userId, Instant since, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    return activityLogRepository.findTopViewedCategoriesByUser(userId, since, pageable);
}
// Trả về ví dụ: ["gpu", "cpu", "storage"]
```

Ngân chỉ cần gọi hàm này, lấy danh sách slug, rồi đưa vào Spring AI prompt:
> *"Người dùng hay xem: gpu, cpu, storage. Hãy gợi ý sản phẩm phù hợp..."*

---

#### ② `RecentCategoryConfiguration.java` — Bridge vào Spring AI

📁 `src/main/java/.../tracking/infrastructure/RecentCategoryConfiguration.java`

```java
@Configuration
public class RecentCategoryConfiguration {

    @Bean
    public RecentCategoryProvider recentCategoryProvider(UserTrackingService history) {
        // Wiring: Ngân gọi RecentCategoryProvider → thực ra gọi hàm của Nhân
        return (userId, since, limit) -> history.getTopViewedCategories(userId, since, limit);
    }
}
```

**Ý nghĩa:** File này là "dây nối" giữa 3.2 và 3.4. Ngân định nghĩa interface `RecentCategoryProvider`, Nhân implement nó qua `UserTrackingService`. Spring tự động inject Bean này vào service của Ngân — không cần sửa code của nhau.

---

#### ③ REST API `GET /api/tracking/profile` → Bửu (3.5)

```
GET /api/tracking/profile
Response:
{
  "topCategories": ["gpu", "storage", "cpu"],
  "topProductIds": ["uuid-1", "uuid-2", "uuid-3"]
}
```

Bửu chỉ cần gọi API này từ frontend để hiển thị lịch sử xem của người dùng.

---

### ❓ Q&A — Phần 5 (Giáo viên có thể hỏi)

**Q: Em để lại cho người sau là gì cụ thể?**  
> A: Dạ, em để lại 3 thứ chính ạ. Thứ nhất là phương thức `getTopViewedCategories()` trong Service — Ngân (3.4) gọi để lấy danh mục ưa thích của user đưa vào AI. Thứ hai là REST API `GET /api/tracking/profile` — Bửu (3.5) dùng để hiển thị lịch sử xem. Thứ ba là bảng `user_activity_logs` trên Supabase chứa toàn bộ dữ liệu hành vi người dùng ạ.

**Q: Người khác gọi lại hàm em viết phải không?**  
> A: Dạ đúng ạ. Ngân (3.4) gọi hàm `getTopViewedCategories()` của em thông qua interface `RecentCategoryProvider`. Em có viết class `RecentCategoryConfiguration` để "nối dây" — khai báo Bean cho Spring, khi Ngân inject `RecentCategoryProvider` vào service của Ngân thì Spring tự động dùng implementation hàm của em ạ. Hai người không cần sửa code của nhau.

**Q: Nếu Ngân chưa làm 3.4 thì em có thể test riêng không?**  
> A: Dạ được ạ. Em có viết `UserTrackingTest.java` để test riêng Service và Controller của 3.2 bằng Mockito, không phụ thuộc vào code của Ngân ạ.

---

## 📌 PHẦN 6 — Kịch Bản Demo Trước Giảng Viên

### Bước 1: Mở web và bật F12

1. Mở Chrome → truy cập `http://localhost:5175`
2. Nhấn `F12` → vào tab **Network**
3. Trong ô filter, gõ **`events`** để lọc chỉ hiển thị request tracking

### Bước 2: Thao tác trên web để sinh event

| Hành động | Event được gửi | eventType |
|---|---|---|
| Click vào 1 sản phẩm bất kỳ | `POST /api/tracking/events` | `VIEW_PRODUCT` |
| Click vào menu "PC bộ" hoặc "Linh kiện PC" | `POST /api/tracking/events` | `FILTER_CATEGORY` |
| Tìm kiếm sản phẩm | `POST /api/tracking/events` | `SEARCH` |
| Thêm sản phẩm vào giỏ | `POST /api/tracking/events` | `ADD_TO_CART` |

### Bước 3: Xem trong F12 Network

Sau khi click sản phẩm, trong F12 sẽ thấy:
- **Name:** `events` (hoặc `events` × 2 nếu trang có cả VIEW_PRODUCT + FILTER_CATEGORY)
- **Tab Payload:** Dữ liệu gửi lên:
  ```json
  {
    "eventType": "VIEW_PRODUCT",
    "productId": "f16fc762-d9e5-510b-b9d6-1054563dcebb",
    "categorySlug": "storage",
    "clientTimestamp": "2026-11-10T15:19:49.530Z",
    "metadata": "{\"productName\":\"Ổ cứng SSD Samsung...\",\"price\":6990000}"
  }
  ```
- **Tab Response:** Phản hồi từ server:
  ```json
  {
    "status": "success",
    "id": "178a5fed-532e-4b95-32c1-04a6a1b7df57",
    "eventType": "VIEW_PRODUCT"
  }
  ```

### Bước 4: Kiểm tra Database trên Supabase

1. Mở [supabase.com](https://supabase.com) → vào project
2. Vào **Table Editor** → chọn bảng `user_activity_logs`
3. Click **Refresh** (đôi khi cần đợi vài giây)
4. Thấy dòng mới với: `event_type = VIEW_PRODUCT`, `category_slug = storage`, `product_id = f16fc762-...`

### Bước 5: Gọi API Profile (tùy chọn — để demo thêm)

Mở tab mới trong trình duyệt hoặc dùng Postman:
```
GET http://localhost:8080/api/tracking/profile
```
→ Trả về:
```json
{
  "topCategories": ["storage", "gpu", "cpu"],
  "topProductIds": ["uuid-1", "uuid-2"]
}
```

---

## 📌 PHẦN 7 — Ý Nghĩa Các eventType

| eventType | Khi nào xảy ra | Dữ liệu đi kèm |
|---|---|---|
| `VIEW_PRODUCT` | Người dùng mở trang chi tiết sản phẩm | `productId`, `categorySlug`, `metadata` (tên, giá) |
| `FILTER_CATEGORY` | Người dùng click vào menu danh mục | `categorySlug` |
| `SEARCH` | Người dùng tìm kiếm trong ô search | `metadata` (từ khóa tìm) |
| `ADD_TO_CART` | Người dùng thêm sản phẩm vào giỏ | `productId`, `metadata` |
| `VIEW_CART` | Người dùng mở trang giỏ hàng | (không có thêm) |
| `CHECKOUT` | Người dùng vào trang thanh toán | (không có thêm) |

---

## 📌 PHẦN 8 — Tổng Kết Nhanh (Cho Phần Trả Lời Miệng)

### Nếu giáo viên hỏi: *"Em làm được gì trong dự án?"*

> **Trả lời ngắn gọn:**  
> "Dạ em làm nhiệm vụ 3.2 — Backend lưu trữ hành vi người dùng ạ. Em xây dựng hệ thống tự động ghi lại mọi hành động của người dùng trên web (xem sản phẩm, lọc danh mục, tìm kiếm,...) vào database. Hệ thống gồm REST API nhận event từ frontend, Service xử lý nghiệp vụ, Repository với 3 truy vấn SQL tùy chỉnh, và bảng `user_activity_logs` trên Supabase. Ngoài ra em còn để lại hàm `getTopViewedCategories()` để bạn Ngân (3.4) dùng trong Spring AI gợi ý sản phẩm ạ."

### Nếu giáo viên hỏi: *"Liên quan đến Spring AI chỗ nào?"*

> "Dạ, em không viết trực tiếp Spring AI, nhưng em tạo ra dữ liệu đầu vào cho AI ạ. Hàm `getTopViewedCategories()` của em trả về danh sách danh mục mà user hay xem nhất, Ngân đưa vào AI prompt để AI gợi ý sản phẩm phù hợp với sở thích cụ thể của từng user thay vì gợi ý chung chung ạ."

### Nếu giáo viên hỏi: *"Em có làm Database không?"*

> "Dạ có ạ. Em tạo bảng `user_activity_logs` trên Supabase qua Flyway migration, tạo 3 index để tối ưu tốc độ truy vấn, và viết 3 câu JPQL trong Repository để lấy dữ liệu thống kê ạ."

---

*📝 File này được tạo để hỗ trợ báo cáo nhiệm vụ 3.2 — Huỳnh Đoàn Nhân*

