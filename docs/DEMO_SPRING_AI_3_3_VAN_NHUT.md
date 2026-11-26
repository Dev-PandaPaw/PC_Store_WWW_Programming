# Mục 3.3 — Văn Nhựt: hiểu bài, trình bày và demo từ đầu đến cuối

> **Cập nhật 11/11/2026:** Backend hiện chạy `local,semantic` và đã lưu **30 document, mỗi embedding 768 chiều**, vào bảng Supabase `public.product_semantic_vectors`. API xác nhận `storage: "pgvector"`. Bài giải thích chunking bên dưới vẫn minh họa chế độ local/RAM trước đó; nếu demo trạng thái Supabase hiện tại, xem **phần 16** ở cuối tài liệu và [biên bản kiểm tra](testing/SPRING_AI_SUPABASE_VECTOR_VERIFICATION.md).

**Dùng tài liệu này trực tiếp khi trình bày.** Đọc theo thứ tự từ trên xuống. Những đoạn có nhãn **Console — copy và chạy** được dán vào F12 → Console. Những đoạn **Java — mở để giải thích** là trích đoạn mã nguồn, không dán vào Console và không cần sửa code.

Toàn bộ code demo nằm ngay trong tài liệu này, không phải copy thêm file helper. Trong VS Code, mở file này rồi nhấn **Ctrl + Shift + V** để xem bản trình bày; quay về bản Markdown khi cần copy một đoạn.

Tài liệu đối chiếu mã nguồn ngày **10/11/2026**, trình bày luồng **local → SimpleVectorStore trong RAM**, đúng luồng đã xem được 44 document và embedding 768 chiều. Các số lượng thực tế có thể thay đổi khi database thay đổi.

## 1. Mở đầu: mình làm gì trong phần AI?

**Bạn nói:**

> “Dạ thưa Cô, phần 3.3 em phụ trách là chuẩn bị kho dữ liệu sản phẩm cho việc tìm kiếm theo ngữ nghĩa. Em lấy sản phẩm từ database, chuyển nội dung mỗi sản phẩm thành document, sau đó dùng Spring AI gọi Gemini để tạo vector và lưu lại. Em sẽ demo trên một sản phẩm cụ thể để thấy được từ dữ liệu ban đầu đến mảng số sau cùng.”

**Hiểu bằng một ví dụ:**

Database có sản phẩm “Vỏ máy tính Lian Li O11 Vision Compact Black”. Chương trình dựng một đoạn văn bản giới thiệu tên, hãng, danh mục và mô tả của sản phẩm. Gemini nhận văn bản này và trả về một mảng số biểu diễn nội dung của nó. Kho vector lưu văn bản, thông tin đi kèm và mảng số để phía tìm kiếm dùng sau.

```text
Sản phẩm trong database
    ↓ đọc và chọn sản phẩm ACTIVE
Document: id + text + metadata
    ↓ gọi embedding model qua Spring AI
Vector: mảng 768 số trong cấu hình demo
    ↓ lưu
Vector Store: document đi cùng vector của nó
```

Ở đây nên dùng hai từ chính xác:

- **Chunking:** dựng/chia nội dung thành các đoạn.
- **Embedding:** chuyển nội dung thành vector số.

“Băm sản phẩm” là cách mình nói cho dễ hình dung khi trao đổi, nhưng lúc giải thích kỹ nên tách hai việc trên. Đây cũng không phải mã hóa sản phẩm thành một chuỗi hash để giải mã ngược.

**Mối liên hệ với các bạn trong nhóm:**

> “Phần 3.1 ghi nhận hành vi ở frontend; phần 3.2 lưu hành vi và cung cấp hồ sơ người dùng. Phần em đọc dữ liệu sản phẩm từ catalog để tạo kho vector. Sau đó phần 3.4 có thể dùng thông tin quan tâm của người dùng cùng kho sản phẩm này để tìm và lọc gợi ý; phần 3.5 phụ trách phần hiển thị và thao tác đồng bộ trên giao diện.”

Lưu ý: 3.3 đọc **ProductRepository**, không lấy bảng lịch sử hành vi của 3.2 để chuyển thành vector sản phẩm.

## 2. Sáu file của mình nằm ở đâu?

Trong IDE, mở thư mục:

```text
src/main/java/iuh/fit/nhom16/pcstore/ai/vector/
```

| File gốc của 3.3 | Nói ngắn gọn về vai trò |
|---|---|
| [VectorStoreConfig.java](../src/main/java/iuh/fit/nhom16/pcstore/ai/vector/VectorStoreConfig.java) | Chuẩn bị kết nối Gemini, embedding model và kho vector |
| [AdminVectorController.java](../src/main/java/iuh/fit/nhom16/pcstore/ai/vector/AdminVectorController.java) | Nhận yêu cầu đồng bộ từ Admin |
| [ProductVectorIngestionService.java](../src/main/java/iuh/fit/nhom16/pcstore/ai/vector/ProductVectorIngestionService.java) | Điều phối: đọc sản phẩm → dựng document → nạp kho |
| [ProductChunker.java](../src/main/java/iuh/fit/nhom16/pcstore/ai/vector/ProductChunker.java) | Tạo text, metadata, ID và chia mô tả dài |
| [ProductEmbeddingModel.java](../src/main/java/iuh/fit/nhom16/pcstore/ai/vector/ProductEmbeddingModel.java) | Gọi model qua đối tượng Spring AI và kiểm tra vector trả về |
| [ProductVectorStore.java](../src/main/java/iuh/fit/nhom16/pcstore/ai/vector/ProductVectorStore.java) | Lưu document cùng vector; thay kho sau khi nạp thành công |

Khi giải thích, mở file bằng link trên hoặc **Ctrl + P → nhập tên file**. Trong mỗi file, dùng **Ctrl + F** tìm tên hàm được ghi ở từng bước dưới đây. Cách này vẫn tìm đúng nếu số dòng thay đổi.

Ngoài sáu file gốc, hiện có hai file phục vụ xem dữ liệu demo: `AdminVectorInspectionController` và `ProductVectorInspectionService`. Chúng cung cấp API GET để đọc vector đã lưu; không tự tạo lại embedding.

## 3. Chuẩn bị trước khi demo

Mở website tại **http://localhost:5173**, nhấn **F12**. Ở tab **Network**, bật **Preserve log** và chọn **Fetch/XHR**; sau đó chuyển sang **Console** để chạy code.

Nếu backend và frontend đang chạy thì bỏ qua hai lệnh bên dưới. Nếu chưa chạy, mở hai terminal tại thư mục gốc dự án:

**Terminal 1 — backend:**

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

**Terminal 2 — frontend:**

```powershell
cd frontend
npm.cmd run dev
```

Database và `GEMINI_API_KEY` phải được cấu hình sẵn ở backend. Dùng tài khoản có quyền **ADMIN** khi Console yêu cầu đăng nhập.

Các URL `/api/...` trong code bên dưới chạy từ trang localhost:5173; Vite chuyển request đến backend localhost:8080. Không dán code vào Console của một website khác.

