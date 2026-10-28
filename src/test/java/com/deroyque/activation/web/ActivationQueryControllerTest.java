package com.deroyque.activation.web;

import com.deroyque.activation.application.port.in.CreateActivationUseCase;
import com.deroyque.activation.application.port.in.GetActivationUseCase;
import com.deroyque.activation.application.port.in.ListActivationsUseCase;
import com.deroyque.activation.domain.exception.ActivationNotFoundException;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ActivationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CorrelationIdFilter.class})
class ActivationQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateActivationUseCase createActivationUseCase;

    @MockBean
    private GetActivationUseCase getActivationUseCase;

    @MockBean
    private ListActivationsUseCase listActivationsUseCase;

    @Test
    @DisplayName("GET /api/v1/requests/{id} should return 200 OK when entity exists")
    void shouldReturn200WhenExists() throws Exception {
        UUID id = UUID.randomUUID();
        ActivationRequest request = ActivationRequest.create("EXT-100", "CUST-001", "INTERNET-500");
        when(getActivationUseCase.getById(id)).thenReturn(request);

        mockMvc.perform(get("/api/v1/requests/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("EXT-100"))
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.productCode").value("INTERNET-500"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/requests/{id} should return 404 Not Found Problem Details when entity is missing")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getActivationUseCase.getById(id)).thenThrow(new ActivationNotFoundException(id));

        mockMvc.perform(get("/api/v1/requests/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:read"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Activation Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString(id.toString())));
    }

    @Test
    @DisplayName("GET /api/v1/requests should return paginated results with metadata")
    void shouldReturnPaginatedList() throws Exception {
        ActivationRequest r1 = ActivationRequest.create("EXT-1", "CUST-001", "INTERNET-500");
        ActivationRequest r2 = ActivationRequest.create("EXT-2", "CUST-001", "TV-HD");

        when(listActivationsUseCase.list(eq("CUST-001"), eq(ActivationStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/v1/requests?customerId=CUST-001&status=PENDING&page=0&size=20")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_activations:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}
