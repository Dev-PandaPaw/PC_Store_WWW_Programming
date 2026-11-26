/*
 * Mở http://localhost:5173 → F12 → Console và dán toàn bộ file này.
 * File chỉ tạo các hàm hỗ trợ demo; chưa gọi Gemini hoặc tự chạy reindex.
 * Bắt đầu: await demoVector33.load()
 */
(() => {
  let authorization;
  let snapshot;

  function authenticate() {
    if (authorization) return;
    const username = prompt('Tên Admin local', 'admin');
    const password = prompt('Mật khẩu Admin local');
    if (!username || !password) throw new Error('Chưa nhập tài khoản Admin.');
    authorization = 'Basic ' + btoa(username + ':' + password);
  }

  async function request(path, options = {}) {
    authenticate();
    const response = await fetch(path, {
      ...options,
      credentials: 'include',
      headers: { Authorization: authorization, ...options.headers },
    });
    const result = await response.json();
    if (!response.ok) {
      if (response.status === 401) authorization = undefined;
      throw new Error(`HTTP ${response.status}: ${result.error?.message || result.message || result.error?.code || 'Request thất bại'}`);
    }
    return result;
  }

  function documentAt(index = 0) {
    if (!snapshot) throw new Error('Chạy await demoVector33.load() trước.');
    if (!Number.isInteger(index) || index < 0 || index >= snapshot.documents.length)
      throw new Error('Số thứ tự document không hợp lệ.');
    return snapshot.documents[index];
  }

  window.demoVector33 = {
    async load() {
      let offset = 0;
      let firstPage;
      const documents = [];
      do {
        const page = await request(`/api/admin/vector/documents?offset=${offset}&limit=100`);
        firstPage ||= page;
        if (page.total !== firstPage.total || page.storage !== firstPage.storage)
          throw new Error('Index vừa thay đổi. Chạy load() lại để lấy danh sách mới.');
        documents.push(...page.documents);
        offset += page.documents.length;
        if (!page.documents.length) break;
      } while (offset < firstPage.total);
      if (documents.length !== firstPage.total)
        throw new Error('Chưa đọc đủ document. Chạy load() lại.');
      snapshot = { storage: firstPage.storage, total: firstPage.total, documents };
      window.vectorDocuments = documents;
      console.log(`Đã đọc ${documents.length} document từ ${snapshot.storage}. GET chỉ đọc, không gọi Gemini.`);
      return { storage: snapshot.storage, documentCount: documents.length };
    },

    summary() {
      if (!snapshot) throw new Error('Chạy await demoVector33.load() trước.');
      console.table(snapshot.documents.map((document, index) => ({
        index,
        productId: document.metadata.productId,
        sanPham: document.text.split('\n')[0].replace(/^Sản phẩm:\s*/, ''),
        danhMuc: document.metadata.category,
        chunkIndex: document.metadata.chunkIndex,
        chunkCount: document.metadata.chunkCount,
        dimensions: document.dimensions,
      })));
    },

    async original(index = 0) {
      const document = documentAt(index);
      const product = await request('/api/catalog/products/' + encodeURIComponent(document.metadata.productId));
      console.log('Sản phẩm nguồn đọc lại từ DB:', product);
      console.log('ID khớp metadata.productId:', product.id === document.metadata.productId);
      return product;
    },

    text(index = 0) {
      console.log('Text thực tế của document:\n' + documentAt(index).text);
    },

    metadata(index = 0) {
      console.log('Metadata thực tế:', documentAt(index).metadata);
    },

    embedding(index = 0) {
      const document = documentAt(index);
      console.log('Document ID:', document.id);
      console.log('Product ID:', document.metadata.productId);
      console.log('Số chiều:', document.dimensions);
      console.log('Mảng embedding đầy đủ:', document.embedding);
    },

    verify(expectedDimensions = 768) {
      if (!snapshot) throw new Error('Chạy await demoVector33.load() trước.');
      const documents = snapshot.documents;
      const result = {
        storage: snapshot.storage,
        documentCount: documents.length,
        productCount: new Set(documents.map(document => document.metadata.productId)).size,
        expectedDimensions,
        allVectorsValid: documents.length > 0 && documents.every(document =>
          document.dimensions === expectedDimensions &&
          document.embedding.length === expectedDimensions &&
          document.embedding.every(Number.isFinite) &&
          document.embedding.some(value => value !== 0)),
      };
      console.table([result]);
      return result;
    },

    async reindex() {
      // Chỉ chạy chủ động: POST này có thể gọi Gemini cho toàn bộ sản phẩm ACTIVE.
      const csrf = await request('/api/csrf');
      console.log('Đang nạp lại vector. Chờ HTTP hoàn tất; không chạy lặp nhiều lần.');
      const result = await request('/api/admin/vector/reindex', {
        method: 'POST',
        headers: { [csrf.headerName]: csrf.token },
      });
      snapshot = undefined;
      window.vectorDocuments = [];
      console.log('Kết quả đồng bộ:', result);
      console.log('Chạy await demoVector33.load() để đọc index mới.');
      return result;
    },
  };

  console.log('Đã tạo demoVector33. Bắt đầu bằng: await demoVector33.load()');
  console.log('Các bước: summary() → original(0) → text(0) → metadata(0) → embedding(0) → verify()');
})();
