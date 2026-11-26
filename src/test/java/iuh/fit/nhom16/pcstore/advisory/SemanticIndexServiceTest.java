package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.infrastructure.ProductSemanticIndexService;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import java.sql.ResultSet;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SemanticIndexServiceTest {
    @Test void indexingIsIdempotentUpdatesChangedContentAndRemovesStaleIds() throws Exception {
        var catalog=mock(RecommendationCatalog.class); var vectors=mock(VectorStore.class); var jdbc=mock(JdbcTemplate.class);
        Map<String,Map<String,Object>> indexed=new HashMap<>();
        when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(true);
        doAnswer(call -> {
            RowCallbackHandler callback=call.getArgument(1);
            for(var entry:indexed.entrySet()) {
                var rs=mock(ResultSet.class);
                when(rs.getString(1)).thenReturn(entry.getKey());
                when(rs.getString(2)).thenReturn((String)entry.getValue().get("contentHash"));
                when(rs.getString(3)).thenReturn((String)entry.getValue().get("embeddingSignature"));
                callback.processRow(rs);
            }
            return null;
        }).when(jdbc).query(anyString(),any(RowCallbackHandler.class));
        doAnswer(call -> { for(Document document:call.<List<Document>>getArgument(0)) indexed.put(document.getId(),document.getMetadata()); return null; })
                .when(vectors).add(anyList());
        doAnswer(call -> { call.<List<String>>getArgument(0).forEach(indexed::remove); return null; }).when(vectors).delete(anyList());
        var item=RecommendationServiceTest.item("cpu",100,1,0,"ACTIVE","LGA1700");
        when(catalog.indexableProducts()).thenReturn(List.of(item));
        var service=new ProductSemanticIndexService(catalog,vectors,jdbc,"test-model");
        assertEquals(1,service.synchronize().updated());
        assertEquals(0,service.synchronize().updated());
        var old=indexed.get(item.id().toString()); assertEquals("cpu",old.get("category"));
        assertEquals("test-model:768:semantic-v1",old.get("embeddingSignature"));
        var changed=new RecommendationCatalog.Item(item.id(),item.product(),"ACTIVE",1,0,"AM5");
        when(catalog.indexableProducts()).thenReturn(List.of(changed));
        assertEquals(1,service.synchronize().updated());
        assertNotEquals(old.get("contentHash"),indexed.get(item.id().toString()).get("contentHash"));
        when(catalog.indexableProducts()).thenReturn(List.of());
        assertEquals(1,service.synchronize().removed()); assertTrue(indexed.isEmpty());
    }
    @Test void busyLockDoesNotCallProviderOrReadCatalog() {
        var catalog=mock(RecommendationCatalog.class); var vectors=mock(VectorStore.class); var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(false);
        assertTrue(new ProductSemanticIndexService(catalog,vectors,jdbc,"test").synchronize().busy());
        verifyNoInteractions(catalog,vectors);
    }
}
