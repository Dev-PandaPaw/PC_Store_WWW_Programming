package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.application.*;
import iuh.fit.nhom16.pcstore.advisory.web.*;
import iuh.fit.nhom16.pcstore.configuration.security.SecurityConfig;
import iuh.fit.nhom16.pcstore.identity.api.RecommendationAccountAccess;
import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import iuh.fit.nhom16.pcstore.identity.model.AuthenticatedAccount;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecommendationControllerTest {
    static final UUID USER=UUID.randomUUID();
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    ProductSemanticRecommendationService service;
    User account;
    @Configuration @EnableWebMvc
    @Import({SecurityConfig.class,RecommendationController.class,RecommendationExceptionHandler.class,RecommendationAccountAccess.class})
    static class Config {
        @Bean ProductSemanticRecommendationService recommendations() { return mock(ProductSemanticRecommendationService.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean UserDetailsService userDetails() { return new InMemoryUserDetailsManager(); }
    }
    @BeforeEach void setup() {
        context=new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.register(Config.class); context.refresh();
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        service=context.getBean(ProductSemanticRecommendationService.class);
        account=mock(User.class); when(account.isActive()).thenReturn(true);
        when(context.getBean(UserRepository.class).findById(USER)).thenReturn(Optional.of(account));
        when(service.personalized(eq(USER),anyList())).thenReturn(new ProductSemanticRecommendationService.Result(List.of(),"insufficient_history"));
        when(service.similar(any(),anyList())).thenReturn(new ProductSemanticRecommendationService.Result(List.of(),"no_match"));
    }
    @AfterEach void close() { context.close(); SecurityContextHolder.clearContext(); }
    static UsernamePasswordAuthenticationToken token(String role) {
        return UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedAccount(USER,"customer@example.test"),null,
                List.of(new SimpleGrantedAuthority("ROLE_"+role)));
    }
    @Test void personalizedRequiresAnAuthenticatedShopAccount() throws Exception {
        mvc.perform(get("/api/v1/recommendations/personalized")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/recommendations/personalized").with(user("unmapped")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void personalizedUsesSessionIdentityAndDoesNotCachePrivateResults() throws Exception {
        mvc.perform(get("/api/v1/recommendations/personalized").with(authentication(token("CUSTOMER")))
                        .param("userId",UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.status").value("insufficient_history"));
        verify(service).personalized(USER,List.of());
    }
    @Test void blockedAccountsCannotReadPersonalizedHistory() throws Exception {
        when(account.isActive()).thenReturn(false);
        mvc.perform(get("/api/v1/recommendations/personalized").with(authentication(token("CUSTOMER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void similarIsPublicAndValidatesUuidAndContext() throws Exception {
        UUID source=UUID.randomUUID(),selected=UUID.randomUUID();
        mvc.perform(get("/api/v1/recommendations/similar/"+source).param("selectedProductIds",selected.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("no_match"));
        verify(service).similar(source,List.of(selected));
        mvc.perform(get("/api/v1/recommendations/similar/not-a-uuid")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ID"));
        mvc.perform(get("/api/v1/recommendations/similar/"+source).param("selectedProductIds","invalid"))
                .andExpect(status().isBadRequest());
    }
    @Test void adminReindexRequiresRoleAndCsrfAndReportsDisabledStore() throws Exception {
        mvc.perform(post("/api/v1/admin/recommendations/reindex").with(authentication(token("ADMIN"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/recommendations/reindex").with(authentication(token("CUSTOMER"))).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/recommendations/reindex").with(authentication(token("ADMIN"))).with(csrf()))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error.code").value("VECTOR_UNAVAILABLE"));
    }
    @Test void serviceFailuresUseStableEnvelope() throws Exception {
        UUID source=UUID.randomUUID();
        when(service.similar(eq(source),anyList())).thenThrow(new RecommendationFailure(503,"VECTOR_UNAVAILABLE","Tạm thời gián đoạn"));
        mvc.perform(get("/api/v1/recommendations/similar/"+source)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("VECTOR_UNAVAILABLE"));
    }
}