**Cách chọn lượt demo:**

- Nếu muốn chứng minh toàn bộ quá trình từ lúc nhấn đồng bộ: chạy bước 5 một lần, rồi xem kết quả từ bước 6.
- Nếu đã có 44 document như lần trước: có thể bỏ qua thao tác POST ở bước 5, giải thích code của bước đó rồi đi thẳng đến bước 6. Việc xem dữ liệu không gọi lại Gemini.

Ở chế độ RAM, khởi động lại backend sẽ làm mất kho đang có. Khi đó cần đồng bộ lại.

## 4. Trước khi có request: cấu hình model ở đâu?

**Bạn nói:**

> “Đầu tiên em cần cấu hình model để biến văn bản thành vector. File này tạo các đối tượng để Spring quản lý. Model embedding em dùng mặc định là gemini-embedding-001 và số chiều em cấu hình là 768.”

**Java — mở để giải thích:** `VectorStoreConfig.java`, tìm `productEmbeddingModel`.

```java
@Value("${app.ai.vector.model:gemini-embedding-001}") String model,
@Value("${app.ai.vector.dimensions:768}") int dimensions
```

`@Value` đọc cấu hình; phần sau dấu `:` là giá trị mặc định khi không ghi đè. **768 là số phần tử của một vector**, không phải số sản phẩm và cũng không phải số chiều cố định cho mọi mô hình AI.

Ngay bên dưới:

```java
var connection = GoogleGenAiEmbeddingConnectionDetails.builder()
        .genAiClient(productEmbeddingClient).build();
var options = GoogleGenAiTextEmbeddingOptions.builder()
        .model(model).dimensions(dimensions)
        .taskType(GoogleGenAiTextEmbeddingOptions.TaskType.SEMANTIC_SIMILARITY)
        .build();
var delegate = new GoogleGenAiTextEmbeddingModel(connection, options,
        new RetryTemplate(RetryPolicy.withMaxRetries(0)));
return new ProductEmbeddingModel(delegate, settings.configured(), dimensions);
```

Giải thích theo thứ tự:

1. `connection` dùng Google client đã cấu hình để kết nối dịch vụ.
2. `options` chứa tên model, số chiều và loại tác vụ được khai báo trong code.
3. `GoogleGenAiTextEmbeddingModel` là lớp tích hợp của Spring AI để làm việc với model của Google.
4. Lớp `ProductEmbeddingModel` của mình bọc đối tượng đó để kiểm tra cấu hình, kết quả và xử lý lỗi.

Tìm tiếp `productEmbeddingClient`: hàm này gắn API key, địa chỉ dịch vụ, timeout và chính sách retry. Key nằm ở backend, không gửi xuống Console.

Tìm `vectorStore`:

```java
@Bean
@ConditionalOnProperty(name="app.recommendations.enabled",
        havingValue="false", matchIfMissing=true)
ProductVectorStore vectorStore(EmbeddingModel productEmbeddingModel) {
    return new ProductVectorStore(productEmbeddingModel);
}
```

`@Bean` đưa đối tượng vào Spring để các service nhận qua constructor. Điều kiện trên chọn kho RAM khi cấu hình recommendations chưa bật. Khởi tạo bean chỉ chuẩn bị đối tượng; **không tự đọc toàn bộ database và không tự gọi Gemini để nạp sản phẩm**.

## 5. Bấm đồng bộ: request đi qua những file nào?

### 5.1. Gọi API đồng bộ

**Bạn nói:**

> “Bình thường thao tác này có thể gắn vào nút Đồng bộ của Admin. Để demo riêng phần em mà chưa phụ thuộc giao diện, em gọi đúng API đó bằng Console. Sau khi chạy xong, em sẽ kiểm tra dữ liệu thực sự đã được lưu.”

**Console — copy và chạy nếu cần đồng bộ mới.** Đoạn này có gọi Gemini; chạy một lượt rồi đợi hoàn tất.

```javascript
(async () => {
  const username = prompt('Tên tài khoản Admin:');
  const password = prompt('Mật khẩu Admin:');
  if (!username || !password) return;

  const csrfResponse = await fetch('/api/csrf', {
    credentials: 'include'
  });
  if (!csrfResponse.ok) throw new Error('Không lấy được CSRF token');
  const csrf = await csrfResponse.json();

  console.time('Đồng bộ mục 3.3');
  try {
    const response = await fetch('/api/admin/vector/reindex', {
      method: 'POST',
      credentials: 'include',
      headers: {
        Authorization: 'Basic ' + btoa(username + ':' + password),
        [csrf.headerName]: csrf.token
      }
    });
    const result = await response.json();
    console.log('HTTP:', response.status);
    console.log('Kết quả đồng bộ:', result);
    if (!response.ok) throw new Error(JSON.stringify(result));
  } finally {
    console.timeEnd('Đồng bộ mục 3.3');
  }
})().catch(console.error);
```

`fetch` gửi HTTP request. `credentials: 'include'` gửi cookie phiên; header `Authorization` xác thực Admin. POST cần thêm CSRF token nên code lấy `/api/csrf` trước. `await` chờ phản hồi; trong lúc chờ, Console hiện `Promise {<pending>}` là bình thường.

Khi thành công, Network → `reindex` → **Response** có dạng:

```json
{
  "status": "SUCCESS",
  "productCount": 44,
  "documentCount": 44,
  "completedAt": "..."
}
```

`productCount` đếm sản phẩm đã chọn; `documentCount` đếm document tạo ra. **Response này chỉ là báo cáo đồng bộ**, nên chưa thấy mảng 768 số ở đây. Đến bước 6 mình sẽ gọi API đọc dữ liệu trong kho.

### 5.2. Controller tiếp nhận

**Java — mở để giải thích:** `AdminVectorController.java`, tìm `reindex()`.

```java
@PostMapping("/reindex")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ProductVectorIngestionService.ReindexResult> reindex() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ingestion.reindexAll());
}
```

Ghép với `@RequestMapping("/api/admin/vector")` ở đầu class sẽ thành URL đã gọi. `@PreAuthorize` yêu cầu quyền Admin. Controller giao việc cho `ingestion.reindexAll()`, rồi trả kết quả; `noStore()` yêu cầu không cache phản hồi.

Hàm `handleFailure` ở cuối file trả HTTP status cùng `error.code`, `error.message`; nếu cần chờ trước khi thử lại thì thêm `Retry-After`.

**Bạn nói:**

> “Controller chỉ nhận yêu cầu và trả kết quả. Công việc đọc dữ liệu, tạo document và nạp kho nằm ở service.”

### 5.3. Service đọc sản phẩm và điều phối

**Java — mở để giải thích:** `ProductVectorIngestionService.java`, tìm `reindexAll`, rồi tìm `var snapshot`.

