package com.deroyque.activation.client;

import com.deroyque.activation.application.port.out.ExternalProviderPort;
import com.deroyque.activation.infrastructure.client.ExternalProviderClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class ExternalProviderClientIntegrationTest {

    private static WireMockServer wireMockServer;
    private ExternalProviderClient client;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(300));
        factory.setReadTimeout(Duration.ofMillis(500));

        RestClient restClient = RestClient.builder()
                .baseUrl(wireMockServer.baseUrl())
                .requestFactory(factory)
                .build();

        client = new ExternalProviderClient(restClient);
    }

    @Test
    @DisplayName("Should return Success when provider returns 200 OK")
    void shouldReturnSuccessOn200() {
        stubFor(post(urlEqualTo("/provider/v1/activate"))
                .withHeader("X-Idempotency-Key", equalTo("KEY-123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"activationCode\": \"ACT-999\", \"status\": \"ACTIVATED\"}")));

        ExternalProviderPort.ActivationResult result = client.activate("KEY-123", "CUST-1", "PROD-1");

        assertThat(result).isInstanceOf(ExternalProviderPort.ActivationResult.Success.class);
        ExternalProviderPort.ActivationResult.Success success = (ExternalProviderPort.ActivationResult.Success) result;
        assertThat(success.activationCode()).isEqualTo("ACT-999");
    }

    @Test
    @DisplayName("Should return ClientError when provider returns 400 Bad Request")
    void shouldReturnClientErrorOn400() {
        stubFor(post(urlEqualTo("/provider/v1/activate"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"INVALID_PRODUCT\"}")));

        ExternalProviderPort.ActivationResult result = client.activate("KEY-123", "CUST-1", "INVALID");

        assertThat(result).isInstanceOf(ExternalProviderPort.ActivationResult.ClientError.class);
        ExternalProviderPort.ActivationResult.ClientError error = (ExternalProviderPort.ActivationResult.ClientError) result;
        assertThat(error.statusCode()).isEqualTo(400);
        assertThat(error.message()).contains("INVALID_PRODUCT");
    }

    @Test
    @DisplayName("Should return ServerError when provider returns 500")
    void shouldReturnServerErrorOn500() {
        stubFor(post(urlEqualTo("/provider/v1/activate"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        ExternalProviderPort.ActivationResult result = client.activate("KEY-123", "CUST-1", "PROD-1");

        assertThat(result).isInstanceOf(ExternalProviderPort.ActivationResult.ServerError.class);
        ExternalProviderPort.ActivationResult.ServerError error = (ExternalProviderPort.ActivationResult.ServerError) result;
        assertThat(error.statusCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should return Timeout when provider response takes longer than read timeout")
    void shouldReturnTimeoutOnDelay() {
        stubFor(post(urlEqualTo("/provider/v1/activate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"activationCode\": \"ACT-999\"}")
                        .withFixedDelay(2000)));

        ExternalProviderPort.ActivationResult result = client.activate("KEY-123", "CUST-1", "PROD-1");

        assertThat(result).isInstanceOf(ExternalProviderPort.ActivationResult.Timeout.class);
    }
}
