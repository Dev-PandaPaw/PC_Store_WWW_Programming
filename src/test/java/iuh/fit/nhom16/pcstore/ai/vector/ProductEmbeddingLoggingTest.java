package iuh.fit.nhom16.pcstore.ai.vector;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import iuh.fit.nhom16.pcstore.ai.AiFailure;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductEmbeddingLoggingTest {
    final Logger logger=(Logger)LoggerFactory.getLogger(ProductEmbeddingModel.class);
    final ListAppender<ILoggingEvent> logs=new ListAppender<>();
    Level previous;
    @BeforeEach void capture() { previous=logger.getLevel();logger.setLevel(Level.DEBUG);logs.start();logger.addAppender(logs); }
    @AfterEach void restore() { logger.detachAppender(logs);logs.stop();logger.setLevel(previous); }
    @Test void pairsEachInputWithItsActualValidatedVectorWithoutChangingTheResponse() {
        var delegate=mock(EmbeddingModel.class);
        var request=new EmbeddingRequest(List.of("máy tính chơi game\nCPU", "mainboard"),EmbeddingOptions.builder().build());
        var response=new EmbeddingResponse(List.of(new Embedding(new float[]{1f,.25f},0),new Embedding(new float[]{.5f,.75f},1)));
        when(delegate.call(request)).thenReturn(response);
        assertSame(response,new ProductEmbeddingModel(delegate,true,2).call(request));
        assertEquals(2,logs.list.size());
        assertTrue(logs.list.get(0).getFormattedMessage().contains("máy tính chơi game\\nCPU"));
        assertTrue(logs.list.get(0).getFormattedMessage().contains("dimensions=2, vectorPreview=[1.0, 0.25]"));
        assertTrue(logs.list.get(1).getFormattedMessage().contains("mainboard"));
        assertTrue(logs.list.get(1).getFormattedMessage().contains("vectorPreview=[0.5, 0.75]"));
    }
    @Test void malformedProviderOutputDoesNotProduceSuccessfulVectorLogs() {
        var delegate=mock(EmbeddingModel.class);
        var request=new EmbeddingRequest(List.of("CPU"),EmbeddingOptions.builder().build());
        when(delegate.call(request)).thenReturn(new EmbeddingResponse(List.of(new Embedding(new float[]{0,0},0))));
        assertThrows(AiFailure.class,()->new ProductEmbeddingModel(delegate,true,2).call(request));
        assertTrue(logs.list.isEmpty());
    }
}