```java
var snapshot = Objects.requireNonNull(readTransaction.execute(transaction -> {
    var active = products.findAll().stream()
            .filter(p -> "ACTIVE".equals(p.getStatus())).toList();
    var documents = active.stream()
            .flatMap(p -> chunker.chunk(p).stream()).toList();
    return new CatalogSnapshot(active.size(), documents);
}));
vectorStore.replaceAll(snapshot.documents());
return new ReindexResult("SUCCESS", snapshot.productCount(),
        snapshot.documents().size(), Instant.now());
```

**Bạn nói:**

> “Ở đây products là ProductRepository được truyền vào qua constructor. Em đọc sản phẩm trong catalog rồi giữ những sản phẩm ACTIVE. Mỗi sản phẩm đi qua chunker để thành một hoặc nhiều document. Sau đó em đưa toàn bộ document sang Vector Store để tạo embedding và lưu. Hoàn tất mới trả SUCCESS.”

Giải thích thêm nếu được hỏi:

- `stream().filter(...)` chọn sản phẩm đang ACTIVE.
- `chunker.chunk(p)` trả một danh sách document cho sản phẩm đó.
- `flatMap` gom các danh sách nhỏ thành một danh sách document chung.
- `CatalogSnapshot` giữ số sản phẩm và danh sách document của lượt đọc này.
- `readTransaction` được đặt read-only trong constructor. Đoạn đọc database kết thúc trước khi gọi model, tránh giữ kết nối database trong lúc chờ Gemini.
- `reindexLock.tryLock()` phía trên ngăn hai lượt đồng bộ RAM chạy chồng trong cùng ứng dụng. `finally` nhả khóa dù thành công hay lỗi.
- Đầu hàm kiểm tra API key; thiếu cấu hình thì báo `VECTOR_NOT_CONFIGURED`.

Đầu hàm còn có nhánh `persistentIndex.get()`. Đó là đường dùng chỉ mục persistent khi bật phần semantic; **demo local trong tài liệu này đi theo nhánh `var snapshot` vừa giải thích**. Phần 12 nói rõ sự khác nhau.

## 6. Đọc tất cả document đã lưu ra Console

**Bạn nói:**

> “Đồng bộ báo thành công là bước đầu. Bây giờ em đọc kho vector để kiểm tra từng document có nội dung, metadata và embedding thật hay chưa.”

**Console — copy cả khối và chạy.** Không gọi Gemini; chỉ đọc dữ liệu đã lưu. Khối này tự đọc tiếp các trang nếu kho có hơn 100 document.

```javascript
(async () => {
  const username = prompt('Tên tài khoản Admin:');
  const password = prompt('Mật khẩu Admin:');
  if (!username || !password) return;
  const authorization = 'Basic ' + btoa(username + ':' + password);
  const documents = [];
  let total = null;
  let storage = null;

  do {
    const response = await fetch(
      '/api/admin/vector/documents?offset=' + documents.length + '&limit=100',
      { credentials: 'include', headers: { Authorization: authorization } }
    );
    const page = await response.json();
    if (!response.ok) throw new Error(JSON.stringify(page));
    if (total !== null && (page.total !== total || page.storage !== storage)) {
      throw new Error('Kho vừa thay đổi. Đợi đồng bộ xong rồi đọc lại.');
    }
    total = page.total;
    storage = page.storage;
    documents.push(...page.documents);
    if (!page.documents.length && documents.length < total) {
      throw new Error('Chưa đọc đủ dữ liệu. Hãy chạy lại.');
    }
  } while (documents.length < total);

  window.vectorDocuments = documents;
  window.vectorDemoStorage = storage;
  console.log('Kho:', storage, '| Tổng document:', total);
  console.table(documents.map((d, index) => ({
    index,
    productId: d.metadata.productId,
    ten: d.text.split('\n')[0],
    chunkIndex: d.metadata.chunkIndex,
    soChieu: d.embedding.length
  })));
  console.log('Toàn bộ dữ liệu:', documents);
})().catch(console.error);
```

Kỳ vọng với lượt demo hiện có: **Kho: memory | Tổng document: 44**, bảng có 44 dòng và cột `soChieu` là 768.

`window.vectorDocuments` giữ kết quả ngay trong tab trình duyệt để các bước tiếp theo truy cập. Đây là biến mình tạo để demo; tên này không phải biến có sẵn trong ứng dụng.

Sau mỗi lần reindex lại, chạy lại khối đọc này để có dữ liệu mới. Không refresh trang giữa các bước, vì refresh sẽ mất biến Console.

Trong Network sẽ có request `documents?offset=0&limit=100`. Mở **Response**, mỗi phần tử có:

```text
id          ID của document
text        Văn bản của document
metadata    Thông tin đi kèm
embedding   Mảng số đã lưu
dimensions  Số phần tử trong embedding
```

**Code trả dữ liệu để demo:** mở [AdminVectorInspectionController.java](../src/main/java/iuh/fit/nhom16/pcstore/ai/vector/AdminVectorInspectionController.java), tìm `documents`; rồi mở [ProductVectorInspectionService.java](../src/main/java/iuh/fit/nhom16/pcstore/ai/vector/ProductVectorInspectionService.java), tìm nhánh `if (!store.isPersistent())`:

```java
var documents = store.memoryDocuments();
return new DocumentPage("memory", documents.size(), offset, limit,
        documents.stream().skip(offset).limit(limit).toList());
```

Đây là đọc từ kho đã có rồi phân trang. Không gọi model để tạo một bộ số khác cho màn hình demo.

## 7. Chọn một document và đối chiếu với sản phẩm gốc

**Bạn nói:**

> “Em chọn document ở vị trí số 0 để trình bày xuyên suốt. Em dùng productId của nó để lấy lại sản phẩm trong catalog, như vậy mình biết document này thuộc sản phẩm nào.”

**Console — copy và chạy:**

```javascript
(async () => {
  if (!window.vectorDocuments?.length) {
    throw new Error('Chưa có document. Hãy chạy bước 5 và 6 trước.');
  }
  const document = vectorDocuments[0];
  const productId = document.metadata.productId;
  const response = await fetch(
    '/api/catalog/products/' + encodeURIComponent(productId),
    { credentials: 'include' }
  );
  if (!response.ok) throw new Error('Không đọc được sản phẩm gốc');
  window.demoSourceProduct = await response.json();
  console.log('Sản phẩm trong catalog:', demoSourceProduct);
  console.log('ID trùng nhau:',
    String(demoSourceProduct.id) === String(productId));
  console.log('Document đang chọn:', document.id);
})().catch(console.error);
```

Mở object sản phẩm để chỉ tên, ID, mô tả, giá… Sau đó chỉ dòng **ID trùng nhau: true**.

Với dữ liệu đã xem trước đó, document đầu thuộc sản phẩm **Vỏ máy tính Lian Li O11 Vision Compact Black**. Nhưng đừng học thuộc rằng `[0]` luôn là sản phẩm này: API xem kho sắp xếp theo **ID document**, không theo thứ tự thêm sản phẩm trong database. Nếu muốn chọn sản phẩm khác, đổi chỉ số `[0]` nhất quán ở các bước.

## 8. Xem text: model đọc nội dung gì?

**Console — copy và chạy:**

```javascript
console.log(vectorDocuments[0].text);
```

