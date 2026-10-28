package com.deroyque.activation.web;

import com.deroyque.activation.application.port.in.CreateActivationUseCase;
import com.deroyque.activation.application.port.in.GetActivationUseCase;
import com.deroyque.activation.application.port.in.ListActivationsUseCase;
import com.deroyque.activation.domain.exception.ActivationConflictException;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ActivationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CorrelationIdFilter.class})
class ActivationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateActivationUseCase createActivationUseCase;

    @MockBean
    private GetActivationUseCase getActivationUseCase;

    @MockBean
    private ListActivationsUseCase listActivationsUseCase;

    @Test
    @DisplayName("POST /api/v1/requests should return 201 Created with Location header when creation succeeds")
    void shouldCreateActivationRequestSuccessfully() throws Exception {
        ActivationRequest request = ActivationRequest.create("EXT-100", "CUST-001", "INTERNET-500");
        when(createActivationUseCase.createActivation("EXT-100", "CUST-001", "INTERNET-500"))
                .thenReturn(new CreateActivationUseCase.CreateResult(request, true));

        String jsonPayload = """
                {
                    "externalId": "EXT-100",
                    "customerId": "CUST-001",
                    "productCode": "INTERNET-500"
                }
                """;

        mockMvc.perform(post("/api/v1/requests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/requests/" + request.getId())))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.id").value(request.getId().toString()))
                .andExpect(jsonPath("$.externalId").value("EXT-100"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/requests should return 200 OK when request is an idempotent replay")
    void shouldReturn200OnIdempotentReplay() throws Exception {
        ActivationRequest request = ActivationRequest.create("EXT-100", "CUST-001", "INTERNET-500");
        when(createActivationUseCase.createActivation("EXT-100", "CUST-001", "INTERNET-500"))
                .thenReturn(new CreateActivationUseCase.CreateResult(request, false));

        String jsonPayload = """
                {
                    "externalId": "EXT-100",
                    "customerId": "CUST-001",
                    "productCode": "INTERNET-500"
                }
                """;

        mockMvc.perform(post("/api/v1/requests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/requests should return 400 Bad Request Problem Details on validation error")
    void shouldReturn400OnValidationError() throws Exception {
        String invalidPayload = """
                {
                    "externalId": "",
                    "customerId": "CUST-001",
                    "productCode": ""
                }
                """;

        mockMvc.perform(post("/api/v1/requests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Invalid Request Data"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/requests should return 409 Conflict Problem Details on divergent externalId conflict")
    void shouldReturn409OnConflict() throws Exception {
        when(createActivationUseCase.createActivation(anyString(), anyString(), anyString()))
                .thenThrow(new ActivationConflictException("EXT-100", "Divergent productCode"));

        String jsonPayload = """
                {
                    "externalId": "EXT-100",
                    "customerId": "CUST-001",
                    "productCode": "DIFFERENT-PRODUCT"
                }
                """;

        mockMvc.perform(post("/api/v1/requests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Request Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }
}
