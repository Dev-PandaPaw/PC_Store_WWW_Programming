package iuh.fit.nhom16.pcstore.ai.vector;

import com.google.genai.errors.ApiException;
import iuh.fit.nhom16.pcstore.ai.AiFailure;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/** Kiểm tra kết quả thật của Gemini; tuyệt đối không sinh vector giả khi provider lỗi. */
final class ProductEmbeddingModel extends AbstractEmbeddingModel {
    private final EmbeddingModel delegate;
    private final boolean configured;
    private final int dimensions;

    ProductEmbeddingModel(EmbeddingModel delegate, boolean configured, int dimensions) {
        this.delegate = delegate;
        this.configured = configured;
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(Document document) {
        // Chỉ embed nội dung ngữ nghĩa, không trộn giá/tồn kho trong metadata vào text.
        return embed(document.getText());
    }

    @Override
    public int dimensions() { return dimensions; }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        if (!configured)
            throw new AiFailure(503, "VECTOR_NOT_CONFIGURED", "Chưa cấu hình GEMINI_API_KEY cho embedding.");
        try {
            EmbeddingResponse response = delegate.call(request);
            if (response == null || response.getResults().size() != request.getInstructions().size())
                throw new IllegalStateException("Embedding count mismatch");
            for (var result : response.getResults()) {
                float[] vector = result.getOutput();
                if (vector == null || vector.length != dimensions)
                    throw new IllegalStateException("Embedding dimensions mismatch");
                double norm = 0;
                for (float value : vector) {
                    if (!Float.isFinite(value)) throw new IllegalStateException("Non-finite embedding");
                    norm += (double) value * value;
                }
                if (norm == 0) throw new IllegalStateException("Zero embedding");
            }
            return response;
        } catch (Exception error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof ApiException api) {
                    if (api.code() == 429)
                        throw new AiFailure(429, "VECTOR_QUOTA_EXCEEDED", "Gemini embedding đã hết quota. Hãy thử lại sau.", 60);
                    if (api.code() == 401 || api.code() == 403)
                        throw new AiFailure(503, "VECTOR_ACCESS_DENIED", "API key chưa có quyền dùng embedding model.");
                }
            }
            // Không trả message provider vì có thể chứa URL, key hoặc dữ liệu sản phẩm.
            throw new AiFailure(502, "VECTOR_EMBEDDING_FAILED", "Không tạo được embedding hợp lệ. Vector Store cũ được giữ nguyên.");
        }
    }
}