**Bạn nói:**

> “Đây là phần văn bản mà em chuẩn bị để đưa vào embedding model. Nó gom tên sản phẩm, SKU, danh mục, thương hiệu, thông số nếu có và mô tả. Em giữ ngữ cảnh sản phẩm trong mỗi đoạn để khi chia mô tả dài, đoạn nào cũng biết đang nói về linh kiện nào.”

**Java — mở để giải thích:** `ProductChunker.java`, tìm `chunk(Product product)`.

```java
StringBuilder context = new StringBuilder();
field(context, "Sản phẩm", product.getName());
field(context, "SKU", product.getSku());
if (product.getCategory() != null)
    field(context, "Danh mục", product.getCategory().getName());
field(context, "Thương hiệu", product.getBrand());
```

`context` là phần giới thiệu chung. `field` chỉ thêm dòng khi giá trị có nội dung. Khối `ProductSpec spec = product.getSpec()` bên dưới bổ sung CPU, GPU, RAM, dung lượng, loại ổ cứng, mainboard, nguồn, case và hệ điều hành nếu có.

Tìm tiếp:

```java
String description = clean(product.getDescription());
List<String> parts = splitDescription(description);
```

`clean` bỏ thẻ HTML bằng biểu thức trong code và gom khoảng trắng; đây là làm gọn văn bản cho embedding. `splitDescription` chia mô tả dài thành các phần khoảng tối đa **1.200 ký tự Java**, ưu tiên ngắt ở dấu cách và tránh cắt giữa surrogate pair Unicode.

Mức 1.200 áp dụng cho **phần mô tả**, không phải giới hạn toàn bộ text và không phải 1.200 token. Khi ghép thêm context, text đầy đủ có thể dài hơn. Mô tả trống vẫn tạo một phần rỗng để sản phẩm có document từ các thông tin khác.

Cuối vòng lặp:

```java
String id = UUID.nameUUIDFromBytes(
        ("product:" + product.getId() + ":chunk:" + index)
        .getBytes(StandardCharsets.UTF_8)).toString();
String text = context
        + (parts.get(index).isEmpty() ? "" : "Mô tả: " + parts.get(index));
documents.add(new Document(id, text.strip(), metadata));
```

**Bạn nói:**

> “Mỗi phần mô tả sẽ thành một document, có ID riêng. ID được tạo từ productId và số thứ tự đoạn, nên cùng sản phẩm và cùng số đoạn sẽ có cùng ID. Tới dòng new Document này em mới dựng dữ liệu, chưa có 768 số. Vector được tạo khi nạp document vào store.”

Đừng nhầm UUID ở đây với vector: UUID là mã nhận diện; embedding là mảng số biểu diễn nội dung.

## 9. Xem metadata: vì sao text rồi vẫn cần phần này?

**Console — copy và chạy:**

```javascript
console.table(vectorDocuments[0].metadata);
```

**Bạn nói:**

> “Text giúp model hiểu nội dung. Metadata là thông tin có cấu trúc để chương trình biết kết quả tìm được là sản phẩm nào và thuộc đoạn nào. Ví dụ sau khi tìm được một document tương đồng, em lấy productId trong metadata để truy ngược về sản phẩm.”

Các trường của **luồng local đang demo**:

| Trường | Ý nghĩa và lý do giữ lại |
|---|---|
| `productId` | ID sản phẩm gốc; nối kết quả vector về catalog |
| `category` | Slug danh mục, ví dụ `case`; dùng phân loại/lọc |
| `price` | Giá tại thời điểm đồng bộ, dạng số |
| `stockOnHand` | Số lượng tồn tại thời điểm đồng bộ |
| `availableStock` | Lượng còn có thể bán: tồn trừ lượng đã giữ, không âm |
| `status` | Trạng thái sản phẩm tại thời điểm đồng bộ |
| `chunkIndex` | Thứ tự đoạn của sản phẩm, bắt đầu từ 0 |
| `chunkCount` | Tổng số đoạn tạo ra cho sản phẩm đó |

Ví dụ `chunkIndex: 0, chunkCount: 1` nghĩa là document đang xem là đoạn đầu và sản phẩm chỉ có một đoạn. `stockOnHand: 50`, `availableStock: 50` nghĩa là lúc đồng bộ chưa có lượng hàng bị giữ làm giảm lượng có thể bán.

**Java — mở để giải thích:** vẫn ở `ProductChunker.java`, tìm `metadata.put`.

```java
metadata.put("productId", product.getId().toString());
metadata.put("price", Objects.requireNonNull(product.getPrice(),
        "Product price is required").doubleValue());
metadata.put("category", product.getCategory() == null
        ? "" : clean(product.getCategory().getSlug()));
metadata.put("stockOnHand",
        Objects.requireNonNullElse(product.getStockOnHand(), 0));
int reserved = Objects.requireNonNullElse(product.getReservedQuantity(), 0);
metadata.put("availableStock",
        Math.max(0, (int) metadata.get("stockOnHand") - reserved));
metadata.put("status", clean(product.getStatus()));
metadata.put("chunkIndex", index);
metadata.put("chunkCount", parts.size());
```

`Map<String, Object>` giữ các cặp tên trường–giá trị. Mình giữ giá/tồn bằng kiểu số thay vì chuỗi để có thể so sánh/lọc theo số. `Objects.requireNonNullElse(..., 0)` dùng 0 khi giá trị không có; `Math.max(0, ...)` tránh lượng bán được bị âm.

**Chốt ý quan trọng:**

> “Metadata do code Java lấy từ dữ liệu sản phẩm để gắn vào, không phải Gemini tự đoán ra. Nó là ảnh chụp tại lúc đồng bộ. Khi tư vấn giá và tồn kho thực tế, phía sử dụng kết quả vẫn cần đọc lại database vì dữ liệu có thể đã đổi.”

Trong luồng local, chương trình không tự ghép toàn bộ metadata vào text trước khi embedding. Nội dung mô tả của người nhập vẫn có thể chứa giá hay thông tin khác, nhưng map metadata được giữ riêng.

## 10. Xem embedding: 768 số nằm ở đây

**Console — copy và chạy:**

```javascript
console.log('Sản phẩm:', vectorDocuments[0].text.split('\n')[0]);
console.log('Mảng embedding đầy đủ:', vectorDocuments[0].embedding);
console.log('Số chiều:', vectorDocuments[0].embedding.length);
console.table(vectorDocuments[0].embedding.slice(0, 10)
  .map((value, index) => ({ viTri: index, giaTri: value })));
```

Console hiện mảng **(768)**; bấm mũi tên để mở các nhóm phần tử. Bảng bên dưới chỉ lấy 10 số đầu để dễ đọc; mảng đầy đủ vẫn nằm trong `embedding`.

Nếu cần trình chiếu cả mảng dưới dạng văn bản:

```javascript
console.log(JSON.stringify(vectorDocuments[0].embedding));
```

**Bạn nói:**

