package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import iuh.fit.nhom16.pcstore.configuration.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AdminVectorInspectionControllerTest.Config.class)
class AdminVectorInspectionControllerTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, AdminVectorInspectionController.class})
    static class Config {
        @Bean ProductVectorInspectionService inspection() { return mock(ProductVectorInspectionService.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired ProductVectorInspectionService inspection;
    MockMvc mvc;
    @BeforeEach void setup() {
        reset(inspection);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void anonymousAndCustomersCannotReadStoredEmbeddings() throws Exception {
        mvc.perform(get("/api/admin/vector/documents")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/vector/documents").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inspection);
    }

    @Test void adminGetsFullNumericEmbeddingWithoutCsrfAndResponseIsNotCached() throws Exception {
        var document = new ProductVectorStore.VectorDocument("doc-1", "CPU Intel", Map.of("productId", "p1"),
                List.of(0.125f, -0.25f, 0.5f), 3);
        when(inspection.documents(0, 50)).thenReturn(new ProductVectorInspectionService.DocumentPage("memory", 1, 0, 50, List.of(document)));
        mvc.perform(get("/api/admin/vector/documents").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.storage").value("memory"))
                .andExpect(jsonPath("$.documents[0].embedding.length()").value(3))
                .andExpect(jsonPath("$.documents[0].embedding[1]").value(-0.25))
                .andExpect(jsonPath("$.documents[0].dimensions").value(3));
        verify(inspection).documents(0, 50);
    }

    @Test void invalidPaginationReturnsSafeErrorEnvelope() throws Exception {
        when(inspection.documents(0, 101)).thenThrow(new AiFailure(400, "INVALID_VECTOR_PAGE", "limit từ 1 đến 100"));
        mvc.perform(get("/api/admin/vector/documents").param("limit", "101").with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_VECTOR_PAGE"));
    }
}
