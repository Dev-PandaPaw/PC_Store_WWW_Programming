package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductVectorInspectionTest {
    @Test void listsCompleteActualVectorsWithoutEmbeddingAgainOrReadingCatalog() {
        var embedding = mock(EmbeddingModel.class);
        float[] vector = new float[768];
        vector[0] = 0.023f; vector[1] = -0.148f; vector[767] = 0.091f;
        when(embedding.embed(any(Document.class))).thenReturn(vector);
        var store = new ProductVectorStore(embedding);
        var document = new Document("doc-1", "Sản phẩm: CPU Intel", Map.of("productId", "product-1"));
        store.replaceAll(List.of(document));
        clearInvocations(embedding);
        ObjectProvider<JdbcTemplate> database = mock(ObjectProvider.class);
        var service = new ProductVectorInspectionService(store, database, JsonMapper.builder().build());

        var page = service.documents(0, 50);

        assertEquals("memory", page.storage()); assertEquals(1, page.total());
        var found = page.documents().getFirst();
        assertEquals("doc-1", found.id()); assertEquals(document.getText(), found.text());
        assertEquals("product-1", found.metadata().get("productId"));
        assertEquals(768, found.dimensions()); assertEquals(768, found.embedding().size());
        assertEquals(vector[0], found.embedding().getFirst());
        assertEquals(vector[1], found.embedding().get(1));
        assertEquals(vector[767], found.embedding().getLast());
        verifyNoInteractions(embedding, database);
    }

    @Test void paginationIsStableAndReturnedDataCannotModifyStoredVector() {
        var embedding = mock(EmbeddingModel.class);
        when(embedding.embed(any(Document.class))).thenReturn(new float[]{0.25f, -0.5f});
        var store = new ProductVectorStore(embedding);
        store.replaceAll(List.of(new Document("b", "B", Map.of()), new Document("a", "A", Map.of())));
        var service = new ProductVectorInspectionService(store, mock(ObjectProvider.class), JsonMapper.builder().build());
        var page = service.documents(1, 1);
        assertEquals(2, page.total()); assertEquals("b", page.documents().getFirst().id());
        assertThrows(UnsupportedOperationException.class, () -> page.documents().getFirst().embedding().set(0, 9f));
        assertEquals(0.25f, store.memoryDocuments().getFirst().embedding().getFirst());
        assertTrue(service.documents(99, 1).documents().isEmpty());
    }

    @Test void failedReindexKeepsInspectableOldVectorsAndDeleteRemovesThem() {
        var embedding = mock(EmbeddingModel.class);
        when(embedding.embed(any(Document.class))).thenReturn(new float[]{0.25f, -0.5f});
        var store = new ProductVectorStore(embedding);
        store.replaceAll(List.of(new Document("old", "Old", Map.of())));
        when(embedding.embed(any(Document.class))).thenReturn(new float[]{1f, 0f}).thenThrow(new IllegalStateException("provider"));
        assertThrows(IllegalStateException.class, () -> store.replaceAll(List.of(
                new Document("new-1", "New 1", Map.of()), new Document("new-2", "New 2", Map.of()))));
        assertEquals(List.of("old"), store.memoryDocuments().stream().map(ProductVectorStore.VectorDocument::id).toList());
        store.delete(List.of("old")); assertTrue(store.memoryDocuments().isEmpty());
    }

    @Test void emptyStoreAndInvalidPageNeverCallProvider() {
        var embedding = mock(EmbeddingModel.class);
        var service = new ProductVectorInspectionService(new ProductVectorStore(embedding),
                mock(ObjectProvider.class), JsonMapper.builder().build());
        assertEquals(0, service.documents(0, 100).total());
        for (int[] args : new int[][]{{-1, 50}, {0, 0}, {0, 101}})
            assertEquals(400, assertThrows(AiFailure.class, () -> service.documents(args[0], args[1])).status);
        verifyNoInteractions(embedding);
    }

    @Test void persistentInspectionReadsNumericArrayFromDatabaseWithoutVectorSearch() throws Exception {
        var persistent = mock(VectorStore.class);
        var database = mock(JdbcTemplate.class);
        ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(database);
        when(database.queryForObject(anyString(), eq(Long.class))).thenReturn(3L);
        var row = mock(ResultSet.class);
        when(row.getString("id")).thenReturn("stored-id");
        when(row.getString("content")).thenReturn("Stored CPU");
        when(row.getString("metadata")).thenReturn("{\"productId\":\"product-1\"}");
        when(row.getString("embedding")).thenReturn("[0.125,-0.25,0.5]");
        when(database.query(anyString(), any(RowMapper.class), eq(2), eq(1)))
                .thenAnswer(invocation -> List.of(((RowMapper<?>) invocation.getArgument(1)).mapRow(row, 0)));
        var service = new ProductVectorInspectionService(new ProductVectorStore(persistent), provider, JsonMapper.builder().build());
        var page = service.documents(1, 2);
        assertEquals("pgvector", page.storage()); assertEquals(3, page.total());
        assertEquals(List.of(0.125f, -0.25f, 0.5f), page.documents().getFirst().embedding());
        assertEquals(3, page.documents().getFirst().dimensions());
        verifyNoInteractions(persistent);
    }
}