> “Đây mới là vector của document vừa xem. Nó có 768 số do embedding model tính từ text. Em không tự gán các số này. Mỗi số là một tọa độ trong biểu diễn mà model học được; em không đọc riêng một số để kết luận nó là giá tiền hay dung lượng RAM. Mình so sánh cả vector để tìm nội dung tương đồng.”

### 10.1. Đoạn nào gọi model?

**Java — mở để giải thích:** `ProductEmbeddingModel.java`, tìm `embed(Document document)`.

```java
@Override
public float[] embed(Document document) {
    return embed(document.getText());
}
```

Hàm lấy text của document để embedding. Phần xử lý lớp cha cuối cùng đi qua `call(EmbeddingRequest request)`; tìm dòng:

```java
EmbeddingResponse response = delegate.call(request);
```

`delegate` chính là `GoogleGenAiTextEmbeddingModel` đã tạo trong file config. Spring AI và Google client xử lý lời gọi dịch vụ cùng việc đọc phản hồi. Code của mình nhận kết quả rồi kiểm tra.

### 10.2. Kiểm tra kết quả như thế nào?

Trong cùng hàm `call`:

```java
if (response == null
        || response.getResults().size() != request.getInstructions().size())
    throw new IllegalStateException("Embedding count mismatch");
for (var result : response.getResults()) {
    float[] vector = result.getOutput();
    if (vector == null || vector.length != dimensions)
        throw new IllegalStateException("Embedding dimensions mismatch");
    double norm = 0;
    for (float value : vector) {
        if (!Float.isFinite(value))
            throw new IllegalStateException("Non-finite embedding");
        norm += (double) value * value;
    }
    if (norm == 0) throw new IllegalStateException("Zero embedding");
}
```

**Bạn nói:**

> “Em không chỉ nhận một mảng là lưu ngay. Em kiểm tra số kết quả có khớp số đầu vào, mỗi vector có đúng số chiều đã cấu hình, các phần tử có phải số hữu hạn và vector có khác vector toàn số 0 hay không. Nếu sai thì báo lỗi.”

`norm` trong đoạn này là tổng bình phương dùng để phát hiện vector toàn 0. Code này **không chuẩn hóa vector** và chưa tính cosine similarity.

Khối `catch` bên dưới đổi lỗi provider thành thông báo của ứng dụng:

- 429 → `VECTOR_QUOTA_EXCEEDED`: vượt quota, có thông tin chờ thử lại.
- 401/403 → `VECTOR_ACCESS_DENIED`: key không có quyền dùng model.
- Lỗi khác hoặc vector không hợp lệ → `VECTOR_EMBEDDING_FAILED`.

Không trả nguyên thông báo nội bộ của provider cho trình duyệt và không tự tạo vector giả để báo thành công.

### 10.3. Đi sâu: ai tính 768 số, Spring AI và Google client xử lý ra sao?

**Bạn nói:**

> “Số 768 là kích thước đầu ra em yêu cầu trong cấu hình. Giá trị của 768 phần tử do model trên dịch vụ Google tính. Code Java chuẩn bị đầu vào, gửi yêu cầu, nhận kết quả, kiểm tra và lưu; em không viết công thức tự sinh các tọa độ đó.”

Phần dưới đã đối chiếu **Spring AI 2.0.2** và **Google Gen AI Java SDK 1.65.0** mà dự án đang dùng. Các đoạn ghi “mã thư viện” là code của dependency, không phải file em tự viết trong package vector. Trong IDE, dùng Ctrl + click tên lớp/hàm hoặc Go to Definition để mở; có thể cần chọn tải Sources.

**A. Lúc khởi động: tạo client và gắn cấu hình.**

Trong `VectorStoreConfig.productEmbeddingClient()`, code tạo Google client:

```java
Client.builder()
        .apiKey(settings.configured()
                ? settings.apiKey().trim() : "not-configured")
```

Phần `httpOptions(...)` ngay sau đó thiết lập base URL, API version `v1beta`, timeout và retry. Client là đối tượng Java để giao tiếp HTTP, không phải bản Gemini chạy trong máy mình.

Trong `productEmbeddingModel()`:

```java
@Value("${app.ai.vector.dimensions:768}") int dimensions
```

Biến `dimensions` được đưa vào options qua `.dimensions(dimensions)`. Google client đi vào `connection`; connection và options đi vào `GoogleGenAiTextEmbeddingModel`; đối tượng đó trở thành `delegate` của `ProductEmbeddingModel`.

```text
Google client: kết nối, xác thực, gửi HTTP
    ↓ gắn vào connection
GoogleGenAiTextEmbeddingModel: chuyển yêu cầu Spring AI sang API Google
    ↓ gán làm delegate
ProductEmbeddingModel: lớp của mình kiểm tra cấu hình và kết quả
    ↓ truyền vào store
SimpleVectorStore: dùng model này khi nạp document
```

**B. Khi đồng bộ: store yêu cầu embedding cho từng document.**

`ProductVectorStore.replaceAll()` gọi `staging.add(documents)`. Bên trong **mã thư viện** `SimpleVectorStore.doAdd()` có:

```java
float[] embedding = this.embeddingModel.embed(document);
SimpleVectorStoreContent storeContent =
        new SimpleVectorStoreContent(document.getId(),
                Objects.requireNonNullElse(document.getText(), ""),
                document.getMetadata(), embedding);
this.store.put(document.getId(), storeContent);
```

Đoạn này nằm trong vòng lặp duyệt document. Nó phải nhận vector trước rồi mới lưu nội dung document cùng vector đó.

Lời gọi `embed(document)` đi vào lớp của mình:

```java
return embed(document.getText());
```

Đây là gọi phiên bản `embed(String)` có sẵn của interface Spring AI, không phải gọi lặp vô hạn vào chính hàm `embed(Document)`. Kiểu tham số khác nhau nên Java chọn overload khác.

Luồng bên trong Spring AI là `embed(String) → embed(List<String>) → tạo EmbeddingRequest → call(request)`. Vì đối tượng đang dùng là `ProductEmbeddingModel`, `call(request)` chạy vào hàm override của mình. Hàm này gọi:

```java
EmbeddingResponse response = delegate.call(request);
```

**C. Spring AI chuyển “768” sang cấu hình API Google.**

Trong **mã thư viện** `GoogleGenAiTextEmbeddingModel.call()`, các options của request được kết hợp với options mặc định đã tạo trong config. Sau đó:

```java
EmbedContentConfig.Builder configBuilder = EmbedContentConfig.builder();
if (options.getDimensions() != null) {
    configBuilder.outputDimensionality(options.getDimensions());
}
```

Đây là đoạn chuyển `dimensions = 768` thành `outputDimensionality = 768` để gửi đến Google. Nó chưa tính các số, chỉ thiết lập kích thước đầu ra được yêu cầu.

Model tích hợp lọc các text rỗng rồi gọi SDK:

```java
EmbedContentResponse embeddingResponse =
        RetryUtils.execute(this.retryTemplate,
                () -> this.genAiClient.models.embedContent(
                        modelName, validTexts, config));
```

