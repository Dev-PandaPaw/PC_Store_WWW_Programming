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

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AdminVectorControllerTest.Config.class)
class AdminVectorControllerTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, AdminVectorController.class})
    static class Config {
        @Bean ProductVectorIngestionService ingestion() { return mock(ProductVectorIngestionService.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired ProductVectorIngestionService ingestion;
    MockMvc mvc;
    @BeforeEach void setup() {
        reset(ingestion);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void anonymousIsUnauthorizedAndCustomerIsForbidden() throws Exception {
        mvc.perform(post("/api/admin/vector/reindex").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/vector/reindex").with(user("customer").roles("CUSTOMER")).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ingestion);
    }

    @Test void adminWithoutCsrfCannotStartPaidEmbeddingWork() throws Exception {
        mvc.perform(post("/api/admin/vector/reindex").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ingestion);
    }

    @Test void adminWithCsrfGetsCountsForFutureSyncUi() throws Exception {
        when(ingestion.reindexAll()).thenReturn(new ProductVectorIngestionService.ReindexResult("SUCCESS", 2, 3, Instant.now()));
        mvc.perform(post("/api/admin/vector/reindex").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.productCount").value(2)).andExpect(jsonPath("$.documentCount").value(3));
    }

    @Test void quotaFailureReturnsRetryAfterAndSafeErrorCode() throws Exception {
        when(ingestion.reindexAll()).thenThrow(new AiFailure(429, "VECTOR_QUOTA_EXCEEDED", "Hết quota", 60));
        mvc.perform(post("/api/admin/vector/reindex").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.error.code").value("VECTOR_QUOTA_EXCEEDED"));
    }
}
