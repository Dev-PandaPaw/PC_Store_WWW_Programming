package iuh.fit.nhom16.pcstore.catalog.application;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import iuh.fit.nhom16.pcstore.ai.vector.ProductVectorIngestionService;
import iuh.fit.nhom16.pcstore.ai.vector.ProductVectorStore;
import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import java.util.*;

/** Vectors supply relevance only. Product facts always come from the current catalog. */
@Service
public class ProductRetrievalService {
    private final ProductVectorStore vectors;
    private final ProductVectorIngestionService ingestion;
    private boolean persistentChecked;

    public ProductRetrievalService(ProductVectorStore vectors, ProductVectorIngestionService ingestion) {
        this.vectors=vectors; this.ingestion=ingestion;
    }

    public List<StoreProduct> rank(String query, List<StoreProduct> current) {
        ensureIndex();
        var documents=vectors.similaritySearch(SearchRequest.builder().query(query).topK(200)
                .similarityThreshold(0).build());
        if (documents == null || documents.isEmpty())
            throw new AiFailure(503,"VECTOR_INDEX_EMPTY","Chưa có dữ liệu vector sản phẩm. Vui lòng đồng bộ chỉ mục rồi thử lại.");
        Map<String,Integer> rank=new LinkedHashMap<>();
        for (var document:documents) {
            Object id=document.getMetadata().get("productId");
            if(id!=null) rank.putIfAbsent(id.toString(),rank.size());
        }
        // Include unmatched catalog rows: hard filters and completeness must not depend on top-K recall.
        return current.stream().sorted(Comparator.comparingInt(p -> rank.getOrDefault(p.id(),Integer.MAX_VALUE))).toList();
    }

    private synchronized void ensureIndex() {
        if(!vectors.isPersistent() && vectors.memoryDocumentCount()==0) ingestion.reindexAll();
        else if(vectors.isPersistent() && !persistentChecked) {
            // Persistent synchronization is hash-based and only embeds changed products.
            ingestion.reindexAll(); persistentChecked=true;
        }
    }
}