`modelName` chọn model; `validTexts` là nội dung cần embedding; `config` mang cấu hình đầu ra, trong đó có 768 chiều.

**D. Google SDK dựng và gửi HTTP.**

Trong **mã thư viện** `com.google.genai.Models.embedContent(...)`, mỗi chuỗi được đóng gói thành `Content` gồm phần text, rồi chuyển đến `privateEmbedContent(...)`.

SDK dựng JSON và đường dẫn request. Với Google Developer API trong cấu hình hiện tại, SDK 1.65.0 dùng đường dẫn `models/gemini-embedding-001:batchEmbedContents` dưới base URL và API version đã cấu hình. Tên hàm Java là `embedContent`, nhưng đường HTTP của overload này dùng `batchEmbedContents`, kể cả khi danh sách hiện chỉ có một text.

Trong `Models.privateEmbedContent()`, **mã thư viện** gửi request:

```java
this.apiClient.request(
        "post", builtRequest.path(),
        builtRequest.body(), builtRequest.httpOptions())
```

`ApiClient` ghép URL, tạo body JSON và gắn key vào header `x-goog-api-key`. `HttpApiClient` dùng OkHttp để gửi request và chờ phản hồi. Vì đây là request backend gửi ra Google, Network của trình duyệt chỉ thấy request đến backend; không thấy trực tiếp lời gọi Google này.

**E. Model trên máy chủ Google tính vector; SDK và Spring AI đọc kết quả.**

Sau khi Google trả phản hồi, SDK đọc JSON thành `EmbedContentResponse`. Các số nằm trong `ContentEmbedding.values()`.

Trong **mã thư viện** `GoogleGenAiTextEmbeddingModel.call()`:

```java
List<Float> floatList = contentEmbedding.values().get();
float[] vectorValues = new float[floatList.size()];
for (int i = 0; i < floatList.size(); i++) {
    vectorValues[i] = floatList.get(i);
}
embeddingList.add(new Embedding(vectorValues, originalIndex));
```

Đây là chuyển **kiểu dữ liệu** từ danh sách số sang mảng `float[]`, không phải tính lại ngữ nghĩa. Các số đã có từ phản hồi của model. Spring AI đóng gói chúng thành `EmbeddingResponse` để trả về cho lớp của mình.

**F. Lớp của mình kiểm tra 768 phần tử rồi store mới lưu.**

Trong `ProductEmbeddingModel.call()`:

```java
float[] vector = result.getOutput();
if (vector == null || vector.length != dimensions)
    throw new IllegalStateException("Embedding dimensions mismatch");
```

Hàm tiếp tục kiểm tra số hữu hạn và không toàn 0 như mục 10.2. Khi hợp lệ, kết quả đi ngược về `SimpleVectorStore.doAdd()` để lưu. Nạp xong tất cả document mới có `current = staging`.

```text
Text của document
 → ProductEmbeddingModel.embed(Document)
 → embed(String), embed(List), EmbeddingRequest của Spring AI
 → ProductEmbeddingModel.call(request)
 → delegate: GoogleGenAiTextEmbeddingModel.call(request)
 → outputDimensionality = 768
 → Google client: models.embedContent(...)
 → SDK dựng JSON, gắn key và gửi HTTP
 → dịch vụ Google tính vector
 → SDK đọc JSON, Spring AI chuyển List<Float> thành float[]
 → ProductEmbeddingModel kiểm tra kết quả
 → SimpleVectorStore lưu document cùng vector
```

**Câu trả lời khi Cô hỏi “đâu là code tính 768 chiều?”:**

> “Dạ, trong code của em có chỗ cấu hình 768 chiều, chỗ gọi model và chỗ kiểm tra đủ 768 phần tử. Code thực hiện phép tính embedding nằm ở dịch vụ mô hình của Google. SDK và Spring AI ở phía em đảm nhiệm giao tiếp và chuyển đổi dữ liệu phản hồi.”

## 11. Vector được lưu khi nào, và làm sao biết toàn bộ kho đạt yêu cầu?

### 11.1. Code nạp kho

**Java — mở để giải thích:** `ProductVectorStore.java`, tìm `replaceAll`.

```java
synchronized void replaceAll(List<Document> documents) {
    if (persistent != null)
        throw new IllegalStateException(
                "Use the persistent catalog synchronizer for PgVector reindexing");
    var staging = new InspectableSimpleVectorStore(embeddingModel);
    if (!documents.isEmpty()) staging.add(documents);
    current = staging;
}
```

**Bạn nói:**

> “Điểm bắt đầu tạo embedding trong luồng nạp kho nằm ở staging.add(documents). SimpleVectorStore dùng embedding model đã truyền vào để tạo các vector và lưu kèm document. Em nạp vào kho tạm trước; chỉ khi thành công hết mới gán current bằng kho tạm đó.”

Trình tự thực sự là:

```text
Service gọi vectorStore.replaceAll(documents)
  → tạo store tạm staging
  → staging.add(documents)
      → Spring AI dùng ProductEmbeddingModel
      → delegate gọi Gemini, kiểm tra kết quả
      → SimpleVectorStore lưu text + metadata + embedding
  → current = staging
  → service trả SUCCESS
```

Nếu lỗi ở bước tạo embedding, dòng `current = staging` chưa chạy nên kho cũ vẫn còn. Đây là cách thay toàn bộ kho RAM sau khi chuẩn bị thành công, không phải cứ xóa kho cũ trước rồi mới gọi model.

`synchronized` điều phối các thao tác ghi trên cùng đối tượng; `volatile current` giúp các luồng đọc thấy kho mới khi được thay. Trường hợp danh sách rỗng thì thay bằng kho rỗng, không cần gọi model.

### 11.2. Các hàm còn lại trong file làm gì?

- Constructor nhận `EmbeddingModel` tạo store RAM; constructor nhận `VectorStore persistent` bọc kho persistent.
- `active()` chọn kho đang dùng; `isPersistent()` cho biết loại kho.
- `add`, hai hàm `delete` và `similaritySearch` chuyển thao tác sang kho đang dùng. Nghiệp vụ chọn người dùng, ngưỡng tương đồng và kiểm tra gợi ý nằm ở phần sử dụng phía sau.
- `memoryDocuments()` đọc kho RAM cho API xem dữ liệu; không gọi embedding model.
- Lớp lồng `InspectableSimpleVectorStore` kế thừa `SimpleVectorStore` và xuất dữ liệu đã lưu thành DTO. Nó dùng Jackson để ánh xạ các trường nội bộ, rồi sắp xếp theo ID document.
- Record `VectorDocument` trả `id, text, metadata, embedding, dimensions`; sao chép map/list và tính `dimensions = embedding.size()` để phản ánh mảng thực tế.

Một `Document` lúc vừa được tạo trong chunker chưa mang mảng số. Sau khi nạp, kho lưu thêm embedding đi kèm. API kiểm tra ghép các thông tin đã lưu thành DTO để mình xem chung, nên ở Console mới có `vectorDocuments[0].embedding`.

### 11.3. Chạy kiểm tra toàn bộ dữ liệu đã đọc

