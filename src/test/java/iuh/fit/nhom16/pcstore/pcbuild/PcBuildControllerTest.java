package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.configuration.web.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;

class PcBuildControllerTest {
    final PcBuildService service = mock(PcBuildService.class);
    final CartOwnerResolver owners = mock(CartOwnerResolver.class);
    final UUID owner = UUID.randomUUID();
    MockMvc mvc;
    final String body = """
        {"question":"Synthetic test","budgetVnd":"45000000","requireDedicatedGpu":true,
         "includeRag":false,"replaceProductIds":[]}
        """;
    @BeforeEach void setup() {
        when(owners.resolve(any(),any())).thenReturn(owner);
        var filter = new CsrfFilter(new HttpSessionCsrfTokenRepository());
        filter.setRequestHandler(new CsrfTokenRequestAttributeHandler());
        mvc = MockMvcBuilders.standaloneSetup(new PcBuildController(service,owners))
                .setControllerAdvice(new RestExceptionHandler())
                .addFilters(filter).build();
    }
    private RequestPostProcessor validCsrf() {
        return request -> {
            var repository = new HttpSessionCsrfTokenRepository();
            var token = repository.generateToken(request);
            repository.saveToken(token,request,new MockHttpServletResponse());
            request.addHeader(token.getHeaderName(),token.getToken());
            return request;
        };
    }
    @Test void writeRoutesRequireCsrf() throws Exception {
        mvc.perform(post("/api/v1/pc-build/advice").contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service,owners);
    }
    @Test void identityComesFromResolverAndNotRequestBody() throws Exception {
        mvc.perform(post("/api/v1/pc-build/advice").with(validCsrf()).contentType("application/json").content(body))
                .andExpect(status().isOk());
        verify(owners).resolve(any(),any());
        verify(service).advice(any(),eq(owner),argThat(input -> input.budgetVnd().toPlainString().equals("45000000")));
    }
    @Test void ownershipFailureNeverReachesAdviceService() throws Exception {
        when(owners.resolve(any(),any())).thenThrow(new CartException(HttpStatus.UNAUTHORIZED,"CART_USER_UNAVAILABLE","Unavailable"));
        mvc.perform(post("/api/v1/pc-build/advice").with(validCsrf()).contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void confirmationMustExplicitlyBeTrue() throws Exception {
        mvc.perform(post("/api/v1/pc-build/confirm").with(validCsrf()).contentType("application/json")
                .content("{\"proposalId\":\"" + UUID.randomUUID() + "\",\"confirmed\":false,\"acknowledgeUnverified\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void negativeBudgetAndBlankQuestionAreRejected() throws Exception {
        mvc.perform(post("/api/v1/pc-build/advice").with(validCsrf()).contentType("application/json")
                .content(body.replace("45000000","-1"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/pc-build/advice").with(validCsrf()).contentType("application/json")
                .content(body.replace("Synthetic test"," "))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
