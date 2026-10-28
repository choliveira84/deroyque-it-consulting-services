package com.deroyque.activation.web;

import com.deroyque.activation.application.port.in.CreateActivationUseCase;
import com.deroyque.activation.application.port.in.GetActivationUseCase;
import com.deroyque.activation.application.port.in.ListActivationsUseCase;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.infrastructure.security.SecurityConfig;
import com.deroyque.activation.infrastructure.web.ActivationController;
import com.deroyque.activation.infrastructure.web.advice.GlobalExceptionHandler;
import com.deroyque.activation.infrastructure.web.filter.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ActivationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CorrelationIdFilter.class})
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateActivationUseCase createActivationUseCase;

    @MockBean
    private GetActivationUseCase getActivationUseCase;

    @MockBean
    private ListActivationsUseCase listActivationsUseCase;

    @Test
    @DisplayName("Request without token should return 401 Unauthorized")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/requests/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/requests with only read scope should return 403 Forbidden")
    void shouldReturn403WhenScopeInsufficientForPost() throws Exception {
        String jsonPayload = """
                {
                    "externalId": "EXT-100",
                    "customerId": "CUST-001",
                    "productCode": "INTERNET-500"
                }
                """;

        mockMvc.perform(post("/api/v1/requests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/requests/{id} with read scope should succeed (200 OK)")
    void shouldAllowGetWithReadScope() throws Exception {
        UUID id = UUID.randomUUID();
        ActivationRequest request = ActivationRequest.create("EXT-100", "CUST-001", "INTERNET-500");
        when(getActivationUseCase.getById(id)).thenReturn(request);

        mockMvc.perform(get("/api/v1/requests/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:read"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Documentation endpoints should permit unauthenticated access (not 401)")
    void shouldPermitUnauthenticatedAccessToDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401));
    }
}