**Console — copy và chạy:**

```javascript
(() => {
  const docs = window.vectorDocuments || [];
  const expectedDimensions = 768;
  const invalid = docs.filter(d =>
    !Array.isArray(d.embedding) ||
    d.embedding.length !== expectedDimensions ||
    d.dimensions !== expectedDimensions ||
    !d.embedding.every(v => typeof v === 'number' && Number.isFinite(v)) ||
    !d.embedding.some(v => v !== 0) ||
    !d.text?.trim() ||
    !d.metadata?.productId
  );
  const uniqueIds = new Set(docs.map(d => d.id));
  const productIds = new Set(docs.map(d => d.metadata.productId));
  const pass = docs.length > 0 &&
    invalid.length === 0 && uniqueIds.size === docs.length;

  console.table([{
    kho: window.vectorDemoStorage,
    soSanPhamPhanBiet: productIds.size,
    soDocument: docs.length,
    soChieuMoiVector: expectedDimensions,
    documentLoi: invalid.length,
    ketQua: pass ? 'PASS' : 'FAIL'
  }]);
  console.log('Document lỗi:', invalid);
  console.assert(pass, 'Kiểm tra chưa đạt; xem document lỗi bên trên.');
})();
```

Với snapshot đã demo: **44 sản phẩm phân biệt, 44 document, mỗi vector 768 chiều, 0 document lỗi, PASS**.

**Bạn nói:**

> “Em kiểm tra trên toàn bộ document vừa đọc, không chỉ nhìn một sản phẩm. Trong lượt dữ liệu này có 44 sản phẩm và mỗi sản phẩm vừa một đoạn nên có 44 vector. Nếu sau này mô tả dài bị chia thành nhiều đoạn thì số document có thể lớn hơn số sản phẩm.”

PASS ở đây chứng minh dữ liệu đã đọc có cấu trúc hợp lệ và vector đúng kích thước trong cấu hình demo. Nó chưa chứng minh chất lượng gợi ý hay hoàn tất toàn bộ UI của các phần sau.

**Đã kiểm tra lại code trong tài liệu ngày 10/11/2026:** cả 8 khối JavaScript đều qua kiểm tra cú pháp; 7 khối chỉ đọc đã chạy với server local hiện tại, qua 2 request GET, lấy 44 document × 768 chiều và đối chiếu đúng ID sản phẩm gốc. Khối POST được kiểm tra cú pháp nhưng không chạy lại trong lượt kiểm tra tài liệu, để không gọi thêm Gemini. Kết quả này gắn với snapshot đang chạy; khi bạn demo vẫn nên chạy kiểm tra ở trên và chụp kết quả của chính lượt đó.

### 11.4. Chụp minh chứng nào để dễ bảo vệ?

Chụp lần lượt bốn màn hình, giữ cùng document đang chọn:

1. **Network → reindex → Response:** HTTP 200, SUCCESS, số sản phẩm/document. Nếu bỏ qua POST vì dùng kho đã có thì dùng minh chứng đồng bộ đã lưu trước đó.
2. **Console → sản phẩm gốc và text:** thấy tên sản phẩm, ID đối chiếu true, văn bản dùng để embedding.
3. **Console → metadata và embedding:** mở rõ productId, chunkIndex/chunkCount; mảng (768) và vài giá trị thực.
4. **Console → bảng kiểm tra cuối:** 44 document, 768 chiều, 0 lỗi và PASS với dữ liệu hiện tại.

Không cần đợi UI gợi ý hoàn thiện mới chụp được bốn bằng chứng này. Đây là các thao tác để bạn tự chụp sau khi chạy, không phải ảnh minh chứng được tạo sẵn trong tài liệu.

## 12. Sau 3.3, vector được dùng thế nào?

**Bạn nói:**

> “Phần em hoàn tất kho sản phẩm đã được biểu diễn thành vector. Khi tìm theo ý nghĩa, phía tìm kiếm cũng cần chuyển nội dung truy vấn thành vector bằng model tương thích rồi so với vector sản phẩm. Các document gần nhau về ngữ nghĩa được lấy ra, dùng productId để đọc lại dữ liệu sản phẩm, kiểm tra điều kiện rồi mới trả kết quả.”

Ví dụ “cần case thoáng, nhìn được linh kiện” không nhất thiết trùng nguyên văn tên sản phẩm. Biểu diễn ngữ nghĩa giúp tìm những nội dung liên quan; phần lọc còn phải kiểm tra danh mục, ngân sách, tồn kho… trước khi gợi ý.

**Phân biệt khi nói về code hiện tại:**

- Demo ở trên dùng **local, kho RAM, ProductChunker** và các trường metadata đã chỉ ra.
- Khi bật `app.recommendations.enabled=true` như profile semantic, `reindexAll()` chuyển sang `ProductSemanticIndexService.synchronize()`; hệ thống dùng **PgVector** và cách dựng nội dung/metadata của nhánh đó. Không lấy metadata của RAM để khẳng định mọi profile đều giống nhau.
- Nhánh semantic tạo tài liệu theo sản phẩm đủ điều kiện lập chỉ mục, dùng hash/signature để nhận biết thay đổi và lưu persistent. Nó không chạy đoạn `chunker.chunk(p)` của nhánh RAM vừa demo.
- Phần semantic recommendations hiện yêu cầu kho persistent; không lấy kết quả PASS của RAM để khẳng định cả luồng recommendations đã chạy.
- Thanh tìm kiếm/chat hiện có luồng AI riêng. Chưa thể kết luận rằng nhập một câu trên thanh tìm kiếm là đã chạy qua sáu file vector này. Dùng API đồng bộ và API xem kho ở trên để demo đúng phạm vi 3.3.

Giá trị của 3.3 là cung cấp dữ liệu và hạ tầng cho luồng ngữ nghĩa; không phải tự nó đã làm xong cả giao diện và tư vấn.

## 13. Những câu Cô có thể hỏi

**“Vector là gì?”**

> “Dạ là một dãy số biểu diễn nội dung trong không gian của model. Trong cấu hình của em mỗi dãy có 768 số. Mình so sánh các dãy để đo mức tương đồng của nội dung.”

**“Em tự tính 768 số đó à?”**

> “Dạ không. Em chuẩn bị text và cấu hình model, rồi Spring AI gọi Gemini để tính. Em kiểm tra kết quả và lưu lại.”

**“Một vector có phải một sản phẩm không?”**

> “Trong pipeline này một document có một vector. Một sản phẩm có thể tạo nhiều document nếu mô tả dài. Dữ liệu demo hiện tại mỗi sản phẩm chỉ có một đoạn nên mình đang thấy một sản phẩm đi với một vector.”

**“Vì sao vừa cần text vừa cần metadata?”**

> “Text phục vụ biểu diễn ngữ nghĩa. Metadata giữ thông tin có cấu trúc để nhận diện, phân loại và quản lý kết quả. Ví dụ productId giúp em nối document về sản phẩm gốc; chunkIndex giúp biết đang xem đoạn nào.”

