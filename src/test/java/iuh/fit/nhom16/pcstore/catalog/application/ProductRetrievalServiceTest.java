package iuh.fit.nhom16.pcstore.catalog.application;

import iuh.fit.nhom16.pcstore.ai.vector.*;
import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductRetrievalServiceTest {
    @Test void vectorMetadataOnlyRanksCurrentProductsAndNeverSuppliesPricesOrStock() {
        var vectors=mock(ProductVectorStore.class); var ingestion=mock(ProductVectorIngestionService.class);
        var service=new ProductRetrievalService(vectors,ingestion);
        when(vectors.memoryDocumentCount()).thenReturn(1);
        when(vectors.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of(new Document("old snapshot",Map.of("productId","b","price",1,"stockOnHand",999)),new Document("forged",Map.of("productId","unknown"))));
        StoreProduct a=product("a",200),b=product("b",300);
        var found=service.rank("ram",List.of(a,b));
        assertEquals(List.of(b,a),found); assertEquals(300,found.getFirst().price()); assertEquals(2,found.getFirst().stockOnHand());
        verifyNoInteractions(ingestion);
    }
    private StoreProduct product(String id,long price) { return new StoreProduct(id,null,id,id,"ram","","",price,price,List.of(),2,"ram"); }
}