**“Giá với tồn kho có được Gemini tính ra không?”**

> “Dạ không, Java lấy từ dữ liệu sản phẩm để đưa vào metadata. Nó có thể cũ sau khi đồng bộ nên khi tư vấn phải đọc lại database.”

**“Dòng new Document đã tạo vector chưa?”**

> “Dạ chưa. Nó mới tạo id, text, metadata. Khi gọi staging.add(documents), SimpleVectorStore mới dùng embedding model để tạo vector và nạp kho.”

**“Lỡ Gemini lỗi giữa chừng?”**

> “Dạ em báo lỗi, không sinh số giả. Vì đang nạp vào store tạm nên kho cũ chưa bị thay. Chỉ khi nạp thành công hết mới đổi current sang store mới.”

**“Sao reindex trả về không có mảng số?”**

> “Vì response của reindex là báo cáo thao tác. Muốn xem dữ liệu đã lưu, em gọi GET documents có quyền Admin. Mảng số nằm trong trường embedding của từng document.”

**“Restart có mất vector không?”**

> “Trong demo local lưu RAM thì có. Nhánh PgVector lưu trong PostgreSQL là lựa chọn persistent; hai chế độ phải phân biệt rõ.”

**“Đã huấn luyện Gemini bằng sản phẩm chưa?”**

> “Dạ đây là tạo embedding để xây chỉ mục tìm kiếm, không phải huấn luyện lại model. Sản phẩm được biểu diễn bằng model có sẵn.”

## 14. Lỗi thường gặp khi thao tác demo

| Hiện tượng | Cách xử lý |
|---|---|
| `vectorDocuments is not defined` | Chạy lại toàn bộ bước 6 trên đúng tab; không refresh giữa các bước |
| Tổng document bằng 0 | Kho RAM chưa nạp hoặc backend vừa restart; chạy bước 5 rồi bước 6 |
| `Promise {<pending>}` sau POST | Đợi request hoàn tất; xem Network và log backend, không bấm chạy lặp liên tục |
| 401/403 | Kiểm tra tài khoản Admin; với POST cần CSRF token và cookie đúng phiên |
| 429 / `VECTOR_QUOTA_EXCEEDED` | Model hết quota; dùng kho đã lưu để trình bày nếu còn, hoặc chờ trước khi thử lại |
| 503 / `VECTOR_NOT_CONFIGURED` | Cấu hình key ở backend rồi khởi động lại theo cấu hình dự án |
| `Failed to fetch` hoặc proxy lỗi | Kiểm tra frontend 5173 và backend 8080 có đang chạy |
| Kho hiển thị `pgvector` thay vì `memory` | Đang chạy nhánh persistent; muốn demo đúng nội dung local thì dùng profile/cấu hình của bước 3 |

Nếu muốn xem chi tiết kỹ thuật/kiểm thử ngoài bài trình bày, đọc [Hướng dẫn triển khai và kiểm thử 3.3](SPRING_AI_3_3_VAN_NHUT.md).

## 15. Đoạn chốt để nói cuối buổi

> “Dạ, qua demo này em đã chỉ được sản phẩm gốc trong catalog, text mà chương trình chuẩn bị, metadata để nối về sản phẩm, và vector 768 số thực sự đã lưu. Controller nhận yêu cầu, ingestion service điều phối, chunker dựng document, embedding model gọi Gemini và kiểm tra kết quả, còn vector store lưu dữ liệu và chỉ thay kho khi nạp thành công. File config chuẩn bị các đối tượng đó cho Spring. Phần 3.3 của em là nền dữ liệu cho tìm kiếm ngữ nghĩa; phần tìm và hiển thị gợi ý sẽ sử dụng kho này theo luồng tích hợp của nhóm.”

## 16. Demo trạng thái hiện tại: vector đã lưu trên Supabase

Ngày 11/11/2026 đã chạy đồng bộ thành công với backend profile **local,semantic**. Kiểm tra bằng hai đường độc lập:

- API `GET /api/admin/vector/documents`: HTTP 200, `storage: pgvector`, 30 document, mỗi embedding có 768 số hữu hạn và không toàn 0.
- Kết nối JDBC trực tiếp vào Supabase: bảng `public.product_semantic_vectors` có 30 dòng, 30 embedding không null, số chiều nhỏ nhất và lớn nhất đều bằng 768.

Đây là dữ liệu được tạo qua luồng đồng bộ thật với model, không phải vector giả của test. Hai kiểm thử tích hợp catalog/PgVector chạy riêng trong schema tạm đều pass; schema kiểm thử đã được dọn sau test.

**Bạn nói:**

> “Hiện em đã chuyển kho sang PgVector trên Supabase. Ở đây bảng lưu content là văn bản, metadata để nhận diện và embedding là vector 768 chiều. Dữ liệu được backend tạo và ghi vào database, nên không mất khi backend dừng như kho RAM.”

**Cách xem ngay:** chạy khối Console ở bước 6 để đọc lại dữ liệu hiện tại, sau đó dùng các lệnh `vectorDocuments[0].text`, `vectorDocuments[0].metadata` và `vectorDocuments[0].embedding` như các bước tiếp theo. Không cần gọi reindex chỉ để xem.

Trong Supabase Table Editor, mở `product_semantic_vectors`, bấm Refresh. Hoặc chạy SQL:

```sql
SELECT count(*) AS document_count,
       min(json_array_length(embedding::text::json)) AS min_dimensions,
       max(json_array_length(embedding::text::json)) AS max_dimensions
FROM public.product_semantic_vectors;

SELECT id, content, metadata, embedding
FROM public.product_semantic_vectors
ORDER BY id
LIMIT 5;
```

Kết quả kiểm tra hiện tại: `document_count = 30`, `min_dimensions = 768`, `max_dimensions = 768`.

**Vì sao khác 44 document RAM?** Nhánh semantic hiện chọn sản phẩm ACTIVE trong các danh mục cpu, mainboard, vga, ram, storage, cooling, case, psu, monitor, mouse, keyboard và có dữ liệu storefront liên kết. Catalog hiện có 30 sản phẩm đáp ứng điều kiện đó. Nhánh RAM trước đây đọc toàn bộ 44 sản phẩm ACTIVE.

**Metadata cũng khác:** nhánh semantic hiện lưu `productId`, `category`, `embeddingSignature`, `contentHash`. Nhánh này dựng nội dung trong `ProductSemanticIndexService`, không đi qua `ProductChunker`, nên không có `chunkIndex/chunkCount`. Khi cần giải thích cơ chế chunking của sáu file gốc, dùng phần local trong tài liệu hoặc mở code; đừng nói các document semantic đang có chunkIndex bằng 0.

Lúc chạy lại backend để tiếp tục dùng Supabase:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,semantic"
```

Trong lượt vận hành này đã sửa truy vấn ở `JdbcRecommendationCatalog` để đọc socket từ cả cột cũ và `specs.socket` dạng JSON, phù hợp schema thực tế trên Supabase. Không thay cấu trúc bảng thông số sản phẩm.
